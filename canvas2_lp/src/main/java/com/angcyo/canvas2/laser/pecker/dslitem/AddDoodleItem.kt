package com.angcyo.canvas2.laser.pecker.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.drawCanvasRight
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.library.ex._string

/**
 * 添加涂鸦
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */
class AddDoodleItem : CanvasIconItem() {

    init {
        itemIco = R.drawable.canvas_doodle_ico
        itemText = _string(R.string.canvas_doodle)
        itemEnable = true
        itemClick = {
            /*UMEvent.CANVAS_DOODLE.umengEventValue()
            engraveCanvasFragment.fragment.context.doodleDialog {
                onDoodleResultAction = {
                    engraveCanvasFragment.fragment.engraveLoadingAsync({
                        //涂鸦之后, 默认黑白处理
                        val bean = it.toBlackWhiteBitmapItemData()
                        GraphicsHelper.addRenderItemDataBean(canvasDelegate, bean)
                    })
                }
            }*/
        }
        drawCanvasRight()
    }

}