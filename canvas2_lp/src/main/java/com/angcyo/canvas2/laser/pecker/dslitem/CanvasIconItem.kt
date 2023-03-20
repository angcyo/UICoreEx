package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.doodle.ui.dslitem.DoodleIconItem
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.widget.DslViewHolder

/**
 * 上面ico, 下面文本
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
open class CanvasIconItem : DoodleIconItem(), ICanvasRendererItem {

    override var itemRenderer: BaseRenderer? = null

    override var itemRenderDelegate: CanvasRenderDelegate? = null

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

    open fun initSubItem(subItem: ICanvasRendererItem) {
        subItem.itemRenderer = itemRenderer
        subItem.itemRenderDelegate = itemRenderDelegate
    }
}
