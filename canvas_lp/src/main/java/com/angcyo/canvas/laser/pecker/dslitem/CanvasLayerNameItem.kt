package com.angcyo.canvas.laser.pecker.dslitem

import android.view.View
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.data.EngraveLayerInfo
import com.angcyo.widget.DslViewHolder

/**
 * 图层名字item
 *
 * 图片图层/填充图层/雕刻图层
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/28
 */
class CanvasLayerNameItem : DslAdapterItem() {

    /**雕刻图层信息*/
    var itemLayerInfo: EngraveLayerInfo? = null

    init {
        itemLayoutId = R.layout.item_canvas_layer_name_layout
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
        imageView?.apply {
            rotation = if (itemGroupExtend) {
                -90f
            } else {
                -180f
            }
        }

        itemHolder.tv(R.id.lib_text_view)?.text = itemLayerInfo?.label

        //展开or关闭动画控制
        itemHolder.clickItem {
            imageView?.apply {
                animate()
                    .setDuration(300)
                    .rotation(if (itemGroupExtend) -90f else -180f)
                    .start()
            }
            itemGroupExtend = !itemGroupExtend
        }
    }

}