package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Path
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.BitmapStateStack
import com.angcyo.canvas.render.util.element
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy

/**
 * 图片状态存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/09
 */
class LPBitmapStateStack : BitmapStateStack() {

    //region---需要存储的数据---

    var pathList: List<Path>? = null

    var imageOriginal: String? = null
    var src: String? = null

    var imageFilter = LPDataConstant.DATA_MODE_GREY
    var inverse = false
    var contrast = 0f
    var brightness = 0f
    var blackThreshold = HawkEngraveKeys.lastBWThreshold
    var printsThreshold = HawkEngraveKeys.lastPrintThreshold
    var sealThreshold = HawkEngraveKeys.lastSealThreshold

    var data: String? = null
    var gcodeDirection = 0
    var gcodeLineSpace = 5f
    var gcodeAngle = 0f
    var gcodeOutline = true
    var gcodeFillStep = 0f
    var gcodeFillAngle = 0f

    var minDiameter: Float? = null
    var meshShape: String? = null
    var maxDiameter: Float? = null
    var isMesh = false

    /**切片的数量*/
    var sliceCount: Int = 0

    /**2d浮雕强度[1~20]*/
    var reliefStrength: Int = 1

    //endregion---需要存储的数据---

    override fun saveState(renderer: BaseRenderer, delegate: CanvasRenderDelegate?) {
        super.saveState(renderer, delegate)
        pathList = renderer.element<LPBitmapElement>()?.pathList

        renderer.lpElementBean()?.let { elementBean ->
            imageOriginal = elementBean.imageOriginal
            src = elementBean.src

            imageFilter = elementBean.imageFilter
            inverse = elementBean.inverse
            contrast = elementBean.contrast
            brightness = elementBean.brightness
            blackThreshold = elementBean.blackThreshold
            printsThreshold = elementBean.printsThreshold
            sealThreshold = elementBean.sealThreshold

            data = elementBean.data
            gcodeDirection = elementBean.gcodeDirection
            gcodeLineSpace = elementBean.gcodeLineSpace
            gcodeAngle = elementBean.gcodeAngle
            gcodeOutline = elementBean.gcodeOutline
            gcodeFillStep = elementBean.gcodeFillStep
            gcodeFillAngle = elementBean.gcodeFillAngle

            minDiameter = elementBean.minDiameter
            meshShape = elementBean.meshShape
            maxDiameter = elementBean.maxDiameter
            isMesh = elementBean.isMesh

            sliceCount = elementBean.sliceCount
            reliefStrength = elementBean.reliefStrength
        }
    }

    override fun restoreState(
        renderer: BaseRenderer,
        reason: Reason,
        strategy: Strategy,
        delegate: CanvasRenderDelegate?
    ) {
        renderer.element<LPBitmapElement>()?.pathList = pathList
        renderer.lpElementBean()?.let { elementBean ->
            elementBean.imageOriginal = imageOriginal
            elementBean.src = src

            elementBean.imageFilter = imageFilter
            elementBean.inverse = inverse
            elementBean.contrast = contrast
            elementBean.brightness = brightness
            elementBean.blackThreshold = blackThreshold
            elementBean.printsThreshold = printsThreshold
            elementBean.sealThreshold = sealThreshold

            elementBean.data = data
            elementBean.gcodeDirection = gcodeDirection
            elementBean.gcodeLineSpace = gcodeLineSpace
            elementBean.gcodeAngle = gcodeAngle
            elementBean.gcodeOutline = gcodeOutline
            elementBean.gcodeFillStep = gcodeFillStep
            elementBean.gcodeFillAngle = gcodeFillAngle

            elementBean.minDiameter = minDiameter
            elementBean.maxDiameter = maxDiameter
            elementBean.meshShape = meshShape
            elementBean.isMesh = isMesh

            elementBean.sliceCount = sliceCount
            elementBean.reliefStrength = reliefStrength
        }

        super.restoreState(renderer, reason, strategy, delegate)
    }
}