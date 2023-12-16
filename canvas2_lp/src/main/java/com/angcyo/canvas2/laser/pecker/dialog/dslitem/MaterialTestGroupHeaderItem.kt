package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import android.view.View
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.containsPayload
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/12/16
 */
class MaterialTestGroupHeaderItem : DslAdapterItem() {

    /**Label*/
    var itemLabel: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_material_test_group_header
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //箭头角度控制
        val imageView = itemHolder.v<View>(R.id.lib_image_view)
        if (!payloads.containsPayload(PAYLOAD_UPDATE_EXTEND)) {
            imageView?.rotation = if (itemGroupExtend) 0f else 180f
        }

        itemHolder.tv(R.id.lib_text_view)?.text = itemLabel

        //展开or关闭动画控制
        itemHolder.clickItem {
            imageView?.apply {
                animate().setDuration(300).rotation(if (itemGroupExtend) 180f else 0f)
                    .start()
            }
            itemGroupExtend = !itemGroupExtend
        }
    }
}