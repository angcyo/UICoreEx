package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.Paint
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.util.renderElement
import com.angcyo.canvas2.laser.pecker.element.LPBitmapElement
import com.angcyo.canvas2.laser.pecker.element.LPPathElement
import com.angcyo.canvas2.laser.pecker.element.LPTextElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.canvas2.laser.pecker.util.toPaintStyleInt
import com.angcyo.core.component.file.writeToLog
import com.angcyo.engrave2.EngraveConstant
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.library.L
import com.angcyo.library.ex.classHash
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity

/**
 * 业务相关的数据转换助手工具类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
object LPDataTransitionHelper {

    /**将[renderer]转换成对应的传输数据
     * [com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement]
     * [com.angcyo.engrave2.transition.IEngraveDataProvider]
     * */
    fun transitionRenderer(
        renderer: BaseRenderer?,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        renderer ?: return null
        val element = renderer.renderElement ?: return null
        val bean = renderer.lpElementBean() ?: return null
        var result: TransferDataEntity? = null

        if (element is LPPathElement) {//LPPathElement
            if (bean.isLineShape && bean.paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                //描边的线, 也就是虚线
                result = EngraveTransitionHelper.transitionToGCode(
                    element,
                    transferConfigEntity,
                    TransitionParam(useOpenCvHandleGCode = false, isSingleLine = true)
                )
            } else if (bean.paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                result = EngraveTransitionHelper.transitionToGCode(
                    element,
                    transferConfigEntity,
                    TransitionParam()
                )
            } else {
                //黑白数据
                result = EngraveTransitionHelper.transitionToBitmapPath(
                    element,
                    transferConfigEntity,
                )
            }
        } else if (element is LPBitmapElement) {//LPBitmapElement
            if (bean._layerMode == EngraveConstant.DATA_MODE_GCODE) {
                //GCode
                result = EngraveTransitionHelper.transitionToGCode(
                    element,
                    transferConfigEntity,
                    TransitionParam()
                )
            } else if (bean._layerMode == EngraveConstant.DATA_MODE_DITHERING) {
                //抖动
                result = EngraveTransitionHelper.transitionToBitmapDithering(
                    element,
                    transferConfigEntity,
                    TransitionParam(invert = bean.inverse)
                )
            } else if (bean._layerMode == EngraveConstant.DATA_MODE_GREY) {
                //灰度
                result = EngraveTransitionHelper.transitionToBitmap(
                    element,
                    transferConfigEntity
                )
            } else {
                //黑白线段
                result = EngraveTransitionHelper.transitionToBitmapPath(
                    element,
                    transferConfigEntity
                )
            }
        } else if (element is LPTextElement) {//LPTextElement
            if (bean.paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                //描边的文本走GCode
                result = EngraveTransitionHelper.transitionToGCode(
                    element,
                    transferConfigEntity,
                    TransitionParam()
                )
            } else {
                //否则就是黑白
                result = EngraveTransitionHelper.transitionToBitmapPath(
                    element,
                    transferConfigEntity
                )
            }
        } else {
            "无法处理的元素[${element.classHash()}]".writeToLog(logLevel = L.WARN)
        }
        return result
    }

}