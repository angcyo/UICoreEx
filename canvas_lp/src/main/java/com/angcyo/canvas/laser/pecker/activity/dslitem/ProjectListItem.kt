package com.angcyo.canvas.laser.pecker.activity.dslitem

import com.angcyo.canvas.data.CanvasProjectBean
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/01
 */
class ProjectListItem : DslAdapterItem() {

    var itemProjectBean: CanvasProjectBean? = null

    init {
        itemLayoutId = R.layout.item_project_list_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = itemProjectBean?.file_name
        itemHolder.img(R.id.lib_image_view)
            ?.setImageBitmap(itemProjectBean?.preview_img?.toBitmapOfBase64())
    }
}