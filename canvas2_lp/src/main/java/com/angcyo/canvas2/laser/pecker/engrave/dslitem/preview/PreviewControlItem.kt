package com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave2.data.PreviewInfo
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.component.scaleDrawable
import com.angcyo.library.ex.Action1
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.dslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.flow
import com.angcyo.widget.span.span

/**
 * 预览控制item
 * 范围预览/显示中心点/等
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/09/23
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class PreviewControlItem : BasePreviewItem() {

    /**预览范围*/
    var rangePreviewItem: ControlTextItemInfo? = null

    /**中心点预览*/
    var centerPreviewItem: ControlTextItemInfo? = null

    /**第三轴暂停预览*/
    var pausePreviewItem: ControlTextItemInfo? = null

    /**第三轴继续预览*/
    var continuePreviewItem: ControlTextItemInfo? = null

    /**C1 第三轴滚动*/
    var scrollPreviewItem: ControlTextItemInfo? = null

    /**路径预览, 只在GCode数据时有效*/
    var pathPreviewItem: ControlTextItemInfo? = null

    /**路径预览回调*/
    var itemPathPreviewClick: Action1? = null

    /**第三轴预览后, 机器空闲状态后, 是否需要重新开始预览*/
    val _needRestartPreview: Boolean
        get() = laserPeckerModel.isL1() || laserPeckerModel.isL2() || laserPeckerModel.isL3()

    init {
        itemLayoutId = R.layout.item_preview_control_layout

        //预览范围
        rangePreviewItem = ControlTextItemInfo(
            _string(R.string.range_preview),
            R.drawable.preview_range_svg,
            false
        ) { viewHolder, item ->
            if (!item.selected) {
                //范围预览
                previewModel.updatePreview(restore = true) {
                    isCenterPreview = false
                }
                item.selected = true
                centerPreviewItem?.selected = false
                resetControlLayout(viewHolder)
                queryDeviceStateCmd()
            }
        }

        //中心点预览
        centerPreviewItem = ControlTextItemInfo(
            _string(R.string.center_preview),
            R.drawable.preview_center_svg,
            false
        ) { viewHolder, item ->
            if (!item.selected) {
                previewModel.updatePreview(restore = true) {
                    isCenterPreview = true
                }
                item.selected = true
                rangePreviewItem?.selected = false
                resetControlLayout(viewHolder)
                queryDeviceStateCmd()
            }
        }

        //第三轴暂停预览
        pausePreviewItem = ControlTextItemInfo(
            _string(R.string.pause_preview),
            R.drawable.bracket_stop_svg,
            false
        ) { viewHolder, item ->
            if (deviceStateModel.isIdleMode() && _needRestartPreview) {
                previewModel.updatePreview(restore = true) {
                    isCenterPreview = false
                    zState = null
                }
            } else {
                previewModel.updatePreview(restore = true) {
                    isCenterPreview = false
                    //z轴需要处于的状态
                    zState = PreviewInfo.Z_STATE_PAUSE
                }
            }
            queryDeviceStateCmd()
        }

        //第三轴继续预览
        continuePreviewItem = ControlTextItemInfo(
            _string(R.string.continue_preview),
            R.drawable.preview_pause_svg,
            false
        ) { viewHolder, item ->
            if (deviceStateModel.isIdleMode() && _needRestartPreview) {
                previewModel.updatePreview(restore = true) {
                    isCenterPreview = false
                    zState = null
                }
            } else {
                previewModel.updatePreview(restore = true) {
                    isCenterPreview = false
                    //z轴需要处于的状态
                    zState = PreviewInfo.Z_STATE_CONTINUE
                }
            }
            queryDeviceStateCmd()
        }

        //C1 第三轴滚动
        scrollPreviewItem = ControlTextItemInfo(
            _string(R.string.scroll_preview),
            R.drawable.scroll_preview_svg,
            false
        ) { viewHolder, item ->
            previewModel.updatePreview(restore = true) {
                isCenterPreview = false
                //z轴需要处于的状态
                zState = PreviewInfo.Z_STATE_SCROLL
            }
            queryDeviceStateCmd()
        }

        //路径预览
        pathPreviewItem = ControlTextItemInfo(
            _string(R.string.device_setting_act_model_preview_g_code),
            R.drawable.preview_path_svg,
            false
        ) { viewHolder, item ->
            //进入路径预览
            val previewInfoData = previewModel.previewInfoData.value
            itemPathPreviewClick?.invoke(previewInfoData?.elementBean)
        }
    }

    val itemList = mutableListOf<ControlTextItemInfo>()

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //
        val previewInfoData = previewModel.previewInfoData.value
        val deviceStateData = deviceStateModel.deviceStateData.value

        itemList.clear()
        if (laserPeckerModel.haveExDevice()) {
            itemList.add(pausePreviewItem!!)
            itemList.add(continuePreviewItem!!)

            if (laserPeckerModel.isC1() && !laserPeckerModel.isZOpen()) {
                //C1专属 第三轴滚动 //2023-4-14 z轴下, 不显示滚动按钮
                itemList.add(scrollPreviewItem!!)
            }
        } else {
            itemList.add(rangePreviewItem!!)
            itemList.add(centerPreviewItem!!)

            if (laserPeckerModel.isC1()) {
                //C1不支持此操作
            } else if (laserPeckerModel.deviceSettingData.value?.gcodeView == QuerySettingParser.GCODE_PREVIEW &&
                previewInfoData?.elementBean?._layerMode == LPDataConstant.DATA_MODE_GCODE
            ) {
                //开启了向量预览, 并且GCode数据模式下, 才有路径预览
                itemList.add(pathPreviewItem!!)
            }
        }

        //
        centerPreviewItem?.selected = previewInfoData?.isCenterPreview == true
        rangePreviewItem?.selected = centerPreviewItem?.selected == false //互斥

        //
        resetControlLayout(itemHolder)
    }

    fun resetControlLayout(itemHolder: DslViewHolder) {
        itemHolder.flow(R.id.lib_flow_layout)?.apply {
            resetChild(
                itemList,
                R.layout.layout_preview_control_view
            ) { itemView, item, _ ->
                itemView.dslViewHolder().apply {
                    tv(R.id.lib_text_view)?.text = span {
                        val size = 20
                        appendDrawable(_drawable(item.ico).scaleDrawable(size * dpi, size * dpi))
                        append("  ")
                        append(item.label)
                    }
                    selected(item.selected)
                    //上屏
                    throttleClickItem {
                        //这里用this or itemHolder
                        item.clickAction(itemHolder, item)
                    }
                }
            }
        }
    }

    /**数据结构*/
    data class ControlTextItemInfo(
        val label: String,
        val ico: Int = -1,
        var selected: Boolean = false,
        val clickAction: (DslViewHolder, ControlTextItemInfo) -> Unit
    )

}