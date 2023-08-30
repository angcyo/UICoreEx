package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.canvasMenuPopupWindow
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.ex._string
import com.angcyo.library.ex.size

/**
 * 元素分布菜单item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/20
 */
class RendererFlatMenuItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_flat_horizontal_svg
        itemText = _string(R.string.canvas_flat)

        itemClick = {
            it.context.canvasMenuPopupWindow(it) {
                val renderer = itemRenderer
                val enableSize = renderer is CanvasGroupRenderer &&
                        renderer.rendererList.size() >= 2//2个以上的元素才支持大小设置
                val enableFlat = renderer is CanvasGroupRenderer &&
                        renderer.rendererList.size() >= 3//3个以上的元素才支持分布
                renderAdapterAction = {
                    //--flat
                    RendererFlatItem()() {
                        itemIco = R.drawable.canvas_flat_horizontal_svg
                        itemText = _string(R.string.canvas_flat_horizontal)
                        this@RendererFlatMenuItem.initSubItem(this)
                        itemFlatType = CanvasGroupRenderer.FLAT_TYPE_HORIZONTAL

                        itemEnable = enableFlat
                    }
                    RendererFlatItem()() {
                        itemIco = R.drawable.canvas_flat_vertical_svg
                        itemText = _string(R.string.canvas_flat_vertical)
                        this@RendererFlatMenuItem.initSubItem(this)
                        itemFlatType = CanvasGroupRenderer.FLAT_TYPE_VERTICAL

                        itemEnable = enableFlat
                    }
                    //--size
                    if (HawkEngraveKeys.enableRendererSizeFlat) {
                        RendererSizeItem()() {
                            itemIco = R.drawable.canvas_size_width_svg
                            itemText = _string(R.string.canvas_size_width)
                            this@RendererFlatMenuItem.initSubItem(this)
                            itemSizeType = CanvasGroupRenderer.SIZE_TYPE_WIDTH

                            itemEnable = enableSize
                        }
                        RendererSizeItem()() {
                            itemIco = R.drawable.canvas_size_height_svg
                            itemText = _string(R.string.canvas_size_height)
                            this@RendererFlatMenuItem.initSubItem(this)
                            itemSizeType = CanvasGroupRenderer.SIZE_TYPE_HEIGHT

                            itemEnable = enableSize
                        }
                        RendererSizeItem()() {
                            itemIco = R.drawable.canvas_size_width_height_svg
                            itemText = _string(R.string.canvas_size_width_height)
                            this@RendererFlatMenuItem.initSubItem(this)
                            itemSizeType = CanvasGroupRenderer.SIZE_TYPE_WIDTH_HEIGHT

                            itemEnable = enableSize
                        }
                    }
                }
            }
        }
    }
}