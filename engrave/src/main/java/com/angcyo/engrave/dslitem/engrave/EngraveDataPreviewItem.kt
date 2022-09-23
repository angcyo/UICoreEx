package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveReadyInfo
import com.angcyo.library.ex.toBitmap
import com.angcyo.widget.DslViewHolder
import com.github.chrisbanes.photoview.PhotoView

/**
 * 雕刻数据预览item,
 * 用来预览数据的[Bitmap]对象
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/01
 */
class EngraveDataPreviewItem : DslAdapterItem() {

    /**待雕刻的数据*/
    var itemEngraveReadyInfo: EngraveReadyInfo? = null

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

        var bitmap = itemEngraveReadyInfo?.dataBitmap
        if (bitmap == null) {
            bitmap = itemEngraveReadyInfo?.previewDataPath?.toBitmap()
            itemEngraveReadyInfo?.dataBitmap = bitmap
        }

        itemHolder.v<PhotoView>(R.id.lib_image_view)
            ?.setImageBitmap(itemEngraveReadyInfo?.dataBitmap)
    }

}