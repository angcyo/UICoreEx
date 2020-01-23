package com.angcyo.pager.dslitem

import android.widget.ImageView
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.glide.loadImage
import com.angcyo.pager.R
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/22
 */
open class DslPhotoViewItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.dsl_photo_view_item
    }

    var imageUrl: String? = null

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem)

        itemHolder.img(R.id.lib_image_view)?.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            loadImage(imageUrl) {
                checkGifType = true
                originalSize = true
            }
        }
    }
}