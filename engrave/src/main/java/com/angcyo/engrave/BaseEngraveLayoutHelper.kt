package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.engrave.dslitem.engrave.*
import com.angcyo.engrave.dslitem.preview.PreviewTipItem
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.item.style.itemLabelText
import com.angcyo.library.ex._string

/**
 * 雕刻item布局渲染
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
abstract class BaseEngraveLayoutHelper : BaseEngravePreviewLayoutHelper() {

    override fun renderFlowItems() {
        when (engraveFlow) {
            ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG -> renderTransferConfig()
            ENGRAVE_FLOW_TRANSMITTING -> renderTransmitting()
            ENGRAVE_FLOW_BEFORE_CONFIG -> renderEngraveConfig()
            else -> super.renderFlowItems()
        }
    }

    /**生成百分比数值列表*/
    fun percentList(max: Int = 100): List<Int> {
        return (1..max).toList()
    }

    //

    /**渲染传输数据配置界面*/
    fun renderTransferConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        renderDslAdapter {
            EngraveDataNameItem()() {
                //itemEngraveReadyInfo = engraveReadyInfo
            }
            EngraveDataPxItem()() {
                //itemEngraveDataInfo = dataInfo
                itemPxList = LaserPeckerHelper.findProductSupportPxList()
            }
            EngraveDividerItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据传输界面
                    engraveFlow = ENGRAVE_FLOW_TRANSMITTING
                    renderFlowItems()
                }
            }
        }
    }

    /**渲染传输中的界面*/
    fun renderTransmitting() {
        updateIViewTitle(_string(R.string.transmitting))
        showCloseView(false)

        renderDslAdapter {
            DataTransmittingItem()() {
                itemProgress = 50
            }
            DataStopTransferItem()() {
                itemClick = {
                    //结束文件传输
                    engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
                    renderFlowItems()
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

                }
            }
        }
    }

}