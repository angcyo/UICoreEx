package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.command.*
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.library.ex.*

/**
 * 雕刻布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
class EngraveFlowLayoutHelper : BaseEngraveLayoutHelper() {

/*    */
    /**雕刻对象*//*
    var renderer: BaseItemRenderer<*>? = null
        set(value) {
            field = value
            if (value != null) {
                engraveReadyInfo = null
            }
        }

    */
    /**进度item*//*
    val engraveProgressItem = EngraveProgressItem()

    *//**雕刻需要准备的数据*//*
    var engraveReadyInfo: EngraveReadyInfo? = null*/

    init {
        iViewLayoutId = R.layout.canvas_engrave_flow_layout
    }

    /**监听设备状态, 并做出相应*/
    override fun bindDeviceState() {
        super.bindDeviceState()
        //监听设备状态
        /*laserPeckerModel.deviceStateData.observe(this) {
            val beforeState = laserPeckerModel.deviceStateData.beforeValue
            it?.let {
                engraveProgressItem.itemEnableProgressFlowMode = it.isEngraving()

                if (beforeState?.isModeEngrave() == true && (it.isEngraveStop() || it.isModeIdle())) {
                    //雕刻结束
                    UMEvent.ENGRAVE.umengEventValue {
                        val nowTime = nowTime()
                        put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                        put(
                            UMEvent.KEY_DURATION,
                            (nowTime - beforeState.stateTime).toString()
                        )
                    }
                }

                if (it.isEngraving()) {
                    //雕刻中
                    val progress = clamp(it.rate, 0, 100)
                    updateEngraveProgress(
                        progress,
                        _string(R.string.print_v2_package_printing),
                        time = engraveModel.calcEngraveRemainingTime(it.rate)
                    ) {
                        itemProgressAnimDuration = Anim.ANIM_DURATION
                    }
                    engraveModel.updateEngraveReadyDataInfo {
                        //更新打印次数
                        engraveReadyInfo?.printTimes = it.printTimes
                    }
                    engraveModel.updateEngraveProgress(progress)
                    checkDeviceState()
                } else if (it.isEngravePause()) {
                    //雕刻暂停中
                    updateEngraveProgress(
                        tip = _string(R.string.print_v2_package_print_state),
                        time = -1
                    )
                } else if (it.isEngraveStop()) {
                    //雕刻停止中
                    updateEngraveProgress(
                        100,
                        tip = _string(R.string.print_v2_package_print_over),
                        time = null
                    )
                    engraveModel.stopEngrave()

                    //退出打印模式, 进入空闲模式
                    ExitCmd().enqueue()
                    laserPeckerModel.queryDeviceState()

                    if (engraveModel.isRestore()) {
                        //恢复的雕刻状态
                        hide()
                    }
                } else if (it.isModeIdle()) {
                    //空闲模式
                    if (beforeState?.isModeEngrave() == true) {
                        engraveModel.stopEngrave()
                        checkDeviceState()
                    }
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
                _dslAdapter?.updateItem { it is EngravingItem }
            }
        }*/
    }

    /**初始化布局
     * [onIViewShow]*/
    fun initLayout() {
        /*renderDslAdapter {
            //
        }

        //close按钮
        showCloseView()

        if (engraveReadyInfo == null) {
            //未指定需要雕刻的数据, 则从Render中获取需要雕刻的数据
            engraveReadyInfo = engraveTransitionManager.transitionReadyData(renderer)
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
        }*/
    }

    /**处理雕刻数据*/
    /*
    @AnyThread
    fun showHandleEngraveItem(engraveReadyInfo: EngraveReadyInfo) {
        _dslAdapter?.clearAllItems()
        updateEngraveProgress(100, _string(R.string.v4_bmp_edit_tips)) {
            itemEnableProgressFlowMode = true
        }

        //开始处理需要发送的bytes数据
        fun needHandleEngraveData() {
            runRx({
                if (renderer == null) {
                    //此时可能来自历史文档的数据
                    val dataPathFile = engraveReadyInfo.dataPath?.file()
                    if (dataPathFile?.exists() == true) {
                        engraveReadyInfo.engraveData?.data = dataPathFile.readBytes()
                    }
                } else {
                    engraveTransitionManager.transitionEngraveData(renderer, engraveReadyInfo)
                }
                engraveReadyInfo.engraveData
            }) { dataInfo ->
                if (dataInfo?.data == null) {
                    showEngraveError("data exception!")
                    "雕刻数据处理结果为null".writeErrorLog()
                } else {
                    sendEngraveData(engraveReadyInfo)
                }
            }
        }

        val index = engraveReadyInfo.engraveData?.index
        if (index == null) {
            //需要重新发送数据
            updateEngraveDataIndex(engraveReadyInfo.engraveData)
            needHandleEngraveData()
        } else {

        }
    }

    */
    /**发送雕刻数据*//*
    @AnyThread
    fun sendEngraveData(engraveReadyInfo: EngraveReadyInfo) {
        val engraveData = engraveReadyInfo.engraveData
        if (engraveData == null) {
            showEngraveError("data exception")
            "无需要发送的雕刻数据".writeErrorLog()
            return
        }
        val index = engraveData.index
        if (index == null) {
            "雕刻数据索引为null".writeErrorLog()
            showEngraveError("数据索引异常")
            return
        }

        showCloseView(false)//传输中不允许关闭
        engraveModel.setEngraveReadyDataInfo(engraveReadyInfo)
        updateEngraveProgress(0, _string(R.string.print_v2_package_transfer)) {
            itemEnableProgressFlowMode = true
        }
        val cmd = FileModeCmd(engraveData.data?.size ?: 0)
        cmd.enqueue { bean, error ->
            bean?.parse<FileTransferParser>()?.let {
                if (it.isIntoFileMode()) {
                    //成功进入大数据模式

                    engraveModel.engraveOptionInfoData.value?.x = engraveData.x
                    engraveModel.engraveOptionInfoData.value?.y = engraveData.y

                    //数据类型封装
                    val dataCmd: DataCmd? = EngraveHelper.getEngraveDataCmd(engraveData)

                    //开始发送数据
                    if (dataCmd != null) {
                        dataCmd.enqueue(null, {
                            //进度
                            updateEngraveProgress(
                                it.sendPacketPercentage,
                                time = it.remainingTime
                            ) {
                                itemEnableProgressFlowMode = true
                            }
                        }) { bean, error ->
                            val result = bean?.parse<FileTransferParser>()
                            L.w("传输结束:$result $error")
                            result?.let {
                                if (it.isFileTransferSuccess()) {
                                    //文件传输完成
                                    showStartEngraveItem()
                                } else {
                                    "数据接收未完成".writeErrorLog()
                                    showEngraveError("数据传输失败")
                                }
                            }
                            if (result == null) {
                                "发送数据失败".writeErrorLog()
                                showEngraveError("数据传输失败")
                            }
                        }
                    } else {
                        "数据指令为null".writeErrorLog()
                        showEngraveError("数据有误")
                    }
                } else {
                    "未成功进入数据传输模式".writeErrorLog()
                    showEngraveError("数据传输失败")
                }
            }.elseNull {
                "进入数据传输模式失败".writeErrorLog()
                showEngraveError(error?.message ?: "数据传输失败")
            }
        }
    }

    */
    /**显示开始雕刻相关的item*//*
    fun showStartEngraveItem() {
        showCloseView()

        val engraveOptionInfo = engraveModel.engraveOptionInfoData.value
        val engraveReadyInfo = engraveModel.engraveReadyInfoData.value

        //材质列表
        val materialList = EngraveHelper.getProductMaterialList()

        _dslAdapter?.render {
            clearAllItems()

            //物体直径, 这里应该判断z轴设备的类型, 决定是否显示物理直径
            val showDiameter = laserPeckerModel.isROpen()

            //激光类型
            if (laserPeckerModel.productInfoData.value?.typeList.size() > 1) {
                EngraveOptionTypeItem()() {
                    itemEngraveOptionInfo = engraveOptionInfo

                    observeItemChange {
                        //更新过滤
                    }
                }
            } else {
                engraveOptionInfo?.type = LaserPeckerHelper.LASER_TYPE_BLUE
            }

            if (showDiameter) {
                engraveOptionInfo?.diameterPixel = HawkEngraveKeys.lastDiameterPixel
                EngraveOptionDiameterItem()() {
                    itemEngraveOptionInfo = engraveOptionInfo
                }
            }

            DslNestedGridRecyclerItem()() {
                itemGridSpanCount = 2
                itemWidth = ViewGroup.LayoutParams.MATCH_PARENT

                renderNestedAdapter {
                    //材质
                    EngraveOptionWheelItem()() {
                        itemLabelText = _string(R.string.custom_material)
                        itemWheelList = materialList
                        val material =
                            engraveOptionInfo?.material ?: _string(R.string.material_custom)
                        val index = materialList.indexOfFirst { it.toText() == material }
                        if (index == -1) {
                            engraveOptionInfo?.material =
                                materialList.getOrNull(0)?.toText()?.toString()
                                    ?: _string(R.string.material_custom)
                        }
                        itemSelectedIndex = max(0, index)
                        itemTag = EngraveOptionInfo::material.name
                        itemEngraveOptionInfo = engraveOptionInfo
                    }
                    EngraveOptionWheelItem()() {
                        itemLabelText = _string(R.string.custom_power)
                        itemWheelList = percentList()
                        itemSelectedIndex =
                            EngraveHelper.findOptionIndex(itemWheelList, engraveOptionInfo?.power)
                        itemTag = EngraveOptionInfo::power.name
                        itemEngraveOptionInfo = engraveOptionInfo
                    }
                    EngraveOptionWheelItem()() {
                        itemLabelText = _string(R.string.custom_speed)
                        itemWheelList = percentList()
                        itemSelectedIndex =
                            EngraveHelper.findOptionIndex(itemWheelList, engraveOptionInfo?.depth)
                        itemTag = EngraveOptionInfo::depth.name
                        itemEngraveOptionInfo = engraveOptionInfo
                    }
                    EngraveOptionWheelItem()() {
                        itemLabelText = _string(R.string.print_times)
                        itemWheelList = percentList(255)
                        itemSelectedIndex =
                            EngraveHelper.findOptionIndex(itemWheelList, engraveOptionInfo?.time)
                        itemTag = EngraveOptionInfo::time.name
                        itemEngraveOptionInfo = engraveOptionInfo
                    }
                }
            }
            EngraveConfirmItem()() {
                engraveAction = {
                    //开始雕刻
                    engraveOptionInfo?.let { option ->
                        if (showDiameter && option.diameterPixel <= 0) {
                            toast("diameter need > 0")
                        } else {
                            engraveReadyInfo?.let { readyDataInfo ->
                                //start check
                                checkStartEngrave(readyDataInfo.engraveData!!.index!!, option)
                            }
                        }
                    }
                }
            }
        }

        //进入空闲模式
        ExitCmd().enqueue()
        laserPeckerModel.queryDeviceState()
    }

    */
    /**显示雕刻数据处理前选项相关的item*//*
    fun showEngraveOptionItem() {
        val dataInfo = engraveReadyInfo?.engraveData
        if (dataInfo != null && engraveReadyInfo?.historyEntity != null) {
            //来自历史文档的雕刻数据
            showHandleEngraveItem(engraveReadyInfo!!)
            return
        }

        _dslAdapter?.render {
            clearAllItems()

            if (dataInfo == null) {
                "无需要雕刻的数据".writeErrorLog()
                showEngraveError("数据处理失败")
            } else {
                *//*EngraveDataPreviewItem()() {
                    itemEngraveDataInfo = dataInfo
                }*//*
                EngraveDataNameItem()() {
                    itemEngraveReadyInfo = engraveReadyInfo
                }
                if (isDebug()) {
                    //用来调整发送的数据类型
                    EngraveDataModeItem()() {
                        itemEngraveReadyInfo = engraveReadyInfo
                    }
                }
                if (engraveReadyInfo?.dataSupportPxList.isNullOrEmpty().not()) {
                    val defPx = dataInfo.px
                    EngraveDataPxItem()() {
                        itemEngraveDataInfo = dataInfo
                        itemPxList = engraveReadyInfo?.dataSupportPxList

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
                            renderer?.getRendererRenderItem()?.itemLayerName = dataInfo.name
                            showHandleEngraveItem(engraveReadyInfo!!)
                        }
                    }
                }
                renderEmptyItem(_dimen(R.dimen.lib_xxhdpi))
            }
        }
    }

    //endregion

    */
    /**检查开始雕刻
     * [index] 需要雕刻的数据索引
     * [option] 需要雕刻的数据选项*//*
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

                    if (isDebug()) {
                        negativeButtonText = _string(com.angcyo.dialog.R.string.dialog_negative)
                        positiveButtonListener = { dialog, dialogViewHolder ->
                            dialog.dismiss()
                            checkSafeTip(index, option)
                        }
                    }

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }
        val rFlag = laserPeckerModel.deviceSettingData.value?.rFlag
        if (rFlag == 1) {
            //旋转轴开关打开
            val rConnect = laserPeckerModel.deviceStateData.value?.rConnect
            if (rConnect != 1) {
                //未连接r轴, 弹窗提示
                viewHolder?.context?.messageDialog {
                    dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                    dialogMessage = _string(R.string.zflag_discontent_tips)

                    if (isDebug()) {
                        negativeButtonText = _string(com.angcyo.dialog.R.string.dialog_negative)
                        positiveButtonListener = { dialog, dialogViewHolder ->
                            dialog.dismiss()
                            checkSafeTip(index, option)
                        }
                    }

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }
        val sFlag = laserPeckerModel.deviceSettingData.value?.sFlag
        if (sFlag == 1) {
            //滑台轴开关打开
            val sConnect = laserPeckerModel.deviceStateData.value?.sConnect
            if (sConnect != 1) {
                //未连接r轴, 弹窗提示
                viewHolder?.context?.messageDialog {
                    dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                    dialogMessage = _string(R.string.zflag_discontent_tips)

                    if (isDebug()) {
                        negativeButtonText = _string(com.angcyo.dialog.R.string.dialog_negative)
                        positiveButtonListener = { dialog, dialogViewHolder ->
                            dialog.dismiss()
                            checkSafeTip(index, option)
                        }
                    }

                    onDismissListener = {
                        laserPeckerModel.queryDeviceState()
                    }
                }
                return
            }
        }
        checkSafeTip(index, option)
    }

    */
    /**安全提示弹窗*//*
    fun checkSafeTip(index: Int, option: EngraveOptionInfo) {
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

    */

}