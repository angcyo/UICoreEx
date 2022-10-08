package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.drawable.Drawable
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
open class CanvasBaseLayerItem : DslAdapterItem() {

    //region ---core---

    var itemCanvasDelegate: CanvasDelegate? = null

    var itemRenderer: BaseItemRenderer<*>? = null

    //endregion ---core---

    //region ---计算属性---

    val itemLayerHide: Boolean get() = itemRenderer?.isVisible() == false

    val itemItemDrawable: Drawable? get() = itemRenderer?.getRendererRenderItem()?.itemLayerDrawable

    val itemItemName: CharSequence? get() = itemRenderer?.getRendererRenderItem()?.itemLayerName

    //endregion ---计算属性---

    init {
        itemLayoutId = R.layout.item_canvas_base_layer_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //item 名称
        itemHolder.tv(R.id.layer_item_name_view)?.text = itemItemName
        itemHolder.img(R.id.layer_item_drawable_view)
            ?.setImageDrawable(itemItemDrawable ?: itemRenderer?.preview())
    }

}