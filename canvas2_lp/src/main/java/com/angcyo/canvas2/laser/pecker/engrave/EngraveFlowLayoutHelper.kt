package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveDividerItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.*
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
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.RecyclerItemFlowAnimator
import com.angcyo.dsladapter.find
import com.angcyo.dsladapter.itemIndexPosition
import com.angcyo.engrave2.*
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.model.EngraveModel
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.form.checkItemThrowable
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.*
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.laserpacker.device.exception.TransferException
import com.angcyo.library.L
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex.*
import com.angcyo.library.toastQQ
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
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
                        toastQQ(engraveCmdError.message)
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
    fun renderTransferConfig() {
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
                                engraveBackFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
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
                                    HawkEngraveKeys.lastDpiLayerJson =
                                        transferConfigEntity.layerJson
                                            ?: HawkEngraveKeys.lastDpiLayerJson
                                    transferConfigEntity.lpSaveEntity()

                                    engraveConfigProvider.onSaveTransferConfig(
                                        this@EngraveFlowLayoutHelper,
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
                            } else {
                                toastQQ(error.message)
                            }
                        }
                    }
                }
                if (isDebugType()) {
                    itemLongClick = {
                        val list =
                            engraveConfigProvider.getEngraveMaterialList(this@EngraveFlowLayoutHelper)

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

    //endregion ---数据配置---

    //region ---数据传输中---

    /**开始传输下一个*/
    fun startTransferNext() {
        engraveFlow = ENGRAVE_FLOW_TRANSMITTING
        transferModel.startTransferNextData(flowTaskId)
        //last
        renderFlowItems()
    }

    /**开始雕刻下一个,
     * 2023-4-20 雕刻下一个文件时, 需要进入空闲状态*/
    fun startEngraveNext() {
        deviceStateModel.pauseLoopCheckState(true, "雕刻下一个文件")
        ExitCmd().enqueue { bean, error ->
            if (error != null) {
                toastQQ(error.message)
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
                toastQQ(error.message)
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
    fun renderTransmitting() {
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
                        "${_string(R.string.remaining_time)}:${remainingDuration}"
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
                                    toastQQ(error.message)
                                } else {
                                    engraveFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
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
    fun renderEngraveItemParamsConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        showCloseView(false)
        cancelable = true

        var engraveConfigEntity: EngraveConfigEntity? = null
        val projectItemBean = _engraveItemRenderer?.lpElementBean()
        projectItemBean?.apply {
            //雕刻配置
            engraveConfigEntity =
                engraveConfigProvider.getEngraveElementConfig(this@EngraveFlowLayoutHelper, this)
        }

        renderDslAdapter {
            //材质选择
            EngraveMaterialWheelItem()() {
                itemTag = MaterialEntity::name.name
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = MaterialHelper.unionMaterialList
                itemSelectedIndex = MaterialHelper.indexOfMaterial(
                    MaterialHelper.unionMaterialList,
                    projectItemBean?.materialKey,
                    projectItemBean?.materialKey,
                    projectItemBean?.printType,
                )
                itemEngraveItemBean = projectItemBean
                itemEngraveConfigEntity = engraveConfigEntity

                itemDeleteAction = { key ->
                    showDeleteMaterialDialog(flowTaskId, key) {
                        renderFlowItems()
                    }
                }

                //刷新界面
                observeItemChange {
                    engraveConfigProvider.onSaveEngraveElementConfig(
                        this@EngraveFlowLayoutHelper,
                        projectItemBean,
                        engraveConfigEntity
                    )
                    renderFlowItems()
                }
                observeEngraveParamsChange()
            }

            // 激光光源选择
            val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
            if (laserPeckerModel.productInfoData.value?.isCI() != true && typeList.isNotEmpty()) {
                EngraveLaserSegmentItem()() {
                    observeItemChange {
                        val type = currentLaserTypeInfo().type
                        projectItemBean?.printType = type.toInt()
                        HawkEngraveKeys.lastType = type.toInt()
                        engraveConfigEntity?.type = type
                        engraveConfigEntity.lpSaveEntity()

                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@EngraveFlowLayoutHelper,
                            projectItemBean,
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
                        projectItemBean?.printPrecision
                    )
                    itemEngraveConfigEntity = engraveConfigEntity
                    itemEngraveItemBean = projectItemBean

                    observeItemChange {
                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@EngraveFlowLayoutHelper,
                            projectItemBean,
                            engraveConfigEntity
                        )
                    }

                    observeEngraveParamsChange()
                }
            }

            //雕刻参数
            if (deviceStateModel.isPenMode()) {
                //雕刻速度, 非雕刻深度
                EngraveOptionWheelItem()() {
                    itemTag = MaterialEntity.SPEED
                    itemLabelText = _string(R.string.engrave_speed)
                    itemWheelList = EngraveHelper.percentList()
                    itemEngraveItemBean = projectItemBean
                    itemEngraveConfigEntity = engraveConfigEntity
                    itemSelectedIndex = EngraveHelper.findOptionIndex(
                        itemWheelList,
                        EngraveCmd.depthToSpeed(
                            projectItemBean?.printDepth ?: HawkEngraveKeys.lastDepth
                        )
                    )

                    observeItemChange {
                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@EngraveFlowLayoutHelper,
                            projectItemBean,
                            engraveConfigEntity
                        )
                    }

                    observeEngraveParamsChange()
                }
            } else {
                //功率/深度/次数
                EngravePropertyItem()() {
                    itemEngraveItemBean = projectItemBean
                    itemEngraveConfigEntity = engraveConfigEntity

                    observeItemChange {
                        engraveConfigProvider.onSaveEngraveElementConfig(
                            this@EngraveFlowLayoutHelper,
                            projectItemBean,
                            engraveConfigEntity
                        )
                    }

                    observeEngraveParamsChange()
                }
            }
        }
    }

    /**渲染雕刻配置界面*/
    fun renderEngraveConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val taskId = flowTaskId

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
            if (HawkEngraveKeys.enableItemEngraveParams) {
                //参数配置提示
                PreviewTipItem()() {
                    itemTip = _string(R.string.engrave_item_params_tip)
                    itemTipTextColor = _color(R.color.error)
                }
            } else {
                //雕刻图层切换
                if (layerIconList.isNotEmpty()) {
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

                if (!deviceStateModel.isPenMode()) {//握笔模块, 不需要材质
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

                        //刷新界面
                        observeItemChange {
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@EngraveFlowLayoutHelper,
                                engraveConfigEntity
                            )
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
                        itemCurrentIndex =
                            typeList.indexOfFirst { it.type == engraveConfigEntity.type }
                        observeItemChange {
                            val type = typeList[itemCurrentIndex].type
                            HawkEngraveKeys.lastType = type.toInt()
                            engraveConfigEntity.type = type
                            engraveConfigEntity.lpSaveEntity()
                            engraveConfigProvider.onSaveEngraveConfig(
                                this@EngraveFlowLayoutHelper,
                                engraveConfigEntity
                            )
                            renderFlowItems()
                        }
                        observeMaterialChange()
                    }
                }

                if (laserPeckerModel.isCSeries()) {
                    //C1 加速级别选择
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
                                this@EngraveFlowLayoutHelper,
                                engraveConfigEntity
                            )
                        }

                        observeMaterialChange()
                    }
                }

                //雕刻参数
                if (deviceStateModel.isPenMode()) {
                    //握笔模块, 雕刻速度, 非雕刻深度
                    engraveConfigEntity.power = 100 //功率必须100%
                    engraveConfigEntity.lpSaveEntity()
                    engraveConfigProvider.onSaveEngraveConfig(
                        this@EngraveFlowLayoutHelper,
                        engraveConfigEntity
                    )
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
                                this@EngraveFlowLayoutHelper,
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
                                this@EngraveFlowLayoutHelper,
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
                                            engraveConfigProvider.onStartEngrave(this@EngraveFlowLayoutHelper)
                                            onStartEngrave(taskId)
                                            val taskEntity = engraveModel.startEngrave(taskId)
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
                it._materialEntity?.isChanged = true
                it.updateAdapterItem()
            }
        }
    }

    /**回调*/
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
     * [com.angcyo.engrave.model.EngraveModel.engraveStateData]
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.deviceStateData]
     *
     * [com.angcyo.engrave.BaseFlowLayoutHelper.bindDeviceState]
     * */
    fun renderEngraving() {
        val taskId = flowTaskId
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(taskId)
        updateIViewTitle(span {
            if (!transferConfigEntity?.name.isNullOrBlank()) {
                append(transferConfigEntity?.name)
                appendLine()
                append(_string(R.string.engraving)) {
                    fontSize = _titleFontSize * dpi
                }
            } else {
                append(_string(R.string.engraving))
            }
        })

        if (HawkEngraveKeys.enableBackEngrave) {
            engraveBackFlow = 0
            if (this is HistoryEngraveFlowLayoutHelper) {
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
            if (HawkEngraveKeys.enableSingleItemTransfer) {
                //激活单文件雕刻的情况下, 允许跳过当前雕刻的索引
                DslBlackButtonItem()() {
                    itemButtonText = _string(R.string.engrave_skip_current)
                    itemClick = {
                        //强制退出
                        engraveCanvasFragment?.fragment?.tgStrokeLoadingCaller { isCancel, loadEnd ->
                            ExitCmd(timeout = HawkEngraveKeys.receiveTimeoutMax).enqueue { bean, error ->
                                loadEnd(bean, error)
                                if (error != null) {
                                    toastQQ(error.message)
                                } else {
                                    engraveModel.finishCurrentIndexEngrave()
                                }
                            }
                        }
                    }
                }
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
            }
        }
    }

    //endregion ---雕刻中---

    //region ---雕刻完成---

    /**渲染雕刻完成的界面*/
    open fun renderEngraveFinish() {
        val taskId = flowTaskId
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(taskId)
        updateIViewTitle(span {
            if (!transferConfigEntity?.name.isNullOrBlank()) {
                append(transferConfigEntity?.name)
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
            if (!HawkEngraveKeys.enableItemEngraveParams) {
                EngraveFlowDataHelper.getEngraveLayerList(taskId).forEach { engraveLayerInfo ->
                    EngraveLabelItem()() {
                        itemText = engraveLayerInfo.label
                    }

                    EngraveFinishInfoItem()() {
                        itemTaskId = taskId
                        itemLayerId = engraveLayerInfo.layerId
                    }
                }
            }
            //
            EngraveFinishControlItem()() {
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
            }
        }
    }

    //endregion ---雕刻完成---

}