package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Color
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.doodle.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._color
import com.angcyo.library.ex.alphaRatio
import com.angcyo.widget.DslViewHolder

/**
 * 图片算法提示item, 提示当前的图片正在使用什么算法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/04
 */
class CanvasImageFilterItem : CanvasControlItem2() {

    /**当前的item, 表示的图片算法*/
    var itemImageFilter: Int = -1

    /**是否是扭曲的item*/
    var itemIsMeshItem: Boolean = false

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val renderer = itemRenderer
        itemCheckColor = if (renderer is DataItemRenderer) {
            val dataBean = renderer.dataItem?.dataBean
            if (itemImageFilter == dataBean?.imageFilter || (itemIsMeshItem && dataBean?.isMesh == true)) {
                _color(R.color.colorAccent).alphaRatio(0.5f)
            } else {
                Color.TRANSPARENT
            }
        } else {
            Color.TRANSPARENT
        }
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }
}