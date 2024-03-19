package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.SwitchButton
import com.angcyo.item.DslSwitchInfoItem
import com.angcyo.library.ex._color
import com.angcyo.widget.DslViewHolder

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
open class CanvasSwitchItem : DslSwitchInfoItem() {

    init {
        //itemExtendLayoutId = R.layout.dsl_extent_min_width_switch_item
        itemLayoutId = R.layout.dsl_min_extent_width_info_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.v<SwitchButton>(R.id.lib_switch_view)?.apply {
            checkedColor = _color(R.color.canvas_primary)
        }
    }

    override fun onItemChangeListener(item: DslAdapterItem) {
        //super.onItemChangeListener(item)
    }

}