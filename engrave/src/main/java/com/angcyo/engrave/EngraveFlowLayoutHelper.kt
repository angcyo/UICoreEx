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

    init {
        iViewLayoutId = R.layout.canvas_engrave_flow_layout
    }

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
}