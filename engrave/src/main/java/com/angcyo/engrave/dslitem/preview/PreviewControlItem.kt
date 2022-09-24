package com.angcyo.engrave.dslitem.preview

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.library.component.scaleDrawable
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
    var rangePreviewItem: ControlTextItem? = null

    /**中心点预览*/
    var centerPreviewItem: ControlTextItem? = null

    /**暂停预览*/
    var pausePreviewItem: ControlTextItem? = null

    init {
        itemLayoutId = R.layout.item_preview_control_layout

        //预览范围
        rangePreviewItem = ControlTextItem(
            _string(R.string.range_preview),
            R.drawable.preview_range_svg,
            false
        ) { viewHolder, item ->
            if (!item.selected) {
                //范围预览
                HawkEngraveKeys.isRectCenterPreview = false
                previewModel.refreshPreviewBounds(true, false)
                item.selected = true
                centerPreviewItem?.selected = false
                resetControlLayout(viewHolder)
                queryDeviceStateCmd()
            }
        }

        //中心点预览
        centerPreviewItem = ControlTextItem(
            _string(R.string.center_preview),
            R.drawable.preview_center_svg,
            false
        ) { viewHolder, item ->
            if (!item.selected) {
                if (HawkEngraveKeys.needRectCenterPreview()) {
                    //矩形中心点预览
                    HawkEngraveKeys.isRectCenterPreview = true
                    previewModel.rectCenterPreview(true, false)
                } else {
                    previewModel.previewShowCenter(true)
                }
                item.selected = true
                rangePreviewItem?.selected = false
                resetControlLayout(viewHolder)
                queryDeviceStateCmd()
            }
        }

        //暂停预览
        pausePreviewItem = ControlTextItem(
            _string(R.string.pause_preview),
            R.drawable.preview_pause_svg,
            false
        ) { viewHolder, item ->
            if (item.selected) {
                //已经是第三轴暂停预览, 则第三轴继续预览
                zContinuePreviewCmd()
            } else {
                //需要第三轴暂停预览
                previewModel.refreshPreviewBounds(true, true)
            }
            item.selected = !item.selected
            viewHolder.selected(item.selected)
            queryDeviceStateCmd()
        }
    }

    val itemList = mutableListOf<ControlTextItem>()

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemList.clear()
        itemList.add(rangePreviewItem!!)
        itemList.add(centerPreviewItem!!)
        if (laserPeckerModel.haveExDevice()) {
            itemList.add(pausePreviewItem!!)
        }
        //
        rangePreviewItem?.selected = false
        centerPreviewItem?.selected = false
        pausePreviewItem?.selected = false

        //
        val deviceStateData = laserPeckerModel.deviceStateData.value
        if (deviceStateData?.isModeEngravePreview() == true) {
            if (deviceStateData.workState == 0x07 || HawkEngraveKeys.isRectCenterPreview) {
                //显示中心点
                centerPreviewItem?.selected = true
            } else {
                rangePreviewItem?.selected = true
            }

            if (deviceStateData.workState == 0x05) {
                //第三轴继续预览
            } else if (deviceStateData.workState == 0x04) {
                //第三轴暂停预览
                pausePreviewItem?.selected = true
            }
        }

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
                    clickItem {
                        item.clickAction(itemHolder, item)
                    }
                }
            }
        }
    }

    /**数据结构*/
    data class ControlTextItem(
        val label: String,
        val ico: Int = -1,
        var selected: Boolean = false,
        val clickAction: (DslViewHolder, ControlTextItem) -> Unit
    )

}