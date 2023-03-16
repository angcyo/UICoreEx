package com.angcyo.canvas2.laser.pecker.element

import com.angcyo.canvas.render.element.PathElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.toPaintStyle
import com.angcyo.canvas2.laser.pecker.util.toPaintStyleInt
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.utils.isSvgContent
import com.angcyo.svg.Svg
import com.pixplicity.sharp.Sharp

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/16
 */
class LPPathElement(override val elementBean: LPElementBean) : PathElement(), ILaserPeckerElement {

    override fun createStateStack(renderer: BaseRenderer): IStateStack = LPPathStateStack(renderer)

    override fun updateBeanToElement(renderer: BaseRenderer) {
        super.updateBeanToElement(renderer)
        paint.style = elementBean.paintStyle.toPaintStyle()
        if (pathList == null) {
            val data = elementBean.data
            if (!data.isNullOrEmpty()) {
                when (elementBean.mtype) {
                    LPConstant.DATA_TYPE_GCODE -> {
                        val gCodeDrawable = GCodeHelper.parseGCode(data, paint)
                        if (gCodeDrawable != null) {
                            pathList = listOf(gCodeDrawable.gCodePath)
                        }
                    }
                    LPConstant.DATA_TYPE_SVG -> {
                        if (data.isSvgContent()) {
                            //svg标签数据
                            val sharpDrawable = Svg.loadSvgPathDrawable(data, -1, null, paint, 0, 0)
                            if (sharpDrawable != null) {
                                //
                                pathList = sharpDrawable.pathList
                            }
                        } else {
                            //svg纯路径数据
                            val path = Sharp.loadPath(data)
                            pathList = listOf(path)
                        }
                    }
                }
            }
        }
        updateOriginPathList(pathList)
    }

    override fun updateBeanFromElement(renderer: BaseRenderer) {
        super.updateBeanFromElement(renderer)
        elementBean.paintStyle = paint.style.toPaintStyleInt()
    }
}