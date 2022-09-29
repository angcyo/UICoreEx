package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.core.vmApp
import com.angcyo.engrave.data.TransferDataConfigInfo
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.engrave.dslitem.transfer.TransferDataNameItem
import com.angcyo.engrave.dslitem.transfer.TransferDataPxItem
import com.angcyo.engrave.model.TransferModel
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.ex._string
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
        transferModel.transferStateData.observe(this, allowBackward = false) {
            it?.apply {
                //数据传输进度通知
                if (engraveFlow == ENGRAVE_FLOW_TRANSMITTING) {
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

    /**数据配置信息*/
    var transferDataConfigInfo: TransferDataConfigInfo = TransferDataConfigInfo()

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
                /*val material =
                    engraveOptionInfo?.material ?: _string(R.string.material_custom)
                val index = materialList.indexOfFirst { it.toText() == material }
                if (index == -1) {
                    engraveOptionInfo?.material =
                        materialList.getOrNull(0)?.toText()?.toString()
                            ?: _string(R.string.material_custom)
                }
                itemSelectedIndex = max(0, index)
                itemTag = EngraveOptionInfo::material.name
                itemEngraveOptionInfo = engraveOptionInfo*/
            }
            EngraveSegmentScrollItem()() {
                itemText = _string(R.string.laser_type)
                itemSegmentList = LaserPeckerHelper.findProductSupportLaserTypeList()
            }
            EngraveLayerConfigItem()() {
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_power)
                itemWheelList = percentList()
                /*itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveOptionInfo?.power)
                itemTag = EngraveOptionInfo::power.name
                itemEngraveOptionInfo = engraveOptionInfo*/
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.custom_speed)
                itemWheelList = percentList()
                /*itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveOptionInfo?.depth)
                itemTag = EngraveOptionInfo::depth.name
                itemEngraveOptionInfo = engraveOptionInfo*/
            }
            EngraveOptionWheelItem()() {
                itemLabelText = _string(R.string.print_times)
                itemWheelList = percentList(255)
                /*itemSelectedIndex =
                    EngraveHelper.findOptionIndex(itemWheelList, engraveOptionInfo?.time)
                itemTag = EngraveOptionInfo::time.name
                itemEngraveOptionInfo = engraveOptionInfo*/
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

            }
            EngravingInfoItem()() {

            }
            EngravingControlItem()() {
                itemStopAction = {
                    engraveFlow = ENGRAVE_FLOW_FINISH
                    renderFlowItems()
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
                itemText = "材质:angcyo 材质:angcyo 材质:angcyo"
            }
            EngraveLabelItem()() {
                itemText = _string(R.string.engrave_layer_bitmap)
            }
            EngraveFinishInfoItem()()
            EngraveLabelItem()() {
                itemText = _string(R.string.engrave_layer_fill)
            }
            EngraveFinishInfoItem()()
            EngraveLabelItem()() {
                itemText = _string(R.string.engrave_layer_line)
            }
            EngraveFinishInfoItem()()
            EngraveFinishControlItem()()
        }
    }

}