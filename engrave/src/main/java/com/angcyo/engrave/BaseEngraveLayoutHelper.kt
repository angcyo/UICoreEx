package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.core.vmApp
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.engrave.dslitem.transfer.TransferDataNameItem
import com.angcyo.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.TransferModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.form.checkItemThrowable
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.ex._string
import com.angcyo.library.toast
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.viewmodel.observe

/**
 * 雕刻item布局渲染
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
abstract class BaseEngraveLayoutHelper : BaseEngravePreviewLayoutHelper() {

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
        transferModel.transferStateData.observe(this, allowBackward = false) {
            it?.apply {
                //数据传输进度通知
                if (it.isFinish) {
                    //默认选中第1个雕刻图层
                    selectLayerMode =
                        EngraveFlowDataHelper.getEngraveLayerList(taskId).firstOrNull()?.mode ?: 0
                }
                if (engraveFlow == ENGRAVE_FLOW_TRANSMITTING) {
                    renderFlowItems()
                }
            }
        }
        //
        engraveModel.engraveStateData.observe(this, allowBackward = false) {
            it?.apply {
                if (taskId == flowTaskId && engraveFlow == ENGRAVE_FLOW_ENGRAVING) {
                    if (it.state == EngraveModel.ENGRAVE_STATE_FINISH) {
                        //雕刻完成
                        engraveFlow = ENGRAVE_FLOW_FINISH
                    }
                    renderFlowItems()
                }
            }
        }
    }

    /**生成百分比数值列表*/
    fun percentList(max: Int = 100): List<Int> {
        return (1..max).toList()
    }

    //

    /**当前选中的图层模式*/
    var selectLayerMode: Int = 0

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        super.onEngraveFlowChanged(from, to)
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {

        } else if (to == ENGRAVE_FLOW_BEFORE_CONFIG) {

        }
    }

    //

    /**渲染传输数据配置界面*/
    fun renderTransferConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val transferConfigEntity = EngraveFlowDataHelper.generateTransferConfig(flowTaskId)

        renderDslAdapter {
            TransferDataNameItem()() {
                itemTransferConfigEntity = transferConfigEntity
            }
            TransferDataPxItem()() {
                itemPxList = LaserPeckerHelper.findProductSupportPxList()
                itemTransferConfigEntity = transferConfigEntity
            }
            EngraveDividerItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    if (!checkItemThrowable()) {
                        //下一步, 数据传输界面

                        transferConfigEntity.lpSaveEntity()

                        //让设备进入空闲模式
                        ExitCmd().enqueue()
                        syncQueryDeviceState()

                        engraveBackFlow = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
                        engraveFlow = ENGRAVE_FLOW_TRANSMITTING
                        renderFlowItems()

                        val canvasDelegate = engraveCanvasFragment?.canvasDelegate
                        if (canvasDelegate == null) {
                            //不是画布上的数据, 可能是恢复的数据
                        } else {
                            transferModel.startCreateTransferData(flowTaskId, canvasDelegate)
                        }
                    }
                }
            }
        }
    }

    /**渲染传输中的界面*/
    fun renderTransmitting() {
        updateIViewTitle(_string(R.string.transmitting))
        showCloseView(false)

        val taskStateData = transferModel.transferStateData.value
        if (taskStateData?.isFinish == true) {
            //文件传输完成
            engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
            renderFlowItems()
            //退出打印模式, 进入空闲模式
            ExitCmd().enqueue()
        } else {
            renderDslAdapter {
                DataTransmittingItem()() {
                    itemProgress = taskStateData?.progress ?: 0
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

    //

    /**渲染雕刻配置界面*/
    fun renderEngraveConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        val taskId = flowTaskId

        //雕刻配置信息
        val engraveConfigEntity =
            EngraveFlowDataHelper.generateEngraveConfig(taskId, selectLayerMode)

        //材质列表
        val materialList = EngraveHelper.getProductMaterialList()

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_tip)
            }
            //材质选择
            EngraveOptionWheelItem()() {
                itemTag = MaterialEntity::name.name
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = materialList
                itemSelectedIndex = 0
                itemEngraveConfigEntity = engraveConfigEntity
            }

            if (laserPeckerModel.productInfoData.value?.isLIV() == true) {
                //L4 激光光源选择
                EngraveSegmentScrollItem()() {
                    val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
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
            if (laserPeckerModel.productInfoData.value?.isCI() == true) {
                //C1 加速级别选择
                EngraveOptionWheelItem()() {
                    itemTag = EngraveConfigEntity::precision.name
                    itemLabelText = "加速级别"
                    itemWheelList = percentList(5)
                    itemSelectedIndex =
                        EngraveHelper.findOptionIndex(itemWheelList, engraveConfigEntity.precision)
                    itemEngraveConfigEntity = engraveConfigEntity
                }
            }

            //雕刻图层切换
            EngraveLayerConfigItem()() {
                val layerList = EngraveFlowDataHelper.getEngraveLayerList(taskId)
                itemSegmentList = layerList
                itemCurrentIndex = selectLayerMode
                observeItemChange {
                    selectLayerMode = layerList[itemCurrentIndex].mode
                    renderFlowItems()
                }
            }
            //功率/深度
            EngraveOptionWheelItem()() {
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
            }
            EngraveConfirmItem()() {
                itemClick = {
                    //开始雕刻
                    /*engraveOptionInfo?.let { option ->
                        if (showDiameter && option.diameterPixel <= 0) {
                            toast("diameter need > 0")
                        } else {
                            engraveReadyInfo?.let { readyDataInfo ->
                                //start check
                                checkStartEngrave(readyDataInfo.engraveData!!.index!!, option)
                            }
                        }
                    }*/
                    engraveFlow = ENGRAVE_FLOW_ENGRAVING
                    renderFlowItems()

                    //开始雕刻
                    engraveModel.startEngrave(taskId)
                }
            }
        }
    }

    /**渲染雕刻中的界面*/
    fun renderEngraving() {
        updateIViewTitle(_string(R.string.engraving))
        engraveBackFlow = ENGRAVE_FLOW_BEFORE_CONFIG
        showCloseView(false)

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_move_state_tips)
            }
            EngraveProgressItem()() {
                itemTaskId = flowTaskId
            }
            EngravingInfoItem()() {
                itemTaskId = flowTaskId
            }
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

    /**渲染雕刻完成的界面*/
    fun renderEngraveFinish() {
        updateIViewTitle(_string(R.string.engrave_finish))
        engraveBackFlow = 0
        showCloseView(true, _string(R.string.back_creation))

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
                    itemLayerMode = engraveLayerInfo.mode
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

}