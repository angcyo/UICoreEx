package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.widget.TextView
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.canvasDecimal
import com.angcyo.canvas.render.util.textElement
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.data.HawkEngraveKeys
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
class TextPropertyControlItem : DslAdapterItem(), ICanvasRendererItem {

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

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
        val element = itemRenderer?.textElement

        if (renderer != null && element != null) {
            val textProperty = element.textProperty
            itemHolder.tv(R.id.item_paint_size_view)?.text =
                itemPointValueUnit.convertPixelToValueUnit(textProperty.fontSize)

            val renderUnit = itemRenderDelegate?.axisManager?.renderUnit
            itemHolder.tv(R.id.item_word_space_view)?.text =
                renderUnit?.convertPixelToValue(textProperty.charSpacing)?.canvasDecimal(2)
            itemHolder.tv(R.id.item_line_space_view)?.text =
                renderUnit?.convertPixelToValue(textProperty.lineSpacing)?.canvasDecimal(2)

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
    fun bindPaintSize(itemHolder: DslViewHolder, renderer: BaseRenderer) {
        itemHolder.click(R.id.item_paint_size_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = itemPointValueUnit.convertValueToPixel(number)
                    val pixel = clamp(
                        size,
                        HawkEngraveKeys.minTextSize,
                        HawkEngraveKeys.maxTextSize
                    )
                    itemRenderer?.textElement?.updateTextProperty(renderer, itemRenderDelegate) {
                        fontSize = pixel
                    }
                }
            }
        }
    }

    /**字间距*/
    fun bindWordSpace(itemHolder: DslViewHolder, renderer: BaseRenderer) {
        val renderUnit = itemRenderDelegate?.axisManager?.renderUnit ?: return
        itemHolder.click(R.id.item_word_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = min(
                        renderUnit.convertValueToPixel(number),
                        HawkEngraveKeys.maxTextSize
                    )
                    itemRenderer?.textElement?.updateTextProperty(renderer, itemRenderDelegate) {
                        charSpacing = size
                    }
                }
            }
        }
    }

    /**行间距*/
    fun bindLineSpace(itemHolder: DslViewHolder, renderer: BaseRenderer) {
        val renderUnit = itemRenderDelegate?.axisManager?.renderUnit ?: return
        itemHolder.click(R.id.item_line_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = min(
                        renderUnit.convertValueToPixel(number),
                        HawkEngraveKeys.maxTextSize
                    )
                    itemRenderer?.textElement?.updateTextProperty(renderer, itemRenderDelegate) {
                        lineSpacing = size
                    }
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    private fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }
}