package com.angcyo.engrave

import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCommand
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.MiniReceiveParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.core.CanvasEntryPoint
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.core.vmApp
import com.angcyo.coroutine.launchLifecycle
import com.angcyo.coroutine.withBlock
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.updateItem
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.engrave.dslitem.EngraveConfirmItem
import com.angcyo.engrave.dslitem.EngraveOptionItem
import com.angcyo.engrave.dslitem.EngraveProgressItem
import com.angcyo.engrave.dslitem.EngravingItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.http.rx.doMain
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.L
import com.angcyo.library.component._delay
import com.angcyo.library.ex.Anim
import com.angcyo.library.ex._string
import com.angcyo.library.ex._stringArray
import com.angcyo.library.ex.elseNull
import com.angcyo.widget.layout.touch.SwipeBackLayout.Companion.clamp
import com.angcyo.widget.recycler.noItemChangeAnim
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * 雕刻布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
class EngraveLayoutHelper(val lifecycleOwner: LifecycleOwner) : BaseEngraveLayoutHelper() {

    /**雕刻对象*/
    var renderer: BaseItemRenderer<*>? = null

    /**进度item*/
    val engraveProgressItem = EngraveProgressItem()

    var dslAdapter: DslAdapter? = null

    //产品模式
    val peckerModel = vmApp<LaserPeckerModel>()

    //雕刻模型
    val engraveModel = vmApp<EngraveModel>()

    init {
        layoutId = R.layout.canvas_engrave_layout
    }

    /**绑定布局*/
    @CanvasEntryPoint
    fun bindCanvasView(canvasView: CanvasView) {
        //监听产品信息
        peckerModel.productInfoData.observe(lifecycleOwner) { productInfo ->
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
        //监听设备状态
        peckerModel.deviceStateData.observe(lifecycleOwner) {
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
                    engraveModel.updateEngraveDataInfo {
                        //更新打印次数
                        printTimes = it.printTimes
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
                    peckerModel.queryDeviceState()
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
            }
        }
    }

    override fun showLayout(viewGroup: ViewGroup, canvasDelegate: CanvasDelegate?) {
        super.showLayout(viewGroup, canvasDelegate)
        initLayout()
    }

    /**初始化布局*/
    fun initLayout() {
        //init
        viewHolder?.click(R.id.close_layout_view) {
            hideLayout()
        }

        viewHolder?.rv(R.id.lib_recycler_view)?.apply {
            noItemChangeAnim()
            renderDslAdapter {
                dslAdapter = this
            }
        }

        //close按钮
        showCloseLayout()

        //engrave
        if (peckerModel.deviceStateData.value?.isModeIdle() == true) {
            //设备空闲
            handleEngrave()
        } else if (peckerModel.deviceStateData.value?.isModeEngrave() == true) {
            //设备雕刻中
            showEngravingItem()
        } else {
            //其他模式下, 先退出其他模式, 再next
            ExitCmd().enqueue()
            handleEngrave()
        }
    }

    override fun hideLayout() {
        super.hideLayout()
    }

    fun percentList(): List<Int> {
        return (1..100).toList()
    }

    /**显示关闭按钮*/
    fun showCloseLayout(show: Boolean = true) {
        doMain {
            viewHolder?.visible(R.id.close_layout_view, show)
        }
    }

    /**持续检查工作作态*/
    fun checkDeviceState() {
        _delay(1_000) {
            //延迟1秒后, 继续查询状态
            peckerModel.queryDeviceState() { bean, error ->
                if (error != null) {
                    //出现了错误, 继续查询
                    checkDeviceState()
                }
            }
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
    fun handleEngrave() {
        lifecycleOwner.launchLifecycle {
            updateEngraveProgress(100, _string(R.string.v4_bmp_edit_tips))
            val dataInfo = withBlock {
                EngraveHelper.handleEngraveData(renderer)
            }
            if (dataInfo == null) {
                showEngraveError("数据处理失败")
            } else {
                sendEngraveData(dataInfo)
            }
        }
    }

    /**发送雕刻数据*/
    @AnyThread
    fun sendEngraveData(engraveData: EngraveDataInfo) {
        showCloseLayout(false)//传输中不允许关闭
        engraveModel.setEngraveDataInfo(engraveData)
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
                                engraveData.index,
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
                            engraveData.index,
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
        val engraveInfo = engraveModel.engraveInfoData.value

        dslAdapter?.render {
            clearAllItems()

            EngraveOptionItem()() {
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = _stringArray(R.array.sourceMaterial).toList()
                val material = engraveOptionInfo?.material ?: _string(R.string.material_custom)
                itemSelectedIndex = itemWheelList?.indexOf(material) ?: 0
                itemTag = EngraveOptionInfo::material.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveOptionItem()() {
                itemLabelText = _string(R.string.custom_power)
                itemWheelList = percentList()
                itemSelectedIndex = findOptionIndex(itemWheelList, engraveOptionInfo?.power)
                itemTag = EngraveOptionInfo::power.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveOptionItem()() {
                itemLabelText = _string(R.string.custom_speed)
                itemWheelList = percentList()
                itemSelectedIndex = findOptionIndex(itemWheelList, engraveOptionInfo?.depth)
                itemTag = EngraveOptionInfo::depth.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveOptionItem()() {
                itemLabelText = _string(R.string.print_times)
                itemWheelList = percentList()
                itemSelectedIndex = findOptionIndex(itemWheelList, engraveOptionInfo?.time)
                itemTag = EngraveOptionInfo::time.name
                itemEngraveOptionInfo = engraveOptionInfo
            }
            EngraveConfirmItem()() {
                engraveAction = {
                    //开始雕刻
                    engraveOptionInfo?.let { option ->
                        engraveInfo?.let { data ->
                            EngraveCmd(
                                data.index,
                                option.power,
                                option.depth,
                                option.state,
                                option.x,
                                option.y,
                                option.time,
                                option.type,
                            ).enqueue { bean, error ->
                                L.w("开始雕刻:${bean?.parse<MiniReceiveParser>()}")

                                if (error == null) {
                                    engraveModel.startEngrave()
                                    showEngravingItem()
                                    peckerModel.queryDeviceState()
                                }
                            }
                        }
                    }
                }
            }
        }

        //进入空闲模式
        ExitCmd().enqueue()
        peckerModel.queryDeviceState()
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
        return list?.indexOfFirst { it.toString().toInt() == value?.toInt() } ?: -1
    }

    //endregion

}