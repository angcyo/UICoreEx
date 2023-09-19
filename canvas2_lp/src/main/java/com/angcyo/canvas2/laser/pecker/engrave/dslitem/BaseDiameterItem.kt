package com.angcyo.canvas2.laser.pecker.engrave.dslitem

import android.widget.TextView
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder

/**
 * 周长/直径 输入item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/21
 */
abstract class BaseDiameterItem : DslAdapterItem() {

    companion object {

        /**直径转周长*/
        fun Float.toPerimeter(): Float = (Math.PI * this).toFloat()

        /**周长转直径*/
        fun Float.toDiameter(): Float = (this / Math.PI).toFloat()

        /**直径转周长*/
        fun Double.toPerimeter(): Double = Math.PI * this

        /**周长转直径*/
        fun Double.toDiameter(): Double = this / Math.PI
    }

    /**直径的label*/
    var itemDiameterLabel: CharSequence? = null

    /**直径, 像素单位*/
    @Pixel
    var itemDiameter: Float = 0f

    init {
        itemLayoutId = R.layout.item_engrave_data_diameter
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val unit = LPConstant.renderUnit
        itemHolder.tv(R.id.perimeter_unit_view)?.text = unit.getUnit()
        itemHolder.tv(R.id.diameter_unit_view)?.text = unit.getUnit()

        itemHolder.tv(R.id.diameter_label_view)?.text = itemDiameterLabel
        itemHolder.tv(R.id.diameter_text_view)?.text = unit.convertPixelToValue(itemDiameter)
            .unitDecimal(HawkEngraveKeys.diameterPrecision, ensureInt = false)
        itemHolder.tv(R.id.perimeter_text_view)?.text =
            unit.convertPixelToValue(itemDiameter.toPerimeter())
                .unitDecimal(HawkEngraveKeys.diameterPrecision, ensureInt = false)

        //
        bindPerimeter(itemHolder)
        bindDiameter(itemHolder)
    }

    /**周长, 自动输入成直径*/
    fun bindPerimeter(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.perimeter_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@BaseDiameterItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                onNumberResultAction = { value ->
                    val x = IValueUnit.MM_RENDER_UNIT.convertValueToPixel(value)
                    itemDiameter = x.toDiameter()
                }
            }
        }
    }

    /**直径*/
    fun bindDiameter(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.diameter_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@BaseDiameterItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                onNumberResultAction = { value ->
                    val x = IValueUnit.MM_RENDER_UNIT.convertValueToPixel(value)
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