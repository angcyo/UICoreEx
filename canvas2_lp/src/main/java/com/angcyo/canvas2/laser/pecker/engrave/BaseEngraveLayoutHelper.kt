package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.bluetooth.fsc.laserpacker._showPumpConfig
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.element.haveVariableElement
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveDividerItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveConfirmItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveFinishControlItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveFinishInfoItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveFinishTopItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLabelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLaserSegmentItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLayerConfigItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveMaterialWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveOptionWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveProgressItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravePropertyItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravePumpItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravingControlItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravingInfoItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.appendDrawable
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.GCodeDataOffsetItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewExDeviceTipItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.DataStopTransferItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.DataTransmittingItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataNameItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.canvas2.laser.pecker.manager.LPProjectManager
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.tgStrokeLoadingCaller
import com.angcyo.core.vmApp
import com.angcyo.dialog.inputDialog
import com.angcyo.dialog.messageDialog
import com.angcyo.dialog.toastQQOrMessage
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.RecyclerItemFlowAnimator
import com.angcyo.dsladapter.find
import com.angcyo.dsladapter.itemIndexPosition
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.model.EngraveModel
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.form.checkItemThrowable
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.laserpacker.device.engraveLoadingAsyncTimeout
import com.angcyo.laserpacker.device.exception.TransferException
import com.angcyo.laserpacker.device.filterLayerDpi
import com.angcyo.library.L
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.size
import com.angcyo.library.ex.syncSingle
import com.angcyo.library.ex.uuid
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.observe
import com.angcyo.widget.span.span
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue
import kotlin.math.max

/**
 * 雕刻流程控制
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/08
 */
abstract class BaseEngraveLayoutHelper : BasePreviewLayoutHelper() {

    //数据传输模式
    val transferModel = vmApp<TransferModel>()

