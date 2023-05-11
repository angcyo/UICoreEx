package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Typeface
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.model.TypefaceInfo
import com.angcyo.widget.DslViewHolder

/**
 * 字体展示的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
class TypefaceItem : DslAdapterItem() {

    companion object {

        /**获取字体状态同步资源*/
        var getTypefaceItemSyncStateRes: (item: TypefaceItem) -> Int? = { null }
    }

    /**字体的信息*/
    val itemTypefaceInfo: TypefaceInfo?
        get() = itemData as? TypefaceInfo?

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
        //同步状态
        getTypefaceItemSyncStateRes(this).let {
            itemHolder.visible(R.id.lib_sync_view, it != null)
            itemHolder.img(R.id.lib_sync_view)?.setImageResource(it ?: 0)
        }
    }
}