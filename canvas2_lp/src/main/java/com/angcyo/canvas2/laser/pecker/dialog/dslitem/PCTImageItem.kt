package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import androidx.fragment.app.FragmentActivity
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.ParameterComparisonTableDialogConfig
import com.angcyo.canvas2.laser.pecker.dialog.updateTablePreview
import com.angcyo.component.getFiles
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.component.ROpenFileHelper
import com.angcyo.library.component.lastActivity
import com.angcyo.library.ex.isImageType
import com.angcyo.library.ex.toBitmap
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/11/08
 */
class PCTImageItem : DslAdapterItem() {

    init {
        itemLayoutId = R.layout.item_pct_image_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.visible(
            R.id.lib_clear_view,
            ParameterComparisonTableDialogConfig.selectImage != null
        )
        itemHolder.img(R.id.lib_image_view)
            ?.setImageBitmap(ParameterComparisonTableDialogConfig.selectImage)
        itemHolder.click(R.id.lib_image_view) {
            lastActivity?.let {
                if (it is FragmentActivity) {
                    it.supportFragmentManager.getFiles(1) { uriList ->
                        val uri = uriList?.lastOrNull()
                        if (uri != null) {
                            val filePath = ROpenFileHelper.parseData(uri)
                            if (filePath.isImageType()) {
                                ParameterComparisonTableDialogConfig.selectImage =
                                    filePath?.toBitmap()
                                updateAdapterItem()
                                itemDslAdapter.updateTablePreview()
                            }
                        }
                    }
                }
            }
        }

        itemHolder.click(R.id.lib_clear_view) {
            ParameterComparisonTableDialogConfig.selectImage = null
            updateAdapterItem()
            itemDslAdapter.updateTablePreview()
        }
    }
}