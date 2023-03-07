package com.angcyo.canvas2.laser.pecker.dslitem.control

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.library.ex._string

/**
 * 添加素材
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddMaterialItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_material_ico
        itemText = _string(R.string.canvas_material)
        itemEnable = true

        itemClick = {
            updateItemSelected(!itemIsSelected)
            /*engraveCanvasFragment.fragment.context.canvasMaterialWindow(it) {
                onDismiss = {
                    updateItemSelected(false)
                    false
                }
                onDrawableAction = { data, drawable ->
                    when (drawable) {
                        //bitmap
                        is BitmapDrawable -> itemCanvasDelegate?.addBlackWhiteBitmapRender(
                            drawable.bitmap
                        )
                        //gcode
                        is GCodeDrawable -> itemCanvasDelegate?.addGCodeRender(data as String)
                        //svg
                        is SharpDrawable -> itemCanvasDelegate?.addSvgRender(data as String)
                        //other
                        else -> {
                            itemCanvasDelegate?.addBlackWhiteBitmapRender(drawable.toBitmap())
                        }
                    }
                    UMEvent.CANVAS_MATERIAL.umengEventValue()
                }
            }*/
        }
    }
}