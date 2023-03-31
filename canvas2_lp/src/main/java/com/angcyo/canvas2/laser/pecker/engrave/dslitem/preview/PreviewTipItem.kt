package com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.library.component.watchCount
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.toColorInt
import com.angcyo.library.ex.visible
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base._textColor

/**
 * 拖动动态预览提示/其他提示
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/09/23
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class PreviewTipItem : DslAdapterItem() {

    /**提示语*/
    var itemTip: CharSequence? = _string(R.string.preview_drag_tip)

    /**提示语颜色*/
    var itemTipTextColor: Int = "#0a84ff".toColorInt()//_color(R.color.colorAccent)

    init {
        itemLayoutId = R.layout.item_preview_tip_layout

        //分享雕刻日志
        itemClick = if (isDebug()) {
            {
                DeviceHelper.shareEngraveLog()
            }
        } else {
            {
                it.watchCount(5) {
                    DeviceHelper.shareEngraveLog()
                }
            }
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val visible = !itemTip.isNullOrBlank()
        itemHolder.itemView.visible(visible)
        itemHolder.tv(R.id.lib_text_view)?.apply {
            visible(visible)
            text = itemTip
            _textColor = itemTipTextColor
        }
    }
}