package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.core.vmApp
import com.angcyo.engrave.data.EngraveConfigInfo
import com.angcyo.engrave.data.EngraveDataParam
import com.angcyo.engrave.data.TransferDataConfigInfo
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.engrave.dslitem.transfer.TransferDataNameItem
import com.angcyo.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.TransferModel
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.L
import com.angcyo.library.ex._string
import com.angcyo.library.toast
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

    /**雕刻状态*/
    var engraveState: EngraveModel.EngraveState? = null

    override fun bindDeviceState() {
        super.bindDeviceState()
        //
        transferModel.transferStateData.observe(this, allowBackward = false) {
            it?.apply {
                //数据传输进度通知
                if (it.isFinish) {
                    engraveConfigInfo = transferModel.taskEngraveConfigInfo
                    selectLayerMode = engraveConfigInfo?.getLayerList()?.firstOrNull()?.mode ?: 0
                }
                if (engraveFlow == ENGRAVE_FLOW_TRANSMITTING) {
                    renderFlowItems()
                }
            }
        }
        //
        engraveModel.engraveStateData.observe(this, allowBackward = false) {
            it?.apply {
                if (engraveFlow == ENGRAVE_FLOW_ENGRAVING) {
                    engraveState = it
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

    /**数据配置信息, 在进入页面之前初始化*/
    lateinit var transferDataConfigInfo: TransferDataConfigInfo

    /**雕刻参数配置信息, 进入对应页面前初始化*/
    var engraveConfigInfo: EngraveConfigInfo? = null

    /**当前选中的图层模式*/
    var selectLayerMode: Int = 0

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        super.onEngraveFlowChanged(from, to)
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            if (!::transferDataConfigInfo.isInitialized) {
                transferDataConfigInfo = TransferDataConfigInfo()
            }
        } else if (to == ENGRAVE_FLOW_BEFORE_CONFIG) {
            /*if (!::engraveConfigInfo.isInitialized) {
                engraveConfigInfo = EngraveConfigInfo()
            }*/
        }
    }

    //

    /**渲染传输数据配置界面*/
    fun renderTransferConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        renderDslAdapter {
            TransferDataNameItem()() {
                itemTransferDataConfigInfo = transferDataConfigInfo
            }
            TransferDataPxItem()() {
                itemPxList = LaserPeckerHelper.findProductSupportPxList()
                itemTransferDataConfigInfo = transferDataConfigInfo
            }
            EngraveDividerItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据传输界面
                    //让设备进入空闲模式
                    //进入空闲模式
                    ExitCmd().enqueue()
                    syncQueryDeviceState()

                    engraveFlow = ENGRAVE_FLOW_TRANSMITTING
                    renderFlowItems()

                    val canvasDelegate = engraveCanvasFragment?.canvasDelegate
                    if (canvasDelegate == null) {
                        //不是画布上的数据, 可能是恢复的数据
                    } else {
                        transferModel.startCreateData(transferDataConfigInfo, canvasDelegate)
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

        val engraveConfigInfo = engraveConfigInfo ?: return
        val engraveDataParam =
            engraveConfigInfo.getEngraveDataByLayerMode(selectLayerMode) ?: return

        //材质列表
        val materialList = EngraveHelper.getProductMaterialList()

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_tip)
            }
            //材质
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = materialList
                itemTag = EngraveDataParam::materialName.name
                itemSelectedIndex = 0
            }
            if (laserPeckerModel.productInfoData.value?.isLIV() == true) {
                EngraveSegmentScrollItem()() {
                    itemText = _string(R.string.laser_type)
                    itemSegmentList = LaserPeckerHelper.findProductSupportLaserTypeList()
                }
            }
            EngraveLayerConfigItem()() {
                val layerList = engraveConfigInfo.getLayerList()
                itemSegmentList = layerList
                itemCurrentIndex = selectLayerMode
                observeItemChange {
                    selectLayerMode = layerList[itemCurrentIndex].mode
                    renderFlowItems()
                }
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_power)
                itemWheelList = percentList()
                itemTag = EngraveDataParam::power.name
                itemEngraveDataParam = engraveDataParam
                itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveDataParam.power)
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_speed)
                itemWheelList = percentList()
                itemTag = EngraveDataParam::depth.name
                itemEngraveDataParam = engraveDataParam
                itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveDataParam.depth)
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.print_times)
                itemWheelList = percentList(255)
                itemTag = EngraveDataParam::time.name
                itemEngraveDataParam = engraveDataParam
                itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveDataParam.time)
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
                    L.i(engraveConfigInfo)
                    engraveFlow = ENGRAVE_FLOW_ENGRAVING
                    renderFlowItems()

                    //开始雕刻
                    engraveModel.startEngrave(engraveConfigInfo)
                }
            }
        }
    }

    /**渲染雕刻中的界面*/
    fun renderEngraving() {
        updateIViewTitle(_string(R.string.engraving))
        engraveBackFlow = ENGRAVE_FLOW_BEFORE_CONFIG
        showCloseView(false)

        val engraveState = engraveState ?: return

        renderDslAdapter {
            PreviewTipItem()() {
                itemTip = _string(R.string.engrave_move_state_tips)
            }
            EngraveProgressItem()() {
                itemEngraveState = engraveState
            }
            EngravingInfoItem()() {
                itemEngraveState = engraveState
            }
            EngravingControlItem()() {
                itemEngraveState = engraveState
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

    /**渲染雕刻中的界面*/
    fun renderEngraveFinish() {
        updateIViewTitle(_string(R.string.engrave_finish))
        engraveBackFlow = 0
        showCloseView(true, _string(R.string.back_creation))
        renderDslAdapter {
            EngraveFinishTopItem()() {
                itemEngraveConfigInfo = engraveConfigInfo
            }
            //
            engraveConfigInfo?.engraveDataParamList?.forEach { engraveDataParam ->
                val layerInfo =
                    EngraveTransitionManager.engraveLayerList.find { it.mode == engraveDataParam.layerMode }

                EngraveLabelItem()() {
                    itemText = layerInfo?.label
                }
                EngraveFinishInfoItem()() {
                    itemEngraveDataParam = engraveDataParam
                }
            }
            //
            EngraveFinishControlItem()() {
                itemShareAction = {
                    toast("功能开发中...")
                }
                itemAgainAction = {
                    //再次雕刻, 回退到参数配置页面
                    engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                    renderFlowItems()
                }
            }
        }
    }

}