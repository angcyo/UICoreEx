package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.item.style.ITextItem
import com.angcyo.item.style.TextItemConfig
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.layout.DslCheckFlowLayout

/**
 * GCode扫描方向
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/16
 */
class CanvasDirectionItem : DslAdapterItem(), ITextItem {

    /**[0, 1, 2, 3]
     * [0°, 90°, 180°, 270°]*/
    var itemDirection: Int = 0

    /**回调*/
    var itemSelectChangedAction: (fromIndex: Int, selectIndexList: List<Int>, reselect: Boolean, fromUser: Boolean) -> Unit =
        { fromIndex, selectIndexList, reselect, fromUser ->

        }

    override var textItemConfig: TextItemConfig = TextItemConfig()

    init {
        itemLayoutId = R.layout.item_canvas_direction_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<DslCheckFlowLayout>(R.id.check_layout)?.apply {
            selectIndex(itemDirection)
            onSelectChanged { fromIndex, selectIndexList, reselect, fromUser ->
                if (fromUser) {
                    itemChanging = true
                }
                itemSelectChangedAction(fromIndex, selectIndexList, reselect, fromUser)
            }
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}