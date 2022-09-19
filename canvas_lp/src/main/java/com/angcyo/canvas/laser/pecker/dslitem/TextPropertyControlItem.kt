package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.TextView
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.items.PictureTextItem
import com.angcyo.canvas.items.renderer.PictureTextItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.ex.clamp
import com.angcyo.library.unit.PointValueUnit
import com.angcyo.library.unit.convertPixelToValueUnit
import com.angcyo.widget.DslViewHolder
import kotlin.math.min

/**
 * 文本属性控制输入item
 * 字号/字间距/行间距
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class TextPropertyControlItem : DslAdapterItem() {

    companion object {
        /**文本最小的字体大小, 像素*/
        const val TEXT_MIN_SIZE = 5f

        /**文本最大的字体大小, 像素*/
        const val TEXT_MAX_SIZE = 500f
    }

    var itemRenderer: IRenderer? = null

    /**pt单位转换*/
    val itemPointValueUnit = PointValueUnit()

    init {
        itemLayoutId = R.layout.item_text_property_control_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val renderer = itemRenderer
        if (renderer is PictureTextItemRenderer) {

            itemHolder.tv(R.id.item_paint_size_view)?.text =
                itemPointValueUnit.convertPixelToValueUnit(renderer.paint.textSize)

            val renderItem = renderer.getRendererRenderItem()
            if (renderItem is PictureTextItem) {
                itemHolder.tv(R.id.item_word_space_view)?.text =
                    renderItem.wordSpacing.canvasDecimal(2)
                itemHolder.tv(R.id.item_line_space_view)?.text =
                    renderItem.lineSpacing.canvasDecimal(2)
            }

            bindPaintSize(itemHolder, renderer)
            bindWordSpace(itemHolder, renderer)
            bindLineSpace(itemHolder, renderer)
        } else {
            itemHolder.tv(R.id.item_paint_size_view)?.text = null
            itemHolder.tv(R.id.item_word_space_view)?.text = null
            itemHolder.tv(R.id.item_line_space_view)?.text = null
        }
    }

    /**字号*/
    fun bindPaintSize(itemHolder: DslViewHolder, renderer: PictureTextItemRenderer) {
        itemHolder.click(R.id.item_paint_size_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = clamp(number, TEXT_MIN_SIZE, TEXT_MAX_SIZE)
                    val pixel = itemPointValueUnit.convertValueToPixel(size)
                    renderer.updateTextSize(pixel)
                }
            }
        }
    }

    /**字间距*/
    fun bindWordSpace(itemHolder: DslViewHolder, renderer: PictureTextItemRenderer) {
        itemHolder.click(R.id.item_word_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = min(number, TEXT_MAX_SIZE)
                    renderer.updateTextWordSpacing(size)
                }
            }
        }
    }

    /**行间距*/
    fun bindLineSpace(itemHolder: DslViewHolder, renderer: PictureTextItemRenderer) {
        itemHolder.click(R.id.item_line_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = min(number, TEXT_MAX_SIZE)
                    renderer.updateTextLineSpacing(size)
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }

}