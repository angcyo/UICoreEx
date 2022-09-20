package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.laser.pecker.R
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.button

/**
 * 按钮item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/20
 */
class CanvasButtonItem : DslAdapterItem() {

    var itemButtonText: CharSequence? = null

    init {
        itemLayoutId = R.layout.item_canvas_button_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.itemView.isClickable = false

        itemHolder.button(R.id.lib_button)?.apply {
            text = itemButtonText
            setOnClickListener(_clickListener)
            setOnLongClickListener(_longClickListener)
        }
    }

    override fun _initItemListener(itemHolder: DslViewHolder) {
        //去掉整体item的事件监听
        //super._initItemListener(itemHolder)
    }

}