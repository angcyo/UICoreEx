package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Typeface
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.widget.DslViewHolder

/**
 * 字体展示的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class TypefaceItem : DslAdapterItem() {

    /**显示的名字*/
    var displayName: String? = null

    /**预览的文本*/
    var previewText: String? = null

    /**字体*/
    var typeface: Typeface? = null

    init {
        itemLayoutId = R.layout.item_canvas_typeface_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.visible(R.id.lib_check_view, itemIsSelected)

        itemHolder.tv(R.id.text_view)?.apply {
            text = previewText
            this.typeface = this@TypefaceItem.typeface
        }
        itemHolder.tv(R.id.name_view)?.apply {
            text = displayName
        }
    }
}