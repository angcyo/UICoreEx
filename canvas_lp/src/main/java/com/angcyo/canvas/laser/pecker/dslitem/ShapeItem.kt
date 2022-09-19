package com.angcyo.canvas.laser.pecker.dslitem

import android.graphics.Path
import com.angcyo.canvas.CanvasView
import com.angcyo.canvas.utils.addPictureDrawableRenderer
import com.angcyo.canvas.utils.addPictureShapeRenderer
import com.angcyo.library.ex._drawable

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/04/22
 */
class ShapeItem(val canvasView: CanvasView) : CanvasControlItem2() {

    var shapePath: Path? = null

    init {
        itemClick = {
            if (shapePath == null) {
                _drawable(itemIco)?.let {
                    canvasView.canvasDelegate.addPictureDrawableRenderer(it)
                }
            } else {
                shapePath?.let {
                    //canvasView.addShapeRenderer(it)
                    canvasView.canvasDelegate.addPictureShapeRenderer(it).apply {
                        itemLayerName = itemText
                    }
                }
            }
        }
    }

}