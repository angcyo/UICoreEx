package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.core.vmApp
import com.angcyo.dialog.inputDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.find
import com.angcyo.engrave.data.TransferState
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.engrave.dslitem.preview.PreviewExDeviceTipItem
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.engrave.dslitem.transfer.DataStopTransferItem
import com.angcyo.engrave.dslitem.transfer.DataTransmittingItem
import com.angcyo.engrave.dslitem.transfer.TransferDataNameItem
import com.angcyo.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.TransferModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.form.checkItemThrowable
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.L
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex.*
import com.angcyo.library.toast
import com.angcyo.library.toastQQ
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.observe
import com.angcyo.widget.span.span
import kotlin.math.max

/**
 * 雕刻布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
open class EngraveFlowLayoutHelper : BasePreviewLayoutHelper() {

    //数据传输模式
    val transferModel = vmApp<TransferModel>()

    override fun renderFlowItems() {
        when (engraveFlow) {
            ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG -> renderTransferConfig()
            ENGRAVE_FLOW_TRANSMITTING -> renderTransmitting()
            ENGRAVE_FLOW_BEFORE_CONFIG -> renderEngraveConfig()
            ENGRAVE_FLOW_ENGRAVING -> renderEngraving()
            ENGRAVE_FLOW_FINISH -> renderEngraveFinish()
            else -> super.renderFlowItems()
        }
    }

    override fun bindDeviceState() {
        super.bindDeviceState()
        //
        transferModel.transferStateOnceData.observe(this, allowBackward = false) {
            it?.apply {
                //数据传输进度通知
                if (it.error == null && it.state == TransferState.TRANSFER_STATE_FINISH) {
                    //默认选中第1个雕刻图层
                    selectLayerMode =
                        EngraveFlowDataHelper.getEngraveLayerList(taskId).firstOrNull()?.layerMode
                            ?: 0
                }
                if (engraveFlow == ENGRAVE_FLOW_TRANSMITTING) {
                    renderFlowItems()
                }
            }
        }
        //
        engraveModel.engraveStateData.observe(this, allowBackward = false) {
            it?.apply {
                if (taskId == flowTaskId) {
                    val engraveCmdError = engraveModel._lastEngraveCmdError
                    if (it.state == EngraveModel.ENGRAVE_STATE_ERROR && engraveCmdError != null) {
                        toastQQ(engraveCmdError.message)
                        engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                        renderFlowItems()
                    } else if (engraveFlow == ENGRAVE_FLOW_ENGRAVING) {
                        if (it.state == EngraveModel.ENGRAVE_STATE_FINISH) {
                            //雕刻完成
                            engraveFlow = ENGRAVE_FLOW_FINISH
                        }
                        renderFlowItems()
                    }
                }
            }
        }
    }

    //

    /**当前选中的图层模式
     * [EngraveLayerConfigItem]*/
    var selectLayerMode: Int = 0

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        super.onEngraveFlowChanged(from, to)
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            //在开始传输数据的时候, 创建任务
            /*if (flowTaskId != null) {
                //再次传输不一样的数据时, 重新创建任务id
                flowTaskId = EngraveFlowDataHelper.generateTaskId(flowTaskId)
            }*/
        } else if (to == ENGRAVE_FLOW_BEFORE_CONFIG) {
            //no op
        }
    }

    /**回调*/
    var onStartEngraveAction: (taskId: String?) -> Unit = {}

    /**开始雕刻前回调*/
    open fun onStartEngrave(taskId: String?) {
        onStartEngraveAction(taskId)
    }

    /**回调*/
    var onStartEngraveTransferDataAction: (taskId: String?) -> Unit = {}

    /**开始传输数据的回调*/
    open fun onStartEngraveTransferData(taskId: String?) {
        onStartEngraveTransferDataAction(taskId)
    }

    //region ---数据配置---

    /**渲染传输数据配置界面*/
    fun renderTransferConfig() {
        updateIViewTitle(_string(R.string.file_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val transferConfigEntity = EngraveFlowDataHelper.generateTransferConfig(
            flowTaskId,//此时的flowTaskId可以为空
            engraveCanvasFragment?.canvasDelegate
        )

        renderDslAdapter {
            TransferDataNameItem()() {
                itemTransferConfigEntity = transferConfigEntity

                observeItemChange {
                    engraveCanvasFragment?.canvasDelegate?.changedRenderItemData()
                }
            }
            TransferDataPxItem()() {
                itemPxList = LaserPeckerHelper.findProductSupportPxList()
                itemTransferConfigEntity = transferConfigEntity

                observeItemChange {
                    engraveCanvasFragment?.canvasDelegate?.changedRenderItemData()
                }
            }
            EngraveDividerItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.send_file)
                itemClick = {
                    if (!checkItemThrowable() && !checkOverflowBounds() && checkTransferData()) {
                        //下一步, 数据传输界面

                        //退出打印模式, 进入空闲模式
                        engraveCanvasFragment?.fragment?.engraveLoadingAsyncTimeout({
                            syncSingle { countDownLatch ->
                                ExitCmd().enqueue { bean, error ->
                                    countDownLatch.countDown()
                                    if (error == null) {
                                        engraveBackFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                                        engraveFlow = ENGRAVE_FLOW_TRANSMITTING

                                        val canvasDelegate = engraveCanvasFragment?.canvasDelegate
                                        if (canvasDelegate == null) {
                                            //不是画布上的数据, 可能是恢复的数据
                                        } else {
                                            val flowId = generateFlowId()//每次发送数据之前, 都生成一个新的任务
                                            transferConfigEntity.taskId = flowId
                                            transferConfigEntity.lpSaveEntity()
                                            onStartEngraveTransferData(flowId)
                                            transferModel.startCreateTransferData(
                                                flowId,
                                                canvasDelegate
                                            )
                                        }

                                        //last
                                        renderFlowItems()
                                    } else {
                                        toastQQ(error.message)
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    //endregion ---数据配置---

    //region ---数据传输中---

    /**渲染传输中的界面
     * 通过传输状态改变实时刷新界面
     * [com.angcyo.engrave.model.TransferModel.transferStateOnceData]
     * */
    fun renderTransmitting() {
        updateIViewTitle(_string(R.string.transmitting))
        showCloseView(false)

        val taskStateData = transferModel.transferStateOnceData.value
        if (taskStateData?.error == null && taskStateData?.state == TransferState.TRANSFER_STATE_FINISH) {
            //文件传输完成
            engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
            renderFlowItems()
        } else {
            renderDslAdapter {
                DataTransmittingItem()() {
                    itemProgress = taskStateData?.progress ?: 0
                }
                if (isDebug()) {
                    val monitorEntity = EngraveFlowDataHelper.getTransferMonitor(flowTaskId)
                    if (monitorEntity != null) {
                        PreviewTipItem()() {
                            itemTip = span {
                                if (monitorEntity.dataMakeStartTime > 0) {
                                    if (monitorEntity.dataMakeFinishTime > 0) {
                                        append("生成耗时:${monitorEntity.dataMakeDuration()} ")
                                    } else {
                                        append("数据生成中... ")
                                    }
                                }
                                if (monitorEntity.dataTransferSize > 0) {
                                    append(" 大小:${monitorEntity.dataSize()} ")
                                }
                                if (monitorEntity.dataTransferStartTime > 0) {
                                    appendln()
                                    append("传输耗时:")
                                    if (monitorEntity.dataTransferFinishTime > 0) {
                                        append("${monitorEntity.dataTransferDuration()} ")
                                    } else {
                                        append("${monitorEntity.dataTransferDuration(nowTime())} ")
                                    }
                                    if (monitorEntity.dataTransferSpeed > 0) {
                                        append(" 速率:${monitorEntity.speedString()} :${monitorEntity.maxSpeedString()}")
                                    } else {
                                        append(" 传输中... ")
                                    }
                                }
                            }
                        }
                    }
                }
                DataStopTransferItem()() {
                    itemException = taskStateData?.error
                    itemClick = {
                        vmApp<TransferModel>().stopTransfer()
                        engraveFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                        renderFlowItems()
                    }
                }
            }
        }
    }

    //endregion ---数据传输中---

    //region ---雕刻参数配置---

    /**渲染雕刻配置界面*/
    fun renderEngraveConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val taskId = flowTaskId

        //默认选中材质
        var materialEntity = EngraveFlowDataHelper.findTaskMaterial(taskId)
        L.w("材质:${taskId} $materialEntity")

        //雕刻配置信息
        val engraveConfigEntity = if (materialEntity == null) {
            //未初始化材质信息, 默认使用第一个
            materialEntity =
                EngraveFlowDataHelper.findLastMaterial() ?: EngraveHelper.materialList.firstOrNull()
                        ?: EngraveHelper.createCustomMaterial()
            EngraveFlowDataHelper.generateEngraveConfigByMaterial(
                taskId,
                materialEntity.key,
                materialEntity
            ).find {
                it.layerMode == selectLayerMode
            } ?: EngraveFlowDataHelper.generateEngraveConfig(taskId, selectLayerMode)
        } else {
            EngraveFlowDataHelper.generateEngraveConfig(taskId, selectLayerMode)
        }

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_tip)
            }
            if (!laserPeckerModel.isC1()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            if (laserPeckerModel.needShowExDeviceTipItem()) {
                PreviewExDeviceTipItem()()
            }
            //材质选择
            EngraveMaterialWheelItem()() {
                itemTag = MaterialEntity::name.name
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = EngraveHelper.unionMaterialList
                itemSelectedIndex =
                    EngraveHelper.indexOfMaterial(EngraveHelper.unionMaterialList, materialEntity)
                itemEngraveConfigEntity = engraveConfigEntity

                itemSaveAction = {
                    showSaveMaterialDialog(taskId, materialEntity) {
                        //刷新界面, 使用自定义的材质信息
                        renderFlowItems()
                    }
                }

                //刷新界面
                observeItemChange {
                    renderFlowItems()
                }
            }

            //雕刻图层切换
            val layerList = EngraveFlowDataHelper.getEngraveLayerList(taskId)
            if (layerList.isNotEmpty()) {
                EngraveLayerConfigItem()() {
                    itemSegmentList = layerList
                    itemCurrentIndex =
                        max(
                            0,
                            layerList.indexOf(layerList.find { it.layerMode == selectLayerMode })
                        )
                    observeItemChange {
                        selectLayerMode = layerList[itemCurrentIndex].layerMode
                        renderFlowItems()
                    }
                }
            }

            // 激光光源选择
            val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
            if (laserPeckerModel.productInfoData.value?.isCI() != true && typeList.isNotEmpty()) {
                EngraveSegmentScrollItem()() {
                    itemText = _string(R.string.laser_type)
                    itemSegmentList = typeList
                    itemCurrentIndex = typeList.indexOfFirst { it.type == engraveConfigEntity.type }
                    observeItemChange {
                        engraveConfigEntity.type = typeList[itemCurrentIndex].type
                        engraveConfigEntity.lpSaveEntity()
                        renderFlowItems()
                    }
                }
            }

            if (laserPeckerModel.isC1()) {
                //C1 加速级别选择
                EngraveOptionWheelItem()() {
                    itemTag = EngraveConfigEntity::precision.name
                    itemLabelText = _string(R.string.engrave_precision)
                    itemWheelList = EngraveHelper.percentList(5)
                    itemSelectedIndex =
                        EngraveHelper.findOptionIndex(itemWheelList, engraveConfigEntity.precision)
                    itemEngraveConfigEntity = engraveConfigEntity

                    observeMaterialChange()
                }
            }

            //雕刻参数
            if (laserPeckerModel.isPenMode()) {
                //雕刻速度, 非雕刻深度
                EngraveOptionWheelItem()() {
                    itemTag = MaterialEntity.SPEED
                    itemLabelText = _string(R.string.engrave_speed)
                    itemWheelList = EngraveHelper.percentList()
                    itemEngraveConfigEntity = engraveConfigEntity
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        EngraveCmd.depthToSpeed(engraveConfigEntity.depth)
                    )
                    observeMaterialChange()
                }
            } else {
                //功率/深度/次数
                EngravePropertyItem()() {
                    itemEngraveConfigEntity = engraveConfigEntity
                    observeMaterialChange()
                }
            }
            /*EngraveOptionWheelItem()() {
                itemTag = MaterialEntity::power.name
                itemLabelText = _string(R.string.custom_power)
                itemWheelList = percentList()
                itemEngraveConfigEntity = engraveConfigEntity
                itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveConfigEntity.power)
            }
            EngraveOptionWheelItem()() {
                itemTag = MaterialEntity::depth.name
                itemLabelText = _string(R.string.custom_speed)
                itemWheelList = percentList()
                itemEngraveConfigEntity = engraveConfigEntity
                itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveConfigEntity.depth)
            }
            //次数
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.print_times)
                itemWheelList = percentList(255)
                itemTag = EngraveConfigEntity::time.name
                itemEngraveConfigEntity = engraveConfigEntity
                itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveConfigEntity.time)
            }*/
            EngraveConfirmItem()() {
                itemClick = {
                    //开始雕刻
                    checkEngraveNotify {
                        checkExDevice {
                            showFocalDistance(it.context) {
                                showSafetyTips(it.context) {
                                    engraveCanvasFragment?.fragment?.engraveLoadingAsyncTimeout({
                                        syncSingle { countDownLatch ->
                                            ExitCmd().enqueue { bean, error ->
                                                countDownLatch.countDown()
                                                if (error == null) {
                                                    //开始雕刻
                                                    onStartEngrave(taskId)
                                                    val taskEntity =
                                                        engraveModel.startEngrave(taskId)
                                                    if (taskEntity.dataIndexList.isNullOrEmpty()) {
                                                        toastQQ(_string(R.string.no_data_engrave))
                                                    } else {
                                                        engraveFlow = ENGRAVE_FLOW_ENGRAVING
                                                        renderFlowItems()
                                                    }
                                                } else {
                                                    toastQQ(error.message)
                                                }
                                            }
                                        }
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**监听改变之后, 显示材质保存按钮*/
    fun DslAdapterItem.observeMaterialChange() {
        observeItemChange {
            itemDslAdapter?.find<EngraveMaterialWheelItem>()?.let {
                it.itemShowSaveButton = true
                it.updateAdapterItem()
            }
        }
    }

    /**显示保存自定义材质的对话框*/
    fun showSaveMaterialDialog(taskId: String?, materialEntity: MaterialEntity, action: Action) {
        engraveCanvasFragment?.fragment?.fContext()?.inputDialog {
            dialogTitle = _string(R.string.save_material_title)
            hintInputString = _string(R.string.material_title_limit)
            maxInputLength = 20
            canInputEmpty = false
            defaultInputString = materialEntity.toText() ?: _string(R.string.custom)
            onInputResult = { dialog, inputText ->
                EngraveFlowDataHelper.saveEngraveConfigToMaterial(taskId, "$inputText")
                action.invoke()
                false
            }
        }
    }

    //endregion ---雕刻参数配置---

    //region ---雕刻中---

    /**渲染雕刻中的界面
     * 通过设备状态改变实时刷新界面
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.deviceStateData]
     *
     * [com.angcyo.engrave.BaseFlowLayoutHelper.bindDeviceState]
     * */
    fun renderEngraving() {
        updateIViewTitle(_string(R.string.engraving))
        engraveBackFlow = ENGRAVE_FLOW_BEFORE_CONFIG
        showCloseView(false)

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_move_state_tips)
            }
            if (!laserPeckerModel.isC1()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            //强制显示模块信息
            PreviewExDeviceTipItem()() {
                itemEngraveConfigEntity = EngraveFlowDataHelper.getCurrentEngraveConfig(flowTaskId)
            }
            EngraveProgressItem()() {
                itemTaskId = flowTaskId
            }
            EngravingInfoItem()() {
                itemTaskId = flowTaskId
            }
            //---
            EngravingControlItem()() {
                itemTaskId = flowTaskId
                itemPauseAction = { isPause ->
                    if (isPause) {
                        engraveModel.continueEngrave()
                    } else {
                        engraveModel.pauseEngrave()
                    }
                }
                itemStopAction = {
                    //停止雕刻, 直接完成
                    engraveModel.stopEngrave()
                }
            }
        }
    }

    //endregion ---雕刻中---

    //region ---雕刻完成---

    /**渲染雕刻完成的界面*/
    open fun renderEngraveFinish() {
        updateIViewTitle(_string(R.string.engrave_finish))
        engraveBackFlow = 0
        if (isInPadMode()) {
            showCloseView(true, _string(R.string.ui_quit))
        } else {
            showCloseView(true, _string(R.string.back_creation))
        }

        val taskId = flowTaskId
        renderDslAdapter {
            EngraveFinishTopItem()() {
                itemTaskId = taskId
            }
            //
            EngraveFlowDataHelper.getEngraveLayerList(taskId).forEach { engraveLayerInfo ->
                EngraveLabelItem()() {
                    itemText = engraveLayerInfo.label
                }

                EngraveFinishInfoItem()() {
                    itemTaskId = taskId
                    itemLayerMode = engraveLayerInfo.layerMode
                }
            }
            //
            EngraveFinishControlItem()() {
                itemShareAction = {
                    toast("功能开发中...")
                }
                itemAgainAction = {
                    //再次雕刻, 回退到参数配置页面
                    EngraveFlowDataHelper.againEngrave(taskId) //清除缓存状态数据
                    engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                    renderFlowItems()
                }
            }
        }
    }

    //endregion ---雕刻完成---

}