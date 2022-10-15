package com.angcyo.engrave.dslitem.engrave

import android.graphics.Typeface
import com.angcyo.dialog2.WheelDialogConfig
import com.angcyo.dialog2.wheelDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveHelper.findOptionIndex
import com.angcyo.engrave.EngraveHelper.percentList
import com.angcyo.engrave.R
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * 雕刻属性item, 包含功率/深度/次数
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/15
 */
class EngravePropertyItem : DslAdapterItem() {

    /**参数配置实体*/
    var itemEngraveConfigEntity: EngraveConfigEntity? = null

    init {
        itemLayoutId = R.layout.item_engrave_property_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        val context = itemHolder.context

        //属性
        val powerLabel = _string(R.string.custom_power)
        val power = itemEngraveConfigEntity?.power ?: 100
        itemHolder.tv(R.id.power_view)?.text = span {
            append(powerLabel)
            appendln()
            append("$power") {
                fontSize = 40 * dpi
                style = Typeface.BOLD
                foregroundColor = _color(R.color.colorAccent)
            }
            append("%")
        }
        val speedLabel = _string(R.string.custom_speed)
        val depth = itemEngraveConfigEntity?.depth ?: 3
        itemHolder.tv(R.id.speed_view)?.text = span {
            append(speedLabel)
            appendln()
            append("$depth") {
                fontSize = 40 * dpi
                style = Typeface.BOLD
                foregroundColor = _color(R.color.colorAccent)
            }
            append("%")

        }
        val timesLabel = _string(R.string.print_times)
        val time = itemEngraveConfigEntity?.time ?: 1
        itemHolder.tv(R.id.tims_view)?.text = span {
            append(timesLabel)
            appendln()
            append("$time") {
                fontSize = 40 * dpi
                style = Typeface.BOLD
                foregroundColor = _color(R.color.colorAccent)
            }
        }

        //事件
        itemHolder.click(R.id.power_view) {
            context.wheelDialog {
                dialogTitle = powerLabel
                wheelItems = percentList()
                wheelSelectedIndex = findOptionIndex(wheelItems, power)
                wheelUnit = "%"

                wheelItemSelectorAction = { dialog, index, item ->
                    itemEngraveConfigEntity?.power = getSelectedInt(index, power)
                    itemChanging = true
                    false
                }
            }
        }
        itemHolder.click(R.id.speed_view) {
            context.wheelDialog {
                dialogTitle = speedLabel
                wheelItems = percentList()
                wheelSelectedIndex = findOptionIndex(wheelItems, depth)
                wheelUnit = "%"

                wheelItemSelectorAction = { dialog, index, item ->
                    itemEngraveConfigEntity?.depth = getSelectedInt(index, depth)
                    itemChanging = true
                    false
                }
            }
        }
        itemHolder.click(R.id.tims_view) {
            context.wheelDialog {
                dialogTitle = timesLabel
                wheelItems = percentList(255)
                wheelSelectedIndex = findOptionIndex(wheelItems, time)

                wheelItemSelectorAction = { dialog, index, item ->
                    itemEngraveConfigEntity?.time = getSelectedInt(index, time)
                    itemChanging = true
                    false
                }
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        itemEngraveConfigEntity?.lpSaveEntity()
        super.onItemChangeListener(item)
    }

    /**获取选中的byte数据*/
    fun WheelDialogConfig.getSelectedInt(index: Int, def: Int): Int =
        wheelItems?.get(index)?.toString()?.toIntOrNull() ?: def
}
