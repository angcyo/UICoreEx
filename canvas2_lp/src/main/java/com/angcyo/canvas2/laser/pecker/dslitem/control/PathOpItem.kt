package com.angcyo.canvas2.laser.pecker.dslitem.control

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.component.CanvasSelectorComponent
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.util.RenderHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.canvas2.laser.pecker.util.LPRendererHelper
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.dialog.popup.MenuPopupConfig
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.ex.op
import com.angcyo.library.ex.toListOf
import com.angcyo.toSVGStrokeContentStr

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/29
 */
class PathOpItem : CanvasIconItem() {

    companion object {
        /**合并元素*/
        fun opElement(
            op: Path.Op?,
            originBounds: RectF?, //用来定位中点的bounds
            rendererList: List<BaseRenderer>,
            delegate: CanvasRenderDelegate?
        ) {
            delegate ?: return
            val pathList = mutableListOf<Path>()
            val list = delegate.getSingleElementRendererListIn(rendererList)
            var haveFillElement = false
            for (renderer in list) {
                val element = renderer.lpElement()
                val renderProperty = renderer.renderProperty
                if (element is LPPathElement) {
                    haveFillElement = haveFillElement ||
                            element.paint.style == Paint.Style.FILL ||
                            element.paint.style == Paint.Style.FILL_AND_STROKE
                    RenderHelper.translateToRender(element.getDrawPathList(), renderProperty)?.let {
                        pathList.addAll(it)
                    }
                } else if (element is LPBitmapElement) {
                    haveFillElement = haveFillElement ||
                            element.paint.style == Paint.Style.FILL ||
                            element.paint.style == Paint.Style.FILL_AND_STROKE
                    RenderHelper.translateToRender(element.getDrawPathList(), renderProperty)?.let {
                        pathList.addAll(it)
                    }
                }
            }
            if (pathList.isNotEmpty()) {
                val result = pathList.op(op)
                //val svgContent = result.toSvgPathContent()
                val svgContent = if (result.isEmpty) {
                    null
                } else {
                    result.toSVGStrokeContentStr {
                        it.isSinglePath = true
                        it.needClosePath = false //不需要闭合
                    }
                }

                val elementBean = LPElementBean().apply {
                    mtype = LPDataConstant.DATA_TYPE_SVG
                    this.path = svgContent
                    paintStyle =
                        if ((HawkEngraveKeys.enableXorFill && op == Path.Op.XOR) /*xor操作时, 默认填充处理*/ ||
                            (op == null && haveFillElement) /*合并path时, 检查是否填充处理*/) {
                            Paint.Style.FILL.toPaintStyleInt()
                        } else {
                            Paint.Style.STROKE.toPaintStyleInt()
                        }
                }
                LPRendererHelper.parseElementRenderer(elementBean, false)?.apply {
                    originBounds?.let {
                        translateCenterTo(
                            it.centerX(),
                            it.centerY(),
                            Reason.code,
                            Strategy.preview,
                            null
                        )
                    }
                    delegate.renderManager.replaceElementRenderer(
                        rendererList,
                        listOf(this),
                        true,
                        Reason.user,
                        Strategy.normal
                    )
                }
            }
        }
    }

    /**op*/
    var itemOp: Path.Op? = Path.Op.UNION

    init {
        itemLayoutId = R.layout.item_canvas_icon_horizontal_layout

        itemClick = {
            itemRenderer?.let {
                val rendererList = when (it) {
                    is CanvasSelectorComponent -> it.rendererList
                    is CanvasGroupRenderer -> it.toListOf()
                    else -> null
                }
                if (!rendererList.isNullOrEmpty()) {
                    opElement(itemOp, it.getRendererBounds(), rendererList, itemRenderDelegate)
                }
            }
        }

        //点击后自动关闭pop
        itemFlag = MenuPopupConfig.FLAG_ITEM_DISMISS
    }
}