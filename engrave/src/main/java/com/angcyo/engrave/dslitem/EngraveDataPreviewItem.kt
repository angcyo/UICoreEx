package com.angcyo.engrave.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveReadyDataInfo
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
    var itemEngraveReadyDataInfo: EngraveReadyDataInfo? = null

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

        var bitmap = itemEngraveReadyDataInfo?.optionBitmap
        if (bitmap == null) {
            bitmap = itemEngraveReadyDataInfo?.previewDataPath?.toBitmap()
            itemEngraveReadyDataInfo?.optionBitmap = bitmap
        }

        itemHolder.v<PhotoView>(R.id.lib_image_view)
            ?.setImageBitmap(itemEngraveReadyDataInfo?.optionBitmap)
    }

}