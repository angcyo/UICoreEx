package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.SpeedInfo
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.keyboard.numberKeyboardDialog
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.decimal
import com.angcyo.library.ex.toStr
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/12/18
 */
class SpeedConvertTestItem : DslAdapterItem() {

    /**速度信息*/
    var itemSpeedInfo: SpeedInfo? = null

    init {
        itemLayoutId = R.layout.app_speed_convert_test_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.speed_view)?.text = itemSpeedInfo?.depth.toStr()

        //查找对应的速度
        val speed = itemSpeedInfo?.speed
        itemHolder.tv(R.id.result_text_view)?.text = span {
            append(_string(R.string.engrave_speed))
            append(":")
            append("${speed?.decimal(fadedUp = true) ?: "--"}mm/s") {
                foregroundColor = _color(R.color.colorAccent)
            }
        }

        //click
        itemHolder.click(R.id.speed_view) {
            it.context.numberKeyboardDialog {
                numberValue = itemSpeedInfo?.depth ?: HawkEngraveKeys.lastDepth
                numberMinValue = 1
                numberMaxValue = 100
                onNumberResultAction = {
                    it?.let {
                        HawkEngraveKeys.lastDepth = it as Int
                        itemChanging = true
                    }
                    false
                }
            }
        }
    }
}