package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Path
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.canvasMenuPopupWindow
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.item.style.itemHaveNew
import com.angcyo.item.style.itemNewHawkKeyStr
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.ex._string

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/29
 */
class PathUnionMenuItem : CanvasIconItem() {

    companion object {

        /**所有元素是否都是path*/
        fun isAllShape(renderer: BaseRenderer): Boolean {
            val elementList = renderer.getSingleElementList()
            var isAllShape = true
            for (element in elementList) {
                val elementBean = element.lpElement()?.elementBean
                val mtype = elementBean?.mtype
                if (elementBean?.isPathElement == true ||
                    (mtype == LPDataConstant.DATA_TYPE_BITMAP &&
                            elementBean.imageFilter == LPDataConstant.DATA_MODE_GCODE)
                ) {
                    isAllShape = true
                } else {
                    isAllShape = false
                    break
                }
            }
            return isAllShape
        }
    }

    init {
        itemIco = R.drawable.canvas_path_union
        itemText = _string(R.string.canvas_union)
        itemNewHawkKeyStr = "union"
        itemClick = {
            itemHaveNew = false
            updateAdapterItem()

            it.context.canvasMenuPopupWindow(it) {
                renderAdapterAction = {
                    PathOpItem()() {
                        itemIco = R.drawable.canvas_path_union
                        itemText = _string(R.string.canvas_op_union)
                        this@PathUnionMenuItem.initSubItem(this)
                        itemOp = Path.Op.UNION
                    }
                    PathOpItem()() {
                        itemIco = R.drawable.canvas_path_difference
                        itemText = _string(R.string.canvas_op_difference)
                        this@PathUnionMenuItem.initSubItem(this)
                        itemOp = Path.Op.DIFFERENCE
                    }

                    PathOpItem()() {
                        itemIco = R.drawable.canvas_path_intersect
                        itemText = _string(R.string.canvas_op_intersect)
                        this@PathUnionMenuItem.initSubItem(this)
                        itemOp = Path.Op.INTERSECT
                    }
                    PathOpItem()() {
                        itemIco = R.drawable.canvas_path_xor
                        itemText = _string(R.string.canvas_op_xor)
                        this@PathUnionMenuItem.initSubItem(this)
                        itemOp = Path.Op.XOR
                    }
                }
            }
        }
    }

}