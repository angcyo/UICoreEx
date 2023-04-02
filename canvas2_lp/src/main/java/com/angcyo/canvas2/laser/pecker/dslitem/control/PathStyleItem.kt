package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Paint
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.toPaintStyle
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.widget.DslViewHolder

/**
 * 路径样式 填充/描边/描边+填充
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/16
 */
class PathStyleItem : CanvasIconItem() {

    /**当前item所表示的样式*/
    var itemStyle: Paint.Style = Paint.Style.STROKE

    init {
        itemSingleSelectMutex = true
        itemClick = {
            itemRenderer?.lpElement()?.apply {
                updateElement(itemRenderer, itemRenderDelegate) {
                    this@apply.elementBean.paintStyle = itemStyle.toPaintStyleInt()
                }
            }
            updateItemSelected(true)
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val paintStyle = elementBean?.paintStyle?.toPaintStyle()
        itemIsSelected = paintStyle == itemStyle
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

}