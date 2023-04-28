package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas.render.core.CanvasRenderManager
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.canvasMenuPopupWindow
import com.angcyo.canvas2.laser.pecker.drawCanvasRight
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.dslitem.ICanvasRendererItem
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.ex._string

/**
 * 图层排序item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
class LayerSortMenuItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_layer_sort
        itemText = _string(R.string.canvas_sort)
        itemEnable = true
        drawCanvasRight()
        itemClick = {
            it.context.canvasMenuPopupWindow(it) {
                renderAdapterAction = {
                    LayerArrangeItem()() {
                        itemArrange = CanvasRenderManager.ARRANGE_FORWARD
                        this@LayerSortMenuItem.initSubItem(this)
                    }
                    LayerArrangeItem()() {
                        itemArrange = CanvasRenderManager.ARRANGE_BACKWARD
                        this@LayerSortMenuItem.initSubItem(this)
                    }
                    LayerArrangeItem()() {
                        itemArrange = CanvasRenderManager.ARRANGE_FRONT
                        this@LayerSortMenuItem.initSubItem(this)
                        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
                    }
                    LayerArrangeItem()() {
                        itemArrange = CanvasRenderManager.ARRANGE_BACK
                        this@LayerSortMenuItem.initSubItem(this)
                        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
                    }
                }
            }
        }
    }

    override fun initSubItem(subItem: ICanvasRendererItem) {
        super.initSubItem(subItem)
        if (subItem is DslAdapterItem) {
            //subItem.itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
        }
    }
}