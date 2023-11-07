package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.widget.TextView
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.mmToRenderUnitValue
import com.angcyo.dialog.TargetWindow
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.NumberKeyboardPopupConfig
import com.angcyo.item.keyboard.keyboardNumberWindow
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder
import kotlin.math.roundToInt

/**
 * 大的速度设置, 目前只支持速度设置
 *
 * 可以输入大值的item
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/11/07
 */
class EngraveBigValueItem : DslAdapterItem() {

    /**Label*/
    var itemLabel: CharSequence? = _string(R.string.engrave_speed)

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    /**单元素参数配置*/
    var itemEngraveItemBean: LPElementBean? = null

    /**限制值*/
    var itemMinValue = 0f
    var itemMaxValue = 0f

    /**单位*/
    private val itemUnit = LPConstant.renderUnit

    init {
        itemLayoutId = R.layout.item_engrave_big_value
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.value_label_view)?.text = itemLabel

        val unit = itemUnit
        itemHolder.tv(R.id.value_text_view)?.text = getValueString()
        itemHolder.tv(R.id.value_unit_view)?.text = "${unit.getUnit()}/s"

        //bind
        bindValue(itemHolder)
    }

    /**自动弹出键盘输入*/
    fun bindValue(itemHolder: DslViewHolder) {
        itemHolder.click(R.id.value_text_view) {
            itemHolder.context.keyboardNumberWindow(it) {
                onDismiss = this@EngraveBigValueItem::onPopupDismiss
                keyboardBindTextView = it as? TextView
                bindPendingDelay = -1 //关闭限流输入
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_DECIMAL)
                //removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_INCREMENT)
                removeKeyboardStyle(NumberKeyboardPopupConfig.STYLE_PLUS_MINUS)
                onNumberResultAction = { value ->
                    setValue(value)
                }
            }
        }
    }

    /**获取值的文本*/
    fun getValueString(): String {
        val value = itemEngraveConfigEntity?.bigSpeed ?: itemEngraveItemBean?.bigSpeed ?: 0
        return value.toFloat().mmToRenderUnitValue().roundToInt().toString()
    }

    /**更新值*/
    fun setValue(value: Float) {
        itemEngraveConfigEntity?.bigSpeed = clamp(value, itemMinValue, itemMaxValue).roundToInt()
        itemEngraveItemBean?.bigSpeed = itemEngraveConfigEntity?.bigSpeed
        itemEngraveConfigEntity?.lpSaveEntity()
    }

    /**popup销毁后, 刷新item*/
    fun onPopupDismiss(window: TargetWindow): Boolean {
        updateAdapterItem()
        itemChanging = true
        return false
    }

}