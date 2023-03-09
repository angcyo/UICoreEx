package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.invertHelpDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 反色开关, 以及反色说明
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class CanvasInvertSwitchItem : CanvasSwitchItem() {

    init {
        itemLayoutId = R.layout.item_canvas_invert_switch_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //反色提示
        itemHolder.click(R.id.invert_help_view) {
            it.context.invertHelpDialog()
        }
    }

}