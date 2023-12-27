package com.angcyo.canvas2.laser.pecker.dialog.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
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
 * @since 2023/12/27
 */
class SpeedConvertItem : DslAdapterItem() {

    var itemLayerId: String? = null

    var itemDpi: Float = 254f

    var itemDepth = HawkEngraveKeys.lastDepth

    private var _resultDepth = itemDepth

    var onItemDepthChanged: (depth: Int) -> Unit = {}

    init {
        itemLayoutId = R.layout.item_speed_convert_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val oldSpeedList =
            SpeedInfo.getOldSpeedList(itemLayerId ?: LaserPeckerHelper.LAYER_LINE, itemDpi)
        val newSpeedList = SpeedInfo.getNewSpeedList(itemDpi)

        //旧值转成新的值
        val speedInfo = oldSpeedList.find { it.depth == itemDepth }
        val newSpeedInfo = SpeedInfo.findNearestSpeed(newSpeedList, speedInfo?.speed)
        _resultDepth = newSpeedInfo?.depth ?: _resultDepth
        onItemDepthChanged(_resultDepth)

        itemHolder.tv(R.id.old_speed_view)?.text = itemDepth.toStr()
        itemHolder.tv(R.id.new_speed_view)?.text = _resultDepth.toStr()

        itemHolder.tv(R.id.old_text_view)?.text = span {
            append(_string(R.string.engrave_speed))
            append(" ")
            append("${speedInfo?.speed?.decimal(fadedUp = true) ?: "--"}mm/s") {
                foregroundColor = _color(R.color.colorAccent)
            }
        }

        itemHolder.tv(R.id.new_text_view)?.text = span {
            append(_string(R.string.engrave_speed))
            append(" ")
            append("${newSpeedInfo?.speed?.decimal(fadedUp = true) ?: "--"}mm/s") {
                foregroundColor = _color(R.color.colorAccent)
            }
        }

        //click
        itemHolder.click(R.id.old_speed_view) {
            it.context.numberKeyboardDialog {
                numberValue = itemDepth
                numberMinValue = 1
                numberMaxValue = 100
                onNumberResultAction = {
                    it?.let {
                        itemDepth = it as Int
                        HawkEngraveKeys.lastDepth = itemDepth
                        itemChanging = true
                        updateAdapterItem()
                    }
                    false
                }
            }
        }
    }

}