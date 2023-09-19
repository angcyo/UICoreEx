package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Color
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement
import com.angcyo.doodle.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._color
import com.angcyo.library.ex.alphaRatio
import com.angcyo.widget.DslViewHolder

/**
 * 图片算法提示item, 提示当前的图片正在使用什么算法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class ImageFilterItem : CanvasIconItem() {

    /**当前的item, 表示的图片算法, 用来判断高亮*/
    var itemImageFilter: Int = -1

    /**是否是扭曲的item*/
    var itemIsMeshItem: Boolean = false

    /**是否是浮雕item*/
    var itemIsReliefItem: Boolean = false

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val renderer = itemRenderer
        val element = renderer?.renderElement
        itemCheckColor = if (element is ILaserPeckerElement) {
            val dataBean = element.elementBean
            if (itemImageFilter == dataBean.imageFilter || (itemIsMeshItem && dataBean.isMesh)) {
                _color(R.color.colorAccentNight).alphaRatio(0.5f)
            } else {
                Color.TRANSPARENT
            }
        } else {
            Color.TRANSPARENT
        }

        //浮雕item的激活逻辑
        if (itemIsReliefItem) {
            itemEnable = _elementBean?.isSupportSliceElement == true
        }

        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }
}