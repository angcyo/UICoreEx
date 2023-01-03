package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.TextView
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.utils.canvasDecimal
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.ex.clamp
import com.angcyo.library.unit.PointValueUnit
import com.angcyo.library.unit.convertPixelToValueUnit
import com.angcyo.library.unit.toPixel
import com.angcyo.widget.DslViewHolder
import kotlin.math.min

/**
 * 文本属性控制输入item
 * 字号/字间距/行间距
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/19
 */
class TextPropertyControlItem : DslAdapterItem() {

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
        if (renderer is DataItemRenderer) {
            val dataItem = renderer.getRendererRenderItem()
            itemHolder.tv(R.id.item_paint_size_view)?.text =
                itemPointValueUnit.convertPixelToValueUnit(dataItem?.dataBean?.fontSize.toPixel())

            val valueUit = renderer.canvasViewBox.valueUnit
            itemHolder.tv(R.id.item_word_space_view)?.text =
                valueUit.convertPixelToValue(dataItem?.dataBean?.charSpacing.toPixel())
                    .canvasDecimal(2)
            //dataItem?.dataBean?.charSpacing.toPixel().canvasDecimal(2)
            itemHolder.tv(R.id.item_line_space_view)?.text =
                valueUit.convertPixelToValue(dataItem?.dataBean?.lineSpacing.toPixel())
                    .canvasDecimal(2)
            //dataItem?.dataBean?.lineSpacing.toPixel().canvasDecimal(2)

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
    fun bindPaintSize(itemHolder: DslViewHolder, renderer: DataItemRenderer) {
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
                    renderer.dataTextItem?.updateTextSize(pixel, renderer)
                }
            }
        }
    }

    /**字间距*/
    fun bindWordSpace(itemHolder: DslViewHolder, renderer: DataItemRenderer) {
        val valueUit = renderer.canvasViewBox.valueUnit
        itemHolder.click(R.id.item_word_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = min(
                        valueUit.convertValueToPixel(number),
                        HawkEngraveKeys.maxTextSize
                    )
                    renderer.dataTextItem?.updateTextWordSpacing(size, renderer)
                }
            }
        }
    }

    /**行间距*/
    fun bindLineSpace(itemHolder: DslViewHolder, renderer: DataItemRenderer) {
        val valueUit = renderer.canvasViewBox.valueUnit
        itemHolder.click(R.id.item_line_space_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@TextPropertyControlItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { number ->
                    val size = min(
                        valueUit.convertValueToPixel(number),
                        HawkEngraveKeys.maxTextSize
                    )
                    renderer.dataTextItem?.updateTextLineSpacing(size, renderer)
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