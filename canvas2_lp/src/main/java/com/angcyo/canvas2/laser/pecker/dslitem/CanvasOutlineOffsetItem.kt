package com.angcyo.canvas2.laser.pecker.dslitem

import android.widget.TextView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslSeekBarInfoItem
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.item.style.itemInfoText
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.library.unit.InchValueUnit
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.unitDecimal
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.progress.DslSeekBar

/**
 * 外线偏移距离
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/26
 */
open class CanvasOutlineOffsetItem : DslSeekBarInfoItem() {

    /**值改变的通知*/
    var itemValueChangeAction: (value: Float) -> Unit = {}

    /**当前的值*/
    var itemValue: Float = 0f

    protected val inchUnit = InchValueUnit()

    protected var maxValue = inchUnit.convertValueToPixel(1f).toMm()
    protected var minValue = -maxValue

    init {
        itemLayoutId = R.layout.item_outline_offset_layout
        itemInfoText = _string(R.string.canvas_outline_offset_tip)
        itemProgressTextFormatAction = {
            formatValue(itemValue)
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<DslSeekBar>(R.id.lib_seek_view)?.apply {
            val color = _color(R.color.canvas_primary)
            setBgGradientColors("${_color(R.color.canvas_line)}")
            setTrackGradientColors("$color")
            updateThumbColor(color)

            showCenterProgressTip = true
            progressMaxValue = maxValue
            progressMinValue = minValue
            progressValue = itemValue
        }

        bindValue(itemHolder)
    }

    override fun onItemSeekChanged(value: Float, fraction: Float, fromUser: Boolean) {
        super.onItemSeekChanged(value, fraction, fromUser)
        if (fromUser) {
            updateValue(value)
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

    open fun bindValue(itemHolder: DslViewHolder) {
        itemHolder.tv(R.id.value_text_view)?.text = formatValue(itemValue)
        itemHolder.click(R.id.value_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@CanvasOutlineOffsetItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                onNumberResultAction = { toValue ->
                    updateValue(toValue)
                }
            }
        }
    }

    open fun formatValue(value: Float): String {
        return value.unitDecimal(1)
    }

    /**更新值*/
    open fun updateValue(value: Float) {
        itemValue = clamp(value, minValue, maxValue)
        itemValueChangeAction(itemValue)
        updateAdapterItem()
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        return false
    }

}