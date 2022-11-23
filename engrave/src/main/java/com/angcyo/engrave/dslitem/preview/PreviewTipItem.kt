package com.angcyo.engrave.dslitem.preview

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toColorInt
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
    var itemTip: CharSequence? = null

    /**提示语颜色*/
    var itemTipTextColor: Int = "#0a84ff".toColorInt()//_color(R.color.colorAccent)

    init {
        itemLayoutId = R.layout.item_preview_tip_layout

        itemTip = _string(R.string.preview_drag_tip)
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.apply {
            text = itemTip
            _textColor = itemTipTextColor
        }
    }
}