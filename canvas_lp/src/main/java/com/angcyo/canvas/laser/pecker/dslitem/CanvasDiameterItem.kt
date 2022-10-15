package com.angcyo.canvas.laser.pecker.dslitem

import android.widget.TextView
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.canvas.utils.CanvasConstant.valueUnit
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder

/**
 * 直径/周长输入item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/14
 */
class CanvasDiameterItem : DslAdapterItem() {

    companion object {

        /**直径转周长*/
        fun Float.toPerimeter(): Float = (Math.PI * this).toFloat()

        /**周长转直径*/
        fun Float.toDiameter(): Float = (this / Math.PI).toFloat()
    }

    /**直径的label*/
    var itemDiameterLabel: CharSequence? = null

    /**直径, 像素单位*/
    @Pixel
    var itemDiameter: Float = 0f

    init {
        itemLayoutId = R.layout.item_canvas_diameter_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val unit = valueUnit
        itemHolder.tv(R.id.perimeter_unit_view)?.text = unit.getUnit()
        itemHolder.tv(R.id.diameter_unit_view)?.text = unit.getUnit()

        itemHolder.tv(R.id.diameter_label_view)?.text = itemDiameterLabel
        itemHolder.tv(R.id.diameter_text_view)?.text =
            unit.convertPixelToValue(itemDiameter).unitDecimal()
        itemHolder.tv(R.id.perimeter_text_view)?.text =
            unit.convertPixelToValue(itemDiameter.toPerimeter()).unitDecimal()

        //
        bindPerimeter(itemHolder)
        bindDiameter(itemHolder)
    }

    /**周长, 自动输入成直径*/
    fun bindPerimeter(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.perimeter_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@CanvasDiameterItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                onNumberResultAction = { value ->
                    val x = valueUnit.convertValueToPixel(value)
                    itemDiameter = x.toDiameter()
                }
            }
        }
    }

    /**直径*/
    fun bindDiameter(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.diameter_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@CanvasDiameterItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                onNumberResultAction = { value ->
                    val x = valueUnit.convertValueToPixel(value)
                    itemDiameter = x
                }
            }
        }
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        itemChanging = true
        return false
    }

}