    override fun renderFlowItems() {
        if (isAttach()) {
            when (engraveFlow) {
                ENGRAVE_FLOW_ITEM_CONFIG -> renderEngraveItemParamsConfig()
                ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG -> renderTransferConfig()
                ENGRAVE_FLOW_AUTO_TRANSFER -> renderAutoTransfer()
                ENGRAVE_FLOW_TRANSMITTING -> renderTransmitting()
                ENGRAVE_FLOW_BEFORE_CONFIG -> renderEngraveConfig()
                ENGRAVE_FLOW_ENGRAVING -> renderEngraving()
                ENGRAVE_FLOW_FINISH -> renderEngraveFinish()
                else -> super.renderFlowItems()
            }
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
                    selectLayerId =
                        EngraveFlowDataHelper.getEngraveLayerList(taskId).firstOrNull()?.layerId
                            ?: LaserPeckerHelper.LAYER_FILL
                }
                if (engraveFlow == ENGRAVE_FLOW_TRANSMITTING) {
                    //在[renderTransmitting] 中 engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                    renderFlowItems()
                }
            }
        }
        //
        engraveModel.engraveStateData.observe(this, allowBackward = false) {
            it?.apply {
                "雕刻状态改变,当前流程id[$flowTaskId]:$this".writeBleLog()
                if (taskId == flowTaskId) {
                    val engraveCmdError = engraveModel._lastEngraveCmdError
                    if (it.state == EngraveModel.ENGRAVE_STATE_ERROR && engraveCmdError != null) {
                        toastQQOrMessage(engraveCmdError.message)
                        engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                        renderFlowItems()
                    } else if (engraveFlow == ENGRAVE_FLOW_ENGRAVING) {
                        when (it.state) {
                            EngraveModel.ENGRAVE_STATE_FINISH -> {
                                //雕刻完成
                                engraveFlow = ENGRAVE_FLOW_FINISH
                                renderFlowItems()
                            }

                            EngraveModel.ENGRAVE_STATE_INDEX_FINISH -> {
                                //当前索引雕刻完成, 传输下一个文件
                                if (HawkEngraveKeys.enableSingleItemTransfer) {
                                    startTransferNext()
                                }
                            }

                            else -> {
                                renderFlowItems()
                            }
                        }
                    }
                }
            }
        }
    }

    //

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
    var onStartEngraveTransferDataAction: (taskId: String?) -> Unit = {}

    /**开始传输数据的回调*/
    open fun onStartEngraveTransferData(taskId: String?) {
        onStartEngraveTransferDataAction(taskId)
    }

    //region ---数据配置---

    /**渲染传输数据配置界面*/
    open fun renderTransferConfig() {
        deviceStateModel.pauseLoopCheckState(true, "传输配置界面")

        updateIViewTitle(_string(R.string.file_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val delegate = engraveCanvasFragment?.renderDelegate

        //此时的flowTaskId可以为空
        val transferConfigEntity = engraveConfigProvider.getTransferConfig(this)

        //全部是GCode数据, 不能选择分辨率, 并且强制使用1k
        val isAllGCode = LPEngraveHelper.isAllSameLayerMode(
            delegate,
            LPDataConstant.DATA_MODE_GCODE
        )

        renderDslAdapter {
            TransferDataNameItem()() {
                itemTransferConfigEntity = transferConfigEntity

                observeItemChange {
                    clearFlowId("传输文件名改变")
                    delegate?.dispatchAllRendererDataChange(Reason.user)
                }
            }
            if (isAllGCode) {
                //全部是GCode数据, 则支持偏移
                if (HawkEngraveKeys.enableCalibrationOffset &&
                    laserPeckerModel.productInfoData.value?.isCSeries() == true
                ) {
                    GCodeDataOffsetItem()() {
                        observeItemChange {
                            clearFlowId("GCode数据传输偏移改变")
                            delegate?.dispatchAllRendererDataChange(Reason.user)
                        }
                    }
                }
            } else {
                //并非全部是GCode数据
                val elementLayerInfoList = LPEngraveHelper.getSelectElementLayerInfoList(delegate)
                for (layerInfo in elementLayerInfoList) {
                    if (layerInfo.showDpiConfig) {
                        TransferDataPxItem()() {
                            val layerSupportPxList =
                                LaserPeckerHelper.findProductLayerSupportPxList(layerInfo.layerId)
                            itemPxList =
                                if (!HawkEngraveKeys.enableZFlagPx && laserPeckerModel.isZOpen()) {
                                    //L3 C1 z轴打开的情况下, 取消4k 2023-1-4 / 2023-3-10
                                    layerSupportPxList
                                        .filter { it.px > LaserPeckerHelper.PX_4K } //2023-4-6 z轴不支持4K及以上
                                } else {
                                    layerSupportPxList
                                }

                            itemTransferConfigEntity = transferConfigEntity
                            itemLayerInfo = layerInfo

                            observeItemChange {
                                clearFlowId("[${layerInfo.layerId}]传输Dpi改变")
                                delegate?.dispatchAllRendererDataChange(Reason.user) {
                                    it.lpElementBean()?._layerId == layerInfo.layerId
                                }
                            }
                        }
                    }
                }
                EngraveDividerItem()()
            }
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.send_file)
                itemClick = {
                    if (!checkItemThrowable() && checkCanNext()) {
                        //下一步, 数据传输界面

                        //退出打印模式, 进入空闲模式
                        asyncTimeoutExitCmd { bean, error ->
                            if (error == null) {
                                changeToTransmitting(transferConfigEntity)
                            } else {
                                toastQQOrMessage(error.message)
                            }
                        }
                    }
                }
                if (isDebugType()) {
                    itemLongClick = {
                        val list =
                            engraveConfigProvider.getEngraveMaterialList(this@BaseEngraveLayoutHelper)

                        for (layerInfo in LayerHelper.engraveLayerList) {
                            val last = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
                                apply(EngraveConfigEntity_.layerId.equal(layerInfo.layerId))
                            }
                            val list = EngraveConfigEntity::class.findAll(LPBox.PACKAGE_NAME) {
                                apply(EngraveConfigEntity_.layerId.equal(layerInfo.layerId))
                            }
                            L.i(last)
                        }
                        false
                    }
                }
            }
        }
    }

    /**改变界面到传输中*/
    open fun changeToTransmitting(transferConfigEntity: TransferConfigEntity) {
        val delegate = engraveCanvasFragment?.renderDelegate

        engraveBackFlow = if (_isSingleItemFlow) {
            ENGRAVE_FLOW_PREVIEW
        } else {
            ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
        }
        engraveFlow = ENGRAVE_FLOW_TRANSMITTING

        if (delegate == null) {
            //不是画布上的数据, 可能是恢复的数据
        } else {
            engraveModel.clearEngrave()
            //每次发送数据之前, 都生成一个新的任务
            val flowId = generateFlowId("准备发送文件")
            transferConfigEntity.taskId = flowId

            //数据雕刻方向
            transferConfigEntity.dataDir = laserPeckerModel.dataDir()
            if (transferConfigEntity.layerJson.isNullOrBlank()) {
                transferConfigEntity.layerJson = HawkEngraveKeys.lastDpiLayerJson
            }
            HawkEngraveKeys.lastDpiLayerJson = transferConfigEntity.layerJson
            transferConfigEntity.lpSaveEntity()

            engraveConfigProvider.onSaveTransferConfig(
                this@BaseEngraveLayoutHelper,
                transferConfigEntity
            )
            onStartEngraveTransferData(flowId)
            LPTransferHelper.startCreateTransferData(
                transferModel,
                flowId,
                delegate
            )
        }

        //last
        renderFlowItems()
    }

    //endregion ---数据配置---

    //region ---数据传输中---

    /**开始传输下一个*/
    open fun startTransferNext() {
        engraveFlow = ENGRAVE_FLOW_TRANSMITTING
        transferModel.startTransferNextData(flowTaskId)
        //last
        renderFlowItems()
    }

    /**开始雕刻下一个,
     * 2023-4-20 雕刻下一个文件时, 需要进入空闲状态*/
    open fun startEngraveNext() {
        deviceStateModel.pauseLoopCheckState(true, "雕刻下一个文件")
        ExitCmd().enqueue { bean, error ->
            if (error != null) {
                toastQQOrMessage(error.message)
                transferModel.errorTransfer(TransferException(error))
            } else {
                //
                engraveFlow = ENGRAVE_FLOW_ENGRAVING
                engraveModel.startEngraveNext(flowTaskId)
                renderFlowItems()
            }
        }
    }

    /**开始自动传输数据*/
    fun renderAutoTransfer() {
        //last
        engraveFlow = ENGRAVE_FLOW_TRANSMITTING
        renderFlowItems()
        ExitCmd().enqueue { bean, error ->
            if (error != null) {
                toastQQOrMessage(error.message)
                transferModel.errorTransfer(TransferException(error))
            } else {
                val flowId = flowTaskId
                onStartEngraveTransferData(flowId)
                transferModel.startTransferData(flowId)
            }
        }
    }

    /**渲染传输中的界面
     * 通过传输状态改变实时刷新界面
     * [com.angcyo.engrave.model.TransferModel.transferStateOnceData]
     * [com.angcyo.engrave.EngraveFlowLayoutHelper.bindDeviceState]
     * */
    open fun renderTransmitting() {
        updateIViewTitle(_string(R.string.transmitting))
        showCloseView(false)

        val taskStateData = transferModel.transferStateOnceData.value
        if (taskStateData?.error == null && taskStateData?.state == TransferState.TRANSFER_STATE_FINISH) {
            //文件传输完成
            if (engraveModel._engraveTaskEntity?.state == EngraveModel.ENGRAVE_STATE_INDEX_FINISH) {
                startEngraveNext()
            } else {
                engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                renderFlowItems()
            }
        } else {
            renderDslAdapter {
                val monitorEntity = EngraveFlowDataHelper.getTransferMonitor(flowTaskId)
                DataTransmittingItem()() {
                    itemProgress = taskStateData?.progress ?: 0
                    val remainingDuration = monitorEntity?.timeRemainingDuration()
                    itemRemainingTime = if (remainingDuration.isNullOrEmpty()) {
                        null
                    } else {
                        span {
                            append(_string(R.string.remaining_time))
                            append(":")
                            append(remainingDuration)
                        }
                    }
                }
                if (isDebug()) {
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
                                monitorEntity.dataName?.let {
                                    appendln()
                                    append("正在传输:$it/${monitorEntity.dataIndex ?: -1}")
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
                                        append(" 速率:${monitorEntity.speedString()} ${monitorEntity.averageSpeedString()} ${monitorEntity.maxSpeedString()}")
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
                        transferModel.stopTransfer()

                        //强制退出
                        engraveCanvasFragment?.fragment?.tgStrokeLoadingCaller { isCancel, loadEnd ->
                            ExitCmd().enqueue { bean, error ->
                                loadEnd(bean, error)
                                if (error != null) {
                                    toastQQOrMessage(error.message)
                                } else {
                                    engraveFlow = if (_isSingleItemFlow) {
                                        ENGRAVE_FLOW_PREVIEW
                                    } else {
                                        ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                                    }
                                    renderFlowItems()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //endregion ---数据传输中---

    //region ---雕刻参数配置---

    /**单元素雕刻参数配置界面, 只能配置参数, 无法next*/
    open fun renderEngraveItemParamsConfig() {
        val elementItemBean = _engraveItemRenderer?.lpElementBean()
        val fileName = elementItemBean?.name
        updateIViewTitle(span {
            if (!fileName.isNullOrBlank()) {
                append(fileName)
                appendLine()
                append(_string(R.string.print_setting)) {
                    fontSize = _titleFontSize * dpi
                }
            } else {
                append(_string(R.string.print_setting))
            }
        })

        showCloseView(false)
        cancelable = true
        viewHolder?.post {
            showRightView()
        }

        val engraveConfigEntity: EngraveConfigEntity? = null

        //图层id
        val layerId = elementItemBean?._layerId ?: LaserPeckerHelper.LAYER_LINE

        elementItemBean?.initEngraveParamsIfNeed()
        var dpi = elementItemBean?.dpi ?: LaserPeckerHelper.DPI_254

        renderDslAdapter {
            //单元素雕刻分辨率
            if (_isSingleItemFlow) {
                if (LayerHelper.getEngraveLayerInfo(layerId)?.showDpiConfig == true) {
                    TransferDataPxItem()() {
                        itemPxList = LaserPeckerHelper.findProductLayerSupportPxList(layerId)
                        selectorCurrentDpi(dpi)
                        itemHidden = itemPxList.isNullOrEmpty() //自动隐藏
                        observeItemChange {
                            //保存最后一次选择的dpi
                            elementItemBean?.clearIndex("数据dpi改变", true)  //清空数据索引
                            dpi =
                                itemPxList?.get(itemCurrentIndex)?.dpi ?: LaserPeckerHelper.DPI_254
                            elementItemBean?.dpi = dpi
                            HawkEngraveKeys.updateLayerDpi(layerId, dpi)

                            //更新材质列表
                            renderFlowItems()
                        }
                        observeEngraveParamsChange()
                    }
                } else {
                    val list = LaserPeckerHelper.findProductLayerSupportPxList(layerId)
                    dpi = if (list.size() >= 1) {
                        layerId.filterLayerDpi(list.first().dpi)
                    } else {
                        layerId.filterLayerDpi(LaserPeckerHelper.DPI_254)
                    }
                    if (elementItemBean?.dpi != dpi) {
                        elementItemBean?.clearIndex("DPI不一致", true)
                        elementItemBean?.dpi = dpi
                        elementItemBean?.initEngraveParamsIfNeed()
                        onEngraveParamsChangeAction()//单文件雕刻参数改变
                    }
                }
            }

            //材质选择
            EngraveMaterialWheelItem()() {
                itemTag = MaterialEntity::name.name
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = MaterialHelper.getLayerMaterialList(layerId, dpi)
                itemSelectedIndex = MaterialHelper.indexOfMaterial(
                    itemWheelList as List<MaterialEntity>,
                    elementItemBean?.materialCode,
                    elementItemBean?.materialKey,
                    elementItemBean?.printType,
                )
                itemEngraveItemBean = elementItemBean
                itemEngraveConfigEntity = engraveConfigEntity

                //刷新界面
                observeItemChange {
                    engraveConfigProvider.onSaveEngraveElementConfig(
                        this@BaseEngraveLayoutHelper,
                        elementItemBean,
                        engraveConfigEntity
                    )
                    renderFlowItems()
                }
                observeEngraveParamsChange()
            }

            // 激光光源选择
            val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
            if (laserPeckerModel.productInfoData.value?.isCSeries() != true && typeList.isNotEmpty()) {
                EngraveLaserSegmentItem()() {
                    itemCurrentIndex =
                        typeList.indexOfFirst { it.type == elementItemBean?.printType?.toByte() }
                    observeItemChange {
                        val type = currentLaserTypeInfo().type
                        elementItemBean?.printType = type.toInt()
                        HawkEngraveKeys.lastType = type.toInt()
                        engraveConfigEntity?.type = type
                        engraveConfigEntity.lpSaveEntity()

                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@BaseEngraveLayoutHelper,
                            elementItemBean,
                            engraveConfigEntity
                        )
                        renderFlowItems()
                    }
                    observeEngraveParamsChange()
                }
            }

            if (laserPeckerModel.isCSeries()) {
                //C1 加速级别选择
                EngraveOptionWheelItem()() {
                    itemTag = EngraveConfigEntity::precision.name
                    itemLabelText = _string(R.string.engrave_precision)
                    itemWheelList = EngraveHelper.percentList(5)
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        elementItemBean?.printPrecision
                    )
                    itemEngraveConfigEntity = engraveConfigEntity
                    itemEngraveItemBean = elementItemBean

                    observeItemChange {
                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@BaseEngraveLayoutHelper,
                            elementItemBean,
                            engraveConfigEntity
                        )
                    }

                    observeEngraveParamsChange()
                }
            }

            //风速等级/气泵
            if (_showPumpConfig) {
                val pumpList = _deviceConfigBean?.pumpMap?.get(elementItemBean?._layerId)
                if (!pumpList.isNullOrEmpty()) {
                    EngravePumpItem()() {
                        itemEngraveConfigEntity = engraveConfigEntity
                        itemEngraveItemBean = elementItemBean
                        initPumpIfNeed()
                        itemSegmentList = pumpList
                        itemUpdateAction(EngravePumpItem.PAYLOAD_UPDATE_PUMP)
                    }
                }
            }

            //雕刻参数
            if (deviceStateModel.isPenMode()) {
                //雕刻速度, 非雕刻深度
                EngraveOptionWheelItem()() {
                    itemTag = MaterialEntity.SPEED
                    itemLabelText = _string(R.string.engrave_speed)
                    itemWheelList = EngraveHelper.percentList()
                    itemEngraveItemBean = elementItemBean
                    itemEngraveConfigEntity = engraveConfigEntity
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        EngraveCmd.depthToSpeed(
                            elementItemBean?.printDepth ?: HawkEngraveKeys.lastDepth
                        )
                    )

                    observeItemChange {
                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@BaseEngraveLayoutHelper,
                            elementItemBean,
                            engraveConfigEntity
                        )
                    }

                    observeEngraveParamsChange()
                }
            } else {
                //功率/深度/次数
                EngravePropertyItem()() {
                    itemEngraveItemBean = elementItemBean
                    itemEngraveConfigEntity = engraveConfigEntity

                    observeItemChange {
                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@BaseEngraveLayoutHelper,
                            elementItemBean,
                            engraveConfigEntity
                        )
                    }

                    observeEngraveParamsChange()
                }
            }
        }
    }

    /**渲染雕刻配置界面
     *
     * 数据导出配置界面
     * [com.angcyo.canvas2.laser.pecker.dialog.ExportDataDialogConfig.onSelfRenderAdapter]
     * */
    open fun renderEngraveConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val taskId = flowTaskId ?: "null"

        //获取任务雕刻数据对应的图层列表
        val layerList = EngraveFlowDataHelper.getEngraveLayerList(taskId)
        val findLayer = layerList.find { it.layerId == selectLayerId }
        if (findLayer == null) {
            //选中的图层不存在, 则使用第一个
            selectLayerId = layerList.firstOrNull()?.layerId ?: selectLayerId
        }

        //图层添加选中后的图标
        selectLayerList.add(selectLayerId)
        val layerIconList = layerList.map {
            EngraveLayerInfo(
                it.layerId,
                span {
                    if (selectLayerList.contains(it.layerId)) {
                        appendDrawable(R.drawable.canvas_layer_selected)
                    }
                    append(it.label)
                },
                it.isGroupExtend,
                it.showDpiConfig
            )
        }

        val materialEntity = engraveConfigProvider.getEngraveMaterial(this, selectLayerId)
        "任务:${taskId} [$selectLayerId]已选材质:$materialEntity".writeToLog(logLevel = L.INFO)

        //当前选中图层的雕刻配置
        var engraveConfigEntity: EngraveConfigEntity? =
            engraveConfigProvider.getEngraveConfigList(this).find {
                it.layerId == selectLayerId
            }

        if (engraveConfigEntity == null) {
            //如果没有材质中没有找到对应图层的配置, 则构建一个
            engraveConfigEntity = engraveConfigProvider.getEngraveConfig(this, selectLayerId)
        }

        /*"任务:${taskId} 图层[$selectLayerId] 材质:${materialEntity} 参数:${engraveConfigEntity}".writeToLog(
            logLevel = L.INFO
        )*/

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_tip)
            }
            if (!laserPeckerModel.isCSeries()) {
                //非C1显示, 设备水平角度
                renderDeviceInfoIfNeed()
            }
            if (deviceStateModel.needShowExDeviceTipItem()) {
                PreviewExDeviceTipItem()() {
                    itemEngraveConfigEntity = engraveConfigEntity
                }
            }

            //雕刻相关的参数
            if (_isSingleItemFlow) {
                //参数配置提示
                PreviewTipItem()() {
                    itemTip = _string(R.string.engrave_item_params_tip)
                    itemTipTextColor = _color(R.color.error)
                }
            } else {
                //雕刻图层切换
                if (layerIconList.isNotEmpty() && !_isSingleFlow) {
                    EngraveLayerConfigItem()() {
                        itemSegmentList = layerIconList
                        itemCurrentIndex = max(
                            0,
                            layerIconList.indexOf(layerIconList.find { it.layerId == selectLayerId })
                        )
                        observeItemChange {
                            selectLayerId = layerIconList[itemCurrentIndex].layerId
                            val itemIndexPosition = it.itemIndexPosition()
                            if (itemIndexPosition != -1) {
                                //图层改变后, 动画提示参数变化
                                RecyclerItemFlowAnimator(
                                    itemIndexPosition + 1,
                                    -2
                                ).start(it.itemDslAdapter?._recyclerView)
                            }
                            renderFlowItems()
                        }
                    }
                }

                if (!deviceStateModel.isPenMode()) { //握笔模块, 不需要材质
                    //材质选择
                    EngraveMaterialWheelItem()() {
                        itemTag = MaterialEntity::name.name
                        itemLabelText = _string(R.string.custom_material)
                        itemWheelList = MaterialHelper.getLayerMaterialList(selectLayerId)
                        itemSelectedIndex = MaterialHelper.indexOfMaterial(
                            itemWheelList as List<MaterialEntity>,
                            materialEntity
                        )
                        itemEngraveConfigEntity = engraveConfigEntity

                        if (!_isSingleFlow) {
                            itemSaveAction = {
                                showSaveMaterialDialog(taskId, materialEntity) {
                                    //刷新界面, 使用自定义的材质信息
                                    renderFlowItems()
                                }
                            }

                            itemDeleteAction = { key ->
                                showDeleteMaterialDialog(taskId, key) {
                                    renderFlowItems()
                                }
                            }
                        }

                        //刷新界面
                        observeItemChange {
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@BaseEngraveLayoutHelper,
                                engraveConfigEntity
                            )
                            renderFlowItems()
                        }
                    }
                }

                // 激光光源选择
                val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
                if (laserPeckerModel.productInfoData.value?.isCSeries() != true && typeList.isNotEmpty()) {
                    EngraveSegmentScrollItem()() {
                        itemText = _string(R.string.laser_type)
                        itemSegmentList = typeList
                        itemCurrentIndex =
                            typeList.indexOfFirst { it.type == engraveConfigEntity.type }
                        observeItemChange {
                            val type = typeList[itemCurrentIndex].type
                            HawkEngraveKeys.lastType = type.toInt()
                            engraveConfigEntity.type = type
                            engraveConfigEntity.lpSaveEntity()
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@BaseEngraveLayoutHelper,
                                engraveConfigEntity
                            )
                            renderFlowItems()
                        }
                        observeMaterialChange()
                    }
                }

                if (laserPeckerModel.isCSeries()) {
                    //C1 加速级别选择 加速级别
                    if (engraveConfigEntity.precision < 0) {
                        engraveConfigEntity.precision = 1
                        engraveConfigEntity.lpSaveEntity()
                    }
                    EngraveOptionWheelItem()() {
                        itemTag = EngraveConfigEntity::precision.name
                        itemLabelText = _string(R.string.engrave_precision)
                        itemWheelList = EngraveHelper.percentList(5)
                        itemSelectedIndex = EngraveHelper.findOptionIndex(
                            itemWheelList,
                            engraveConfigEntity.precision
                        )
                        itemEngraveConfigEntity = engraveConfigEntity

                        observeItemChange {
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@BaseEngraveLayoutHelper,
                                engraveConfigEntity
                            )
                        }

                        observeMaterialChange()
                    }
                }

                //风速等级/气泵
                if (_showPumpConfig) {
                    val pumpList = _deviceConfigBean?.pumpMap?.get(engraveConfigEntity.layerId)
                    if (!pumpList.isNullOrEmpty()) {
                        EngravePumpItem()() {
                            itemEngraveConfigEntity = engraveConfigEntity
                            initPumpIfNeed()
                            itemSegmentList = pumpList
                            itemUpdateAction(EngravePumpItem.PAYLOAD_UPDATE_PUMP)
                        }
                    }
                }

                //雕刻参数
                if (deviceStateModel.isPenMode()) {
                    //握笔模块, 雕刻速度, 非雕刻深度
                    engraveConfigEntity.power = 100 //功率必须100%
                    engraveConfigEntity.lpSaveEntity()
                    engraveConfigProvider.onSaveEngraveConfig(
                        this@BaseEngraveLayoutHelper,
                        engraveConfigEntity
                    )
                    //雕刻速度
                    EngraveOptionWheelItem()() {
                        itemTag = MaterialEntity.SPEED
                        itemLabelText = _string(R.string.engrave_speed)
                        itemWheelList = EngraveHelper.percentList()
                        itemEngraveConfigEntity = engraveConfigEntity
                        itemSelectedIndex = EngraveHelper.findOptionIndex(
                            itemWheelList,
                            EngraveCmd.depthToSpeed(engraveConfigEntity.depth)
                        )
                        observeItemChange {
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@BaseEngraveLayoutHelper,
                                engraveConfigEntity
                            )
                        }
                        observeMaterialChange()
                    }
                } else {
                    //功率/深度/次数
                    EngravePropertyItem()() {
                        itemEngraveConfigEntity = engraveConfigEntity
                        observeItemChange {
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@BaseEngraveLayoutHelper,
                                engraveConfigEntity
                            )
                        }
                        observeMaterialChange()
                    }
                }
            }

            EngraveConfirmItem()() {
                itemClick = {
                    //开始雕刻
                    checkEngraveNotify {
                        checkExDevice {
                            showFocalDistance(it.context) {
                                showSafetyTips(it.context) {
                                    asyncTimeoutExitCmd { bean, error ->
                                        if (error == null) {
                                            //开始雕刻
                                            engraveConfigProvider.onStartEngrave(this@BaseEngraveLayoutHelper)
                                            onStartEngrave(taskId)
                                            val taskEntity =
                                                if (_isSingleFlow) engraveModel.startEngrave(
                                                    taskId,
                                                    selectLayerId,
                                                    singleFlowInfo!!.fileName,
                                                    singleFlowInfo!!.mount
                                                ) else engraveModel.startEngrave(taskId)
                                            if (taskEntity.dataList.isNullOrEmpty()) {
                                                toastQQOrMessage(_string(R.string.no_data_engrave))
                                            } else {
                                                engraveFlow = ENGRAVE_FLOW_ENGRAVING
                                                renderFlowItems()
                                            }
                                        } else {
                                            toastQQOrMessage(error.message)
                                        }
                                    }
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
        if (!_isSingleFlow) {
            observeItemChange {
                itemDslAdapter?.find<EngraveMaterialWheelItem>()?.let {
                    it._materialEntity?.isChanged = true
                    it.updateAdapterItem()
                }
            }
        }
    }

    /**单文件雕刻参数改变回调*/
    var onEngraveParamsChangeAction: () -> Unit = {}

    /**监听改变之后, 单文件雕刻参数*/
    fun DslAdapterItem.observeEngraveParamsChange() {
        observeItemChange {
            onEngraveParamsChangeAction()
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
                materialEntity.isChanged = false
                action.invoke()
                false
            }
        }
    }

    /**显示删除自定义材质的对话框*/
    fun showDeleteMaterialDialog(taskId: String?, materialKey: String, action: Action) {
        engraveCanvasFragment?.fragment?.fContext()?.messageDialog {
            dialogTitle = _string(R.string.engrave_warn)
            dialogMessage = _string(R.string.delete_material_tip)
            needPositiveButton { dialog, dialogViewHolder ->
                dialog.dismiss()
                EngraveFlowDataHelper.deleteMaterial(taskId, materialKey)
                action()
            }
        }
    }

    //endregion ---雕刻参数配置---

    //region ---雕刻中---

    /**渲染雕刻中的界面
     * 通过设备状态改变实时刷新界面
     * [com.angcyo.engrave2.model.EngraveModel.engraveStateData]
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel.deviceStateData]
     *
     * [com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper.bindDeviceState]
     * */
    open fun renderEngraving() {
        val taskId = flowTaskId
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(taskId)
        val fileName = singleFlowInfo?.fileName ?: transferConfigEntity?.name
        updateIViewTitle(span {
            if (!fileName.isNullOrBlank()) {
                append(fileName)
                appendLine()
                if (isDebug()) {
                    val engraveDataEntity =
                        EngraveFlowDataHelper.getCurrentEngraveDataEntity(taskId)
                    engraveDataEntity?.index?.let {
                        append("$it/") {
                            fontSize = _titleFontSize * dpi
                        }
                    }
                }
                append(_string(R.string.engraving)) {
                    fontSize = _titleFontSize * dpi
                }
            } else {
                append(_string(R.string.engraving))
            }
        })

        if (HawkEngraveKeys.enableBackEngrave) {
            engraveBackFlow = 0
            if (_isHistoryFlow) {
                showCloseView(true, _string(R.string.ui_minimum))
            } else {
                showCloseView(true, _string(R.string.back_creation))
            }
        } else {
            engraveBackFlow = ENGRAVE_FLOW_BEFORE_CONFIG
            showCloseView(false)
        }

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_move_state_tips)
            }
            if (!laserPeckerModel.isCSeries()) {
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
            //---雕刻控制-暂停-结束
            EngravingControlItem()() {
                itemTaskId = flowTaskId
                itemPauseAction = { isPause ->
                    if (isPause) {
                        if (!checkUnsafe()) {
                            engraveModel.continueEngrave()
                        }
                    } else {
                        engraveModel.pauseEngrave()
                    }
                }
                itemStopAction = {
                    //停止雕刻, 直接完成
                    engraveCanvasFragment?.fragment?.engraveLoadingAsyncTimeout({
                        syncSingle { countDownLatch ->
                            engraveModel.stopEngrave("来自点击按钮", countDownLatch)
                        }
                    })
                }
                itemShowSkipButton = HawkEngraveKeys.enableSingleItemTransfer
                itemSkipAction = {
                    //强制退出
                    engraveCanvasFragment?.fragment?.tgStrokeLoadingCaller { isCancel, loadEnd ->
                        ExitCmd(timeout = HawkEngraveKeys.receiveTimeoutMax).enqueue { bean, error ->
                            loadEnd(bean, error)
                            if (error != null) {
                                toastQQOrMessage(error.message)
                            } else {
                                engraveModel.finishCurrentIndexEngrave()
                            }
                        }
                    }
                }
            }
        }
    }

    //endregion ---雕刻中---

    //region ---雕刻完成---

    /**渲染雕刻完成的界面*/
    open fun renderEngraveFinish() {
        val taskId = flowTaskId
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(taskId)
        val fileName = singleFlowInfo?.fileName ?: transferConfigEntity?.name
        updateIViewTitle(span {
            if (!fileName.isNullOrBlank()) {
                append(fileName)
                appendLine()
                append(_string(R.string.engrave_finish)) {
                    fontSize = _titleFontSize * dpi
                }
            } else {
                append(_string(R.string.engrave_finish))
            }
        })
        engraveBackFlow = 0
        if (isInPadMode()) {
            showCloseView(true, _string(R.string.ui_quit))
        } else {
            showCloseView(true, _string(R.string.back_creation))
        }

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
                    itemLayerId = engraveLayerInfo.layerId
                }
            }
            //
            EngraveFinishControlItem()() {
                itemShowShareButton =
                    !_isSingleFlow && !_isHistoryFlow && _deviceSettingBean?.showProjectShare == true
                //雕刻完成之后, 如果有变量文本, 则显示继续雕刻按钮
                itemShowContinueButton = !HawkEngraveKeys.enableItemEngraveParams &&
                        engraveCanvasFragment?.renderDelegate?.getSelectorSingleElementRendererList(
                            true,
                            false
                        ).haveVariableElement() == true
                itemShowAgainButton = !itemShowContinueButton //显示持续雕刻的情况下, 不显示再次雕刻按钮
                itemShareAction = {
                    val delegate = engraveCanvasFragment?.renderDelegate
                    LPProjectManager().saveProjectV2Share(delegate, taskId)
                }
                itemAgainAction = {
                    //再次雕刻, 回退到参数配置页面
                    EngraveFlowDataHelper.againEngrave(taskId) //清除缓存状态数据
                    engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                    renderFlowItems()
                }
                itemContinueAction = {
                    //继续雕刻, 重新传输数据, 并且自动发送数据
                    UMEvent.CONTINUE_ENGRAVE.umengEventValue()
                    continueTransferEngrave()
                }
            }
        }
    }

    /**重建任务,使用上一次的传输配置,进行二次传输并雕刻
     * [changeToTransmitting]*/
    open fun continueTransferEngrave() {
        //检查数据是否超出了范围
        if (!checkCanNext()) {
            return
        }

        val oldTaskId = flowTaskId
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(oldTaskId)

        //获取旧的任务传输配置
        if (transferConfigEntity == null) {
            toastQQOrMessage(_string(R.string.not_support))
            return
        }
        if (transferConfigEntity.taskId == oldTaskId) {
            val newTaskId = uuid()
            transferConfigEntity.entityId = 0
            transferConfigEntity.taskId = newTaskId
            transferConfigEntity.lpSaveEntity()
        }

        //退出打印模式, 进入空闲模式
        asyncTimeoutExitCmd { bean, error ->
            if (error == null) {
                flowTaskId = transferConfigEntity.taskId
                changeToTransmitting(transferConfigEntity)
            } else {
                toastQQOrMessage(error.message)
            }
        }
    }

    //endregion ---雕刻完成---

}