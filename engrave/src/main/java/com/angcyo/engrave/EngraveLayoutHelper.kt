package com.angcyo.engrave

import android.graphics.Color
import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.*
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryEngraveFileParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.core.CanvasEntryPoint
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.core.vmApp
import com.angcyo.coroutine.launchLifecycle
import com.angcyo.coroutine.withBlock
import com.angcyo.dialog.messageDialog
import com.angcyo.drawable.DangerWarningDrawable
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.renderEmptyItem
import com.angcyo.dsladapter.updateItem
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.engrave.data.EngraveReadyDataInfo
import com.angcyo.engrave.dslitem.*
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.http.rx.doMain
import com.angcyo.item.form.checkItemThrowable
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.angcyo.widget.layout.touch.SwipeBackLayout.Companion.clamp
import com.angcyo.widget.loading.DangerWarningView
import com.angcyo.widget.recycler.noItemChangeAnim
import com.angcyo.widget.recycler.renderDslAdapter
import kotlin.math.max

/**
 * 雕刻布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
class EngraveLayoutHelper(val lifecycleOwner: LifecycleOwner) : BaseEngraveLayoutHelper() {

    /**雕刻对象*/
    var renderer: BaseItemRenderer<*>? = null
        set(value) {
            field = value
            if (value != null) {
                engraveReadyDataInfo = null
            }
        }

    /**进度item*/
    val engraveProgressItem = EngraveProgressItem()

    var dslAdapter: DslAdapter? = null

    //雕刻模型
    val engraveModel = vmApp<EngraveModel>()

    //雕刻提示
    var dangerWarningView: DangerWarningView? = null

    /**雕刻需要准备的数据*/
    var engraveReadyDataInfo: EngraveReadyDataInfo? = null

    init {
        iViewLayoutId = R.layout.canvas_engrave_layout
    }

    /**显示产品限制框*/
    fun _showProductLimit(canvasView: CanvasView, productInfo: LaserPeckerProductInfo?) {
        if (productInfo == null) {
            canvasView.canvasDelegate.limitRenderer.clear()
        } else {
            if (productInfo.isOriginCenter) {
                canvasView.canvasDelegate.moveOriginToCenter()
            } else {
                canvasView.canvasDelegate.moveOriginToLT()
            }
            canvasView.canvasDelegate.showAndLimitBounds(productInfo.limitPath)
        }
    }

    /**显示Z轴限制框*/
    fun _showZLimit(canvasView: CanvasView) {
        val productInfo = laserPeckerModel.productInfoData.value
        val zLimitPath = productInfo?.zLimitPath
        if (laserPeckerModel.isZOpen() && productInfo != null && zLimitPath != null) {
            //Z轴连接
            //追加显示Z轴显示框
            canvasView.canvasDelegate.limitRenderer.apply {
                updateLimit {
                    addPath(zLimitPath)
                }
                limitBounds = productInfo.bounds
            }
            canvasView.canvasDelegate.showRectBounds(productInfo.bounds)
        } else {
            _showProductLimit(canvasView, productInfo)
        }
    }

    /**绑定布局*/
    @CanvasEntryPoint
    fun bindCanvasView(canvasView: CanvasView) {
        //监听产品信息
        laserPeckerModel.productInfoData.observe(lifecycleOwner) { productInfo ->
            _showProductLimit(canvasView, productInfo)
        }
        //监听Z轴
        laserPeckerModel.deviceSettingData.observe(lifecycleOwner) {
            val before = laserPeckerModel.deviceSettingData.beforeValue?.zFlag ?: 0
            if (before != it?.zFlag) {
                //z轴开关改变后, 检查是否要限制z轴限制
                _showZLimit(canvasView)
            }
        }
        laserPeckerModel.deviceStateData.observe(lifecycleOwner) {
            val before = laserPeckerModel.deviceStateData.beforeValue?.zConnect ?: 0
            if (before != it?.zConnect) {
                //z轴连接状态改变后, 检查是否要限制z轴限制
                _showZLimit(canvasView)
            }
        }
    }

    /**监听设备状态, 并做出相应*/
    @CanvasEntryPoint
    fun bindDeviceState(rootLayout: ViewGroup) {
        //监听设备状态
        laserPeckerModel.deviceStateData.observe(lifecycleOwner) {
            it?.let {
                engraveProgressItem.itemEnableProgressFlowMode = it.isEngraving()
                if (it.isEngraving()) {
                    updateEngraveProgress(
                        clamp(it.rate, 0, 100),
                        _string(R.string.print_v2_package_printing),
                        time = engraveModel.calcEngraveRemainingTime(it.rate)
                    ) {
                        itemProgressAnimDuration = Anim.ANIM_DURATION
                    }
                    engraveModel.updateEngraveReadyDataInfo {
                        //更新打印次数
                        engraveReadyDataInfo?.printTimes = it.printTimes
                    }
                    checkDeviceState()
                } else if (it.isEngravePause()) {
                    updateEngraveProgress(
                        tip = _string(R.string.print_v2_package_print_state),
                        time = -1
                    )
                } else if (it.isEngraveStop()) {
                    updateEngraveProgress(
                        100,
                        tip = _string(R.string.print_v2_package_print_over),
                        time = null
                    )
                    engraveModel.stopEngrave()

                    //退出打印模式, 进入空闲模式
                    ExitCmd().enqueue()
                    laserPeckerModel.queryDeviceState()
                } else if (it.isModeIdle()) {
                    //空闲模式
                    //dslAdapter?.removeItem { it is EngravingItem }
                    updateEngraveProgress(
                        100,
                        tip = _string(R.string.print_v2_package_print_over),
                        time = null,
                        autoInsert = false
                    )
                }
                //更新界面
                dslAdapter?.updateItem { it is EngravingItem }

                //提示
                if (it.isModeEngravePreview()) {
                    //预览模式
                    showDangerWaring(rootLayout, Color.BLUE.alphaRatio(0.4f))
                } else if (it.isModeEngrave()) {
                    //雕刻模式
                    showDangerWaring(rootLayout, Color.RED.alphaRatio(0.4f))
                } else {
                    //空闲
                    dangerWarningView?.removeFromParent()
                }
            }.elseNull {
                dangerWarningView?.removeFromParent()
            }
        }
    }

    /**显示危险提示view*/
    fun showDangerWaring(rootLayout: ViewGroup, color: Int) {
        if (dangerWarningView == null) {
            dangerWarningView = DangerWarningView(rootLayout.context)
        }
        dangerWarningView?.firstDrawable<DangerWarningDrawable>()?.warnColor = color
        if (dangerWarningView?.parent == null) {
            rootLayout.addView(dangerWarningView, -1, -1)
        }
    }

    override fun onIViewShow() {
        super.onIViewShow()
        initLayout()
    }

    /**更新雕刻数据的索引*/
    fun updateEngraveDataIndex(dataInfo: EngraveDataInfo?) {
        dataInfo?.index = EngraveHelper.generateEngraveIndex()
        renderer?.getRendererItem()?.engraveIndex = dataInfo?.index
    }

    /**初始化布局*/
    fun initLayout() {
        //init
        viewHolder?.throttleClick(R.id.close_layout_view) {
            hide()
        }

        viewHolder?.rv(R.id.lib_recycler_view)?.apply {
            noItemChangeAnim()
            renderDslAdapter {
                dslAdapter = this
            }
        }

        //close按钮
        showCloseLayout()

        if (engraveReadyDataInfo == null) {
            //未指定需要雕刻的数据, 则从Render中获取需要雕刻的数据
            engraveReadyDataInfo = EngraveHelper.generateEngraveReadyDataInfo(renderer)
        }

        //engrave
        if (laserPeckerModel.deviceStateData.value?.isModeIdle() == true) {
            //设备空闲
            showEngraveOptionItem()
        } else if (laserPeckerModel.deviceStateData.value?.isModeEngrave() == true) {
            //设备雕刻中
            showEngravingItem()
        } else {
            //其他模式下, 先退出其他模式, 再next
            ExitCmd().enqueue()
            laserPeckerModel.queryDeviceState()
            showEngraveOptionItem()
        }
    }

    /**生成百分比数值列表*/
    fun percentList(max: Int = 100): List<Int> {
        return (1..max).toList()
    }

    /**显示关闭按钮*/
    fun showCloseLayout(show: Boolean = true) {
        cancelable = show
        doMain {
            viewHolder?.visible(R.id.close_layout_view, show)
        }
    }

    //region ---Engrave---

    /**显示错误提示*/
    @AnyThread
    fun showEngraveError(error: String?) {
        dslAdapter?.render {
            this + engraveProgressItem.apply {
                itemUpdateFlag = true
                itemProgress = 0
                itemTip = error
                itemTime = null
            }
        }
        showCloseLayout()
    }

    /**更新显示进度提示*/
    @AnyThread
    fun updateEngraveProgress(
        progress: Int = engraveProgressItem.itemProgress,
        tip: CharSequence? = engraveProgressItem.itemTip,
        time: Long? = engraveProgressItem.itemTime,
        autoInsert: Boolean = true, //自动插入到界面
        action: EngraveProgressItem .() -> Unit = {}
    ) {
        dslAdapter?.apply {
            engraveProgressItem.apply {
                itemUpdateFlag = true
                itemProgress = progress
                itemTip = tip
                itemTime = time
                itemProgressAnimDuration = 0 //取消进度动画

                //dsl
                action()
            }

            //update
            if (adapterItems.contains(engraveProgressItem)) {
                doMain {
                    engraveProgressItem.updateAdapterItem()
                }
            } else if (autoInsert) {
                render {
                    insertItem(0, engraveProgressItem)
                }
            }
        }
    }

    //endregion

    //region ---Handle---

    /**处理雕刻数据*/
    @AnyThread
    fun showHandleEngraveItem(engraveReadyDataInfo: EngraveReadyDataInfo) {
        dslAdapter?.clearAllItems()
        updateEngraveProgress(100, _string(R.string.v4_bmp_edit_tips))

        fun needHandleEngraveData() {
            lifecycleOwner.launchLifecycle {
                val dataInfo = withBlock {
                    if (renderer == null) {
                        //此时可能来自历史文档的数据
                        val dataPathFile = engraveReadyDataInfo.dataPath?.file()
                        if (dataPathFile?.exists() == true) {
                            engraveReadyDataInfo.engraveData?.data = dataPathFile.readBytes()
                        }
                    } else {
                        EngraveHelper.handleEngraveData(renderer, engraveReadyDataInfo)
                    }
                    engraveReadyDataInfo.engraveData
                }
                if (dataInfo?.data == null) {
                    showEngraveError("data exception!")
                } else {
                    sendEngraveData(engraveReadyDataInfo)
                }
            }
        }

        val index = engraveReadyDataInfo.engraveData?.index
        if (index == null) {
            //需要重新发送数据
            updateEngraveDataIndex(engraveReadyDataInfo.engraveData)
            needHandleEngraveData()
        } else {
            //检查数据索引是否存在
            QueryCmd.fileList.enqueue { bean, error ->
                val have =
                    bean?.parse<QueryEngraveFileParser>()?.nameList?.contains(index) == true
                if (have) {
                    //已经存在数据, 更新数据配置即可. 直接显示雕刻相关item
                    if (renderer != null) {
                        //重写解析一下, 但是数据不需要发送给机器
                        EngraveHelper.handleEngraveData(renderer, engraveReadyDataInfo)
                    }
                    engraveModel.setEngraveReadyDataInfo(engraveReadyDataInfo)
                    showEngraveItem()
                } else {
                    needHandleEngraveData()
                }
            }
        }
    }

    /**发送雕刻数据*/
    @AnyThread
    fun sendEngraveData(engraveReadyDataInfo: EngraveReadyDataInfo) {
        val engraveData = engraveReadyDataInfo.engraveData
        if (engraveData == null) {
            showEngraveError("data exception")
            return
        }
        val index = engraveData.index
        if (index == null) {
            showEngraveError("数据索引异常")
            return
        }

        showCloseLayout(false)//传输中不允许关闭
        engraveModel.setEngraveReadyDataInfo(engraveReadyDataInfo)
        updateEngraveProgress(0, _string(R.string.print_v2_package_transfer))
        val cmd = FileModeCmd(engraveData.data?.size ?: 0)
        cmd.enqueue { bean, error ->
            bean?.parse<FileTransferParser>()?.let {
                if (it.isIntoFileMode()) {
                    //成功进入大数据模式

                    //数据类型封装
                    val dataCommand: DataCommand? = when (engraveData.dataType) {
                        EngraveDataInfo.TYPE_BITMAP -> {
                            engraveModel.engraveOptionInfoData.value?.x = engraveData.x
                            engraveModel.engraveOptionInfoData.value?.y = engraveData.y
                            DataCommand.bitmapData(
                                index,
                                engraveData.data,
                                engraveData.width,
                                engraveData.height,
                                engraveData.x,
                                engraveData.y,
                                engraveData.px,
                                engraveData.name,
                            )
                        }
                        EngraveDataInfo.TYPE_GCODE -> DataCommand.gcodeData(
                            index,
                            engraveData.name,
                            engraveData.lines,
                            engraveData.data,
                        )
                        else -> null
                    }

                    //开始发送数据
                    if (dataCommand != null) {
                        dataCommand.enqueue({
                            //进度
                            updateEngraveProgress(it.sendPacketPercentage, time = it.remainingTime)
                        }) { bean, error ->
                            val result = bean?.parse<FileTransferParser>()
                            L.w("传输结束:$result $error")
                            result?.let {
                                if (it.isFileTransferSuccess()) {
                                    //文件传输完成
                                    showEngraveItem()
                                } else {
                                    showEngraveError("数据传输失败")
                                }
                            }
                            if (result == null) {
                                showEngraveError("数据传输失败")
                            }
                        }
                    } else {
                        showEngraveError("数据有误")
                    }
                } else {
                    showEngraveError("数据传输失败")
                }
            }.elseNull {
                showEngraveError(error?.message ?: "数据传输失败")
            }
        }
    }

    /**显示开始雕刻相关的item*/
    fun showEngraveItem() {
        showCloseLayout()

        val engraveOptionInfo = engraveModel.engraveOptionInfoData.value
        val engraveReadyInfo = engraveModel.engraveReadyInfoData.value

        dslAdapter?.render {
            clearAllItems()

            //激光类型
            if (laserPeckerModel.productInfoData.value?.isLIIIMax() == true) {
                EngraveOptionTypeItem()() {
                    itemEngraveOptionInfo = engraveOptionInfo
                }
            } else {
                engraveOptionInfo?.type = 0x01
            }

            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = _stringArray(R.array.sourceMaterial).toList()
                val material = engraveOptionInfo?.material ?: _string(R.string.material_custom)
                itemSelectedIndex = itemWheelList?.indexOf(material) ?: 0
                itemTag = EngraveOptionInfo::material.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_power)
                itemWheelList = percentList()
                itemSelectedIndex = findOptionIndex(itemWheelList, engraveOptionInfo?.power)
                itemTag = EngraveOptionInfo::power.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_speed)
                itemWheelList = percentList()
                itemSelectedIndex = findOptionIndex(itemWheelList, engraveOptionInfo?.depth)
                itemTag = EngraveOptionInfo::depth.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.print_times)
                itemWheelList = percentList(255)
                itemSelectedIndex = findOptionIndex(itemWheelList, engraveOptionInfo?.time)
                itemTag = EngraveOptionInfo::time.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveConfirmItem()() {
                engraveAction = {
                    //开始雕刻
                    engraveOptionInfo?.let { option ->
                        engraveReadyInfo?.let { readyDataInfo ->
                            //start check
                            checkStartEngrave(readyDataInfo.engraveData!!.index!!, option)
                        }
                    }
                }
            }
        }

        //进入空闲模式
        ExitCmd().enqueue()
        laserPeckerModel.queryDeviceState()
    }

    /**显示雕刻数据处理前选项相关的item*/
    fun showEngraveOptionItem() {
        val dataInfo = engraveReadyDataInfo?.engraveData
        if (dataInfo != null && engraveReadyDataInfo?.historyEntity != null) {
            //来自历史文档的雕刻数据
            showHandleEngraveItem(engraveReadyDataInfo!!)
            return
        }

        dslAdapter?.render {
            clearAllItems()

            if (dataInfo == null) {
                showEngraveError("数据处理失败")
            } else {
                /*EngraveDataPreviewItem()() {
                    itemEngraveDataInfo = dataInfo
                }*/
                EngraveDataNameItem()() {
                    itemEngraveReadyDataInfo = engraveReadyDataInfo
                }
                /*EngraveDataModeItem()() {
                    itemEngraveDataInfo = dataInfo
                }*/
                if (engraveReadyDataInfo?.optionSupportPxList.isNullOrEmpty().not()) {
                    val defPx = dataInfo.px
                    EngraveDataPxItem()() {
                        itemEngraveDataInfo = dataInfo
                        itemPxList = engraveReadyDataInfo?.optionSupportPxList

                        observeItemChange {
                            //当px改变之后, 需要更新数据索引
                            if (defPx != dataInfo.px) {
                                updateEngraveDataIndex(dataInfo)
                            }
                        }
                    }
                }
                EngraveDataNextItem()() {
                    itemClick = {
                        if (!checkItemThrowable()) {
                            //next
                            renderer?.getRendererItem()?.itemName = dataInfo.name
                            showHandleEngraveItem(engraveReadyDataInfo!!)
                        }
                    }
                }
                renderEmptyItem(_dimen(R.dimen.lib_xxhdpi))
            }
        }
    }

    /**显示雕刻中相关的item*/
    fun showEngravingItem() {
        dslAdapter?.render {
            clearAllItems()
            updateEngraveProgress(0, _string(R.string.print_v2_package_printing), time = -1)
            EngravingItem()() {
                againAction = {
                    showEngraveItem()
                }
            }
        }
    }

    fun findOptionIndex(list: List<Any>?, value: Byte?): Int {
        return list?.indexOfFirst { it.toString().toInt() == value?.toHexInt() } ?: -1
    }

    //endregion

    /**检查开始雕刻
     * [index] 需要雕刻的数据索引
     * [option] 需要雕刻的数据选项*/
    fun checkStartEngrave(index: Int, option: EngraveOptionInfo) {
        val zFlag = laserPeckerModel.deviceSettingData.value?.zFlag
        if (zFlag == 1) {
            //Z轴开关打开
            val zConnect = laserPeckerModel.deviceStateData.value?.zConnect
            if (zConnect != 1) {
                //未连接z轴, 弹窗提示
                viewHolder?.context?.messageDialog {
                    dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                    dialogMessage = _string(R.string.zflag_discontent_tips)

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }

        //安全提示弹窗
        viewHolder?.context?.messageDialog {
            dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
            dialogTitle = _string(R.string.size_safety_tips)
            dialogMessage = _string(R.string.size_safety_content)
            negativeButtonText = _string(R.string.dialog_negative)

            positiveButton { dialog, dialogViewHolder ->
                dialog.dismiss()
                _startEngrave(index, option)
            }
        }
    }

    fun _startEngrave(index: Int, option: EngraveOptionInfo) {
        EngraveCmd(
            index,
            option.power,
            option.depth,
            option.state,
            option.x,
            option.y,
            max(1, option.time.toHexInt()).toByte(),
            option.type,
        ).enqueue { bean, error ->
            L.w("开始雕刻:${bean?.parse<MiniReceiveParser>()}")

            if (error == null) {
                engraveModel.startEngrave()
                showEngravingItem()
                laserPeckerModel.queryDeviceState()
            }
        }
    }

}