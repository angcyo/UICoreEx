package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Paint
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.items.renderer.IItemRenderer
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex.color
import com.angcyo.library.ex.toColorInt
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.clickIt

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/18
 */
@Deprecated("废弃")
class TextSolidStyleItem(
    val renderer: IItemRenderer<*>,
    val canvasView: CanvasView
) : DslAdapterItem() {

    init {
        itemLayoutId = R.layout.layout_canvas_text_solid_style
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val strokeView = itemHolder.img(R.id.text_stroke_view)
        val solidView = itemHolder.img(R.id.text_solid_view)

        if (renderer is PictureTextItemRenderer) {
            val isStroke = renderer.paint.style == Paint.Style.STROKE
            strokeView?.apply {
                val drawable =
                    drawable.color(if (isStroke) "#282828".toColorInt() else "#b3b7ba".toColorInt())
                setImageDrawable(drawable)

                clickIt {
                    renderer.updatePaintStyle(Paint.Style.STROKE)
                    updateAdapterItem()
                }
            }
            solidView?.apply {
                val drawable =
                    drawable.color(if (!isStroke) "#282828".toColorInt() else "#b3b7ba".toColorInt())
                setImageDrawable(drawable)

                clickIt {
                    renderer.updatePaintStyle(Paint.Style.FILL)
                    updateAdapterItem()
                }
            }
        }
    }
}