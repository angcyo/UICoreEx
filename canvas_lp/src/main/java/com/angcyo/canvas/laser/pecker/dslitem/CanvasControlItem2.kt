package com.angcyo.canvas.laser.pecker.dslitem

import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.laser.pecker.R
import com.angcyo.doodle.ui.dslitem.DoodleIconItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 新版操作界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/15
 */
open class CanvasControlItem2 : DoodleIconItem() {

    var itemRenderer: BaseItemRenderer<*>? = null

    var itemCanvasDelegate: CanvasDelegate? = null

    init {
        //itemLayoutId = R.layout.item_doodle_icon_layout //图文上下结构
        //itemLayoutId = R.layout.item_canvas_icon_horizontal_layout //图文左右结构
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //如果有[R.id.lib_check_view]控制, 则...
        itemHolder.visible(R.id.lib_check_view, itemIsSelected)
    }

}