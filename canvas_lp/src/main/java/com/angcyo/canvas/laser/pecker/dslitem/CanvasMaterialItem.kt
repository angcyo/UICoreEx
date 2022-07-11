package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.drawable.Drawable
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/09
 */
class CanvasMaterialItem : DslAdapterItem() {

    var itemDrawable: Drawable? = null

    init {
        itemLayoutId = R.layout.item_canvas_material_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.img(R.id.lib_image_view)?.setImageDrawable(itemDrawable)
    }

}