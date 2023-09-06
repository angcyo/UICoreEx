package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dialog2.dslitem.IWheelItem
import com.angcyo.dialog2.dslitem.WheelItemConfig
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dialog2.dslitem.itemWheelText
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.DslIncrementItem
import com.angcyo.item.style.itemIncrementMinValue
import com.angcyo.item.style.itemIncrementValue
import com.angcyo.laserpacker.bean.LPVariableBean
import com.angcyo.library.ex._string
import com.angcyo.library.ex.toStr
import com.angcyo.widget.DslViewHolder

/**
 * 日期偏移item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/06
 */
class VarDateOffsetItem : DslIncrementItem(), IWheelItem {

    /**数据结构*/
    var itemVariableBean: LPVariableBean? = null
        set(value) {
            field = value
            updateDefaultDate()
        }

    override var wheelItemConfig: WheelItemConfig = WheelItemConfig()

    init {
        itemLayoutId = R.layout.date_offset_item
        //itemLabel = _string(R.string.variable_date_offset)
        itemWheelList = listOf(
            _string(R.string.variable_date_offset_day),
            _string(R.string.variable_date_offset_month),
            _string(R.string.variable_date_offset_yaer)
        )
        itemIncrementMinValue = 0
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.tv(R.id.lib_text_view)?.text = itemWheelText()
        itemHolder.click(R.id.lib_wrap_layout) {
            showItemWheelDialog(it.context)
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        super.onItemChangeListener(item)
        itemVariableBean?.value =
            itemIncrementValue?.toStr()?.toLongOrNull() ?: (itemVariableBean?.value ?: 1)
    }

    override fun onSelfWheelItemSelector(index: Int, item: Any) {
        super.onSelfWheelItemSelector(index, item)
        itemVariableBean?.stepType = when (index) {
            0 -> LPVariableBean.DATE_STEP_TYPE_DAY
            1 -> LPVariableBean.DATE_STEP_TYPE_MONTH
            2 -> LPVariableBean.DATE_STEP_TYPE_YEAR
            else -> LPVariableBean.DATE_STEP_TYPE_DAY
        }
    }

    private fun updateDefaultDate() {
        itemVariableBean?.let {
            //`D`:天  `M`:月 `Y`:年
            itemSelectedIndex = when (it.stepType) {
                LPVariableBean.DATE_STEP_TYPE_DAY -> 0
                LPVariableBean.DATE_STEP_TYPE_MONTH -> 1
                LPVariableBean.DATE_STEP_TYPE_YEAR -> 2
                else -> 0
            }
            itemIncrementValue = it.value.toStr()
        }
    }

}