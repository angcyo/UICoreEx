package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.TextItemConfig
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.tab

/**
 * GCode方向设置
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class CanvasGCodeDirectionItem : DslAdapterItem(), ITextItem {

    /**[0, 1, 2, 3]
     * [0°, 90°, 180°, 270°]*/
    var itemDirection: Int = 0

    /**回调*/
    var itemSelectChangedAction: (fromIndex: Int, toIndex: Int, reselect: Boolean, fromUser: Boolean) -> Unit =
        { fromIndex, toIndex, reselect, fromUser ->

        }

    override var textItemConfig: TextItemConfig = TextItemConfig()

    init {
        itemLayoutId = R.layout.item_canvas_gcode_direction_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tab(R.id.lib_tab_layout)?.apply {
            setCurrentItem(itemDirection)
            observeIndexChange { fromIndex, toIndex, reselect, fromUser ->
                if (fromUser) {
                    itemChanging = true
                }
                itemSelectChangedAction(fromIndex, toIndex, reselect, fromUser)
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}