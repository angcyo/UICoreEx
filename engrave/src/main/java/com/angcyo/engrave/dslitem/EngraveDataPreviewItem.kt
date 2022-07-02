package com.angcyo.engrave.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.library.annotation.Implementation
import com.angcyo.widget.DslViewHolder
import com.github.chrisbanes.photoview.PhotoView

/**
 * 雕刻数据预览item, 数据目前支持GCode, Bitmap
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
@Implementation
class EngraveDataPreviewItem : DslAdapterItem() {

    /**待雕刻的数据*/
    var itemEngraveDataInfo: EngraveDataInfo? = null

    init {
        itemLayoutId = R.layout.item_engrave_data_preview
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<PhotoView>(R.id.lib_image_view)
            ?.setImageBitmap(itemEngraveDataInfo?.optionBitmap)
    }

}