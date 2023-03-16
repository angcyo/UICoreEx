package com.angcyo.canvas2.laser.pecker.element

import com.angcyo.canvas.render.element.PathElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.util.toPaintStyle
import com.angcyo.canvas2.laser.pecker.util.toPaintStyleInt

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/16
 */
class LPPathElement(override val elementBean: LPElementBean) : PathElement(), ILaserPeckerElement {

    override fun createStateStack(renderer: BaseRenderer): IStateStack = LPPathStateStack(renderer)

    override fun updateBeanToElement(renderer: BaseRenderer) {
        super.updateBeanToElement(renderer)
        paint.style = elementBean.paintStyle.toPaintStyle()
        updateOriginPathList(pathList)
    }

    override fun updateBeanFromElement(renderer: BaseRenderer) {
        super.updateBeanFromElement(renderer)
        elementBean.paintStyle = paint.style.toPaintStyleInt()
    }
}