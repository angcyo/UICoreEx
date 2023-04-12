package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.Paint
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.toPaintStyleInt
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
        val element = renderer.lpElement() ?: return null
        val bean = renderer.lpElementBean() ?: return null
        val result: TransferDataEntity? = when (bean._layerMode) {
            LPDataConstant.DATA_MODE_GCODE -> {
                //线条图层, 发送GCode数据
                if (bean.isLineShape || bean.paintStyle != Paint.Style.STROKE.toPaintStyleInt()) {
                    //虚线,实线,或者填充的图片, 都是GCode数据, 使用pixel转GCode
                    EngraveTransitionHelper.transitionToGCode(
                        element,
                        transferConfigEntity,
                        TransitionParam(
                            onlyUseBitmapToGCode = bean.isLineShape && bean.paintStyle == Paint.Style.STROKE.toPaintStyleInt(),
                            useOpenCvHandleGCode = false,
                            isSingleLine = bean.isLineShape
                        )
                    )
                } else {
                    //其他情况下, 转GCode优先使用Path, 再使用OpenCV
                    EngraveTransitionHelper.transitionToGCode(
                        element,
                        transferConfigEntity,
                        TransitionParam()
                    )
                }
            }
            LPDataConstant.DATA_MODE_BLACK_WHITE -> {
                //填充图层, 发送图片线段数据
                EngraveTransitionHelper.transitionToBitmapPath(
                    element,
                    transferConfigEntity
                )
            }
            LPDataConstant.DATA_MODE_DITHERING -> {
                //图片图层, 发送抖动线段数据
                EngraveTransitionHelper.transitionToBitmapDithering(
                    element,
                    transferConfigEntity,
                    TransitionParam(bean.inverse, bean.contrast, bean.brightness)
                )
            }
            LPDataConstant.DATA_MODE_GREY -> {
                //旧的图片图层, 发送图片数据
                EngraveTransitionHelper.transitionToBitmap(
                    element,
                    transferConfigEntity
                )
            }
            else -> {
                "无法处理的元素[${element.classHash()}]:${bean._layerMode}".writeErrorLog(logLevel = L.WARN)
                null
            }
        }

        //图层模式
        result?.apply {
            layerMode = bean._layerMode ?: -1
        }

        return result
    }

}