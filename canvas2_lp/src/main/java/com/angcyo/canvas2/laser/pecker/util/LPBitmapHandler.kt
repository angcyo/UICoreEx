package com.angcyo.canvas2.laser.pecker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.PathRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig.Companion.APPLY_TYPE_CHANGE
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig.Companion.APPLY_TYPE_DISMISS
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig.Companion.APPLY_TYPE_SUBMIT
import com.angcyo.canvas2.laser.pecker.dialog.canvasRegulateWindow
import com.angcyo.canvas2.laser.pecker.dialog.magicWandDialog
import com.angcyo.canvas2.laser.pecker.element.LPBitmapStateStack
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.crop.ui.cropDialog
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper._defaultGCodeOutputFile
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.laserpacker.toGCodePath
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.LTime
import com.angcyo.library.component.Strategy
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.pool.acquireTempMatrix
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.addBgColor
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.writeToFile
import com.angcyo.opencv.OpenCV
import com.angcyo.rust.handle.RustBitmapHandle
import com.hingin.rn.image.ImageProcess
import java.io.File

/**
 * 图片编辑处理, 实时改变, 不需要确定按钮, 模式恢复
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/08
 */
object LPBitmapHandler {

    //region---图片算法处理---

    /** [com.angcyo.bitmap.handle.BitmapHandle.toBlackWhiteHandle]*/
    fun toBlackWhiteHandle(bitmap: Bitmap, bean: LPElementBean): Bitmap? {
        return toBlackWhiteHandle(bitmap, bean.blackThreshold, bean.inverse)
    }

    /**黑白画处理*/
    fun toBlackWhiteHandle(bitmap: Bitmap, blackThreshold: Float, inverse: Boolean): Bitmap? {
        return BitmapHandle.toBlackWhiteHandle(
            bitmap,
            blackThreshold.toInt(),
            inverse,
            alphaBgColor = if (HawkEngraveKeys.enableRemoveBWAlpha) if (inverse) Color.BLACK else Color.WHITE else Color.TRANSPARENT,
            alphaThreshold = LibHawkKeys.alphaThreshold,
            whiteReplaceColor = if (HawkEngraveKeys.enableBitmapHandleBgAlpha) Color.TRANSPARENT else Color.WHITE
        )
    }

    /**灰度处理*/
    fun toGrayHandle(bitmap: Bitmap, bean: LPElementBean): Bitmap? {
        return toGrayHandle(bitmap, bean.inverse, bean.contrast, bean.brightness)
    }

    /**灰度处理*/
    fun toGrayHandle(
        bitmap: Bitmap,
        invert: Boolean = false,
        contrast: Float = 0f,
        brightness: Float = 0f,
    ): Bitmap? {
        return BitmapHandle.toGrayHandle(
            bitmap,
            invert,
            contrast,
            brightness,
            Color.WHITE,
            LibHawkKeys.bgAlphaThreshold
        )
    }

    /**版画处理*/
    fun toPrint(context: Context, bitmap: Bitmap, printsThreshold: Float): Bitmap? {
        val result = RustBitmapHandle.bitmapPrint(
            bitmap,
            printsThreshold.toInt(),
            LibHawkKeys.alphaThreshold,
            false,
            whiteReplaceColor = if (HawkEngraveKeys.enableBitmapHandleBgAlpha) Color.TRANSPARENT else Color.WHITE
        )
        return result
        /*val grayHandle = toGrayHandle(bitmap)!!
        val print = OpenCV.bitmapToPrint(context, grayHandle, printsThreshold.toInt())
        grayHandle.recycle()
        return if (print != null) {
            val result = BitmapHandle.replaceColors(
                print,
                0,
                intArrayOf(Color.WHITE),
                if (HawkEngraveKeys.enableBitmapHandleBgAlpha) Color.TRANSPARENT else Color.WHITE
            )
            print.recycle()
            result
        } else {
            null
        }*/
    }

    /**印章处理*/
    @Deprecated("使用[toSealHandle]")
    fun toSeal(context: Context, bitmap: Bitmap, sealThreshold: Float): Bitmap? {
        //先黑白画?还是后黑白画?
        val bgBitmap = bitmap.addBgColor(Color.WHITE)
        return OpenCV.bitmapToSeal(context, bgBitmap, sealThreshold.toInt())
    }

    /**印章处理*/
    fun toSealHandle(bitmap: Bitmap, sealThreshold: Float): Bitmap? {
        //先黑白画?还是后黑白画?
        return BitmapHandle.toSealHandle(
            bitmap,
            sealThreshold.toInt(),
            Color.BLACK,
            LibHawkKeys.alphaThreshold,
            if (HawkEngraveKeys.enableBitmapHandleBgAlpha) 0x00 else 0xff
        )
    }

    /**转GCode处理*/
    fun toGCode(context: Context, bitmap: Bitmap, bean: LPElementBean): File {
        return OpenCV.bitmapToGCode(
            context,
            bitmap,
            (1 / 1f.toPixel()).toDouble(),
            lineSpace = bean.gcodeLineSpace.toDouble(),
            direction = bean.gcodeDirection,
            angle = bean.gcodeAngle.toDouble(),
            type = if (bean.gcodeOutline) 1 else 3
        )
    }

    //endregion---图片算法处理---

    //region---core---

    private fun addBitmapStateToStack(
        delegate: CanvasRenderDelegate?,
        renderer: BaseRenderer,
        undoState: IStateStack
    ) {
        delegate?.undoManager?.addToStack(
            renderer,
            undoState,
            LPBitmapStateStack().apply { saveState(renderer, delegate) },
            false,
            Reason.user.apply {
                controlType = BaseControlPoint.CONTROL_TYPE_DATA
            },
            Strategy.normal
        )
    }

    //endregion---core---

    //region---带参数调整对话框---

    /**版画*/
    fun handlePrint(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD,
                bean.printsThreshold.toInt()
            )
            firstApply = bean.imageFilter != LPDataConstant.DATA_MODE_PRINT
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                    if (_valueChange) {
                        addBitmapStateToStack(delegate, renderer, undoState)
                    }
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            bean.printsThreshold = getIntOrDef(
                                CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD,
                                bean.printsThreshold.toInt()
                            ).toFloat()
                            bean.imageFilter = LPDataConstant.DATA_MODE_PRINT
                            LTime.tick()
                            val result = toPrint(context, bitmap, bean.printsThreshold)
                            element.updateOriginWidthHeight(
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat(),
                                false
                            )
                            "图片[${bitmap.byteCount.toSizeString()}]转版画耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**GCode*/
    fun handleGCode(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_OUTLINE, bean.gcodeOutline)
            addRegulate(CanvasRegulatePopupConfig.KEY_LINE_SPACE, bean.gcodeLineSpace)
            addRegulate(CanvasRegulatePopupConfig.KEY_ANGLE, bean.gcodeAngle)
            //addRegulate(CanvasRegulatePopupConfig.KEY_DIRECTION, bean.gcodeDirection)//2023-3-10
            addRegulate(CanvasRegulatePopupConfig.KEY_SUBMIT)
            //firstApply = bean.imageFilter != LPConstant.DATA_MODE_GCODE
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({

                        //direction
                        bean.gcodeDirection = 0

                        //lineSpace
                        val lineSpace = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_LINE_SPACE,
                            bean.gcodeLineSpace
                        )
                        bean.gcodeLineSpace = lineSpace

                        //angle
                        val gcodeAngle = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_ANGLE,
                            bean.gcodeAngle
                        )
                        bean.gcodeAngle = gcodeAngle

                        //outline
                        val gcodeOutline = getBooleanOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE,
                            bean.gcodeOutline
                        )
                        bean.gcodeOutline = gcodeOutline
                        operateBitmap.let { bitmap ->
                            LTime.tick()
                            val gcodeFile = toGCode(context, bitmap, bean)
                            "图片[${
                                bitmap.byteCount.toSizeString()
                            }]转GCode耗时:${LTime.time()}".writePerfLog()
                            val gCodeText = gcodeFile.readText()
                            gcodeFile.deleteSafe()
                            LTime.tick()
                            //val result = gCodeText to GCodeHelper.parseGCode(gCodeText)
                            val path = gCodeText.toGCodePath()
                            val result = gCodeText to path
                            val outputFile = _defaultGCodeOutputFile()
                            gCodeText.writeToFile(outputFile)
                            "解析GCode数据[${gCodeText.length.toSizeString()}]耗时:${LTime.time()} ${outputFile.absolutePath}".writePerfLog()

                            element.updateOriginBitmapGCode(
                                result.second?.run { listOf(this) },
                                result.first,
                                false
                            )
                            //stack
                            addBitmapStateToStack(delegate, renderer, undoState)

                            result
                        }
                    }) {
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**黑白画*/
    fun handleBlackWhite(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_BW_INVERT,
                bean.inverse
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_BW_THRESHOLD,
                bean.blackThreshold.toInt()
            )
            firstApply = bean.imageFilter != LPDataConstant.DATA_MODE_BLACK_WHITE
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                    if (_valueChange) {
                        addBitmapStateToStack(delegate, renderer, undoState)
                    }
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            bean.blackThreshold = getIntOrDef(
                                CanvasRegulatePopupConfig.KEY_BW_THRESHOLD,
                                bean.blackThreshold.toInt()
                            ).toFloat()
                            bean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig.KEY_BW_INVERT, bean.inverse
                            )
                            bean.imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE
                            LTime.tick()
                            val result = toBlackWhiteHandle(bitmap, bean)
                            element.updateOriginWidthHeight(
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat(),
                                false
                            )
                            "图片[${bitmap.byteCount.toSizeString()}]转黑白耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**抖动
     * 2022-12-12 抖动算法, 显示的是灰度图, 只在数据发送的时候使用抖动算法处理, 所以~~~
     * [com.angcyo.canvas.graphics.BitmapGraphicsParser]
     * */
    fun handleDithering(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT, bean.inverse)
            addRegulate(CanvasRegulatePopupConfig.KEY_CONTRAST, bean.contrast)
            addRegulate(CanvasRegulatePopupConfig.KEY_BRIGHTNESS, bean.brightness)
            firstApply = bean.imageFilter != LPDataConstant.DATA_MODE_DITHERING
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                    if (_valueChange) {
                        addBitmapStateToStack(delegate, renderer, undoState)
                    }
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            bean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig.KEY_SHAKE_INVERT,
                                bean.inverse
                            )
                            bean.contrast = getFloatOrDef(
                                CanvasRegulatePopupConfig.KEY_CONTRAST,
                                bean.contrast
                            )
                            bean.brightness = getFloatOrDef(
                                CanvasRegulatePopupConfig.KEY_BRIGHTNESS,
                                bean.brightness
                            )
                            bean.imageFilter = LPDataConstant.DATA_MODE_DITHERING
                            LTime.tick()
                            val result = toGrayHandle(bitmap, bean)
                            element.updateOriginWidthHeight(
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat(),
                                false
                            )
                            "图片[${bitmap.byteCount.toSizeString()}]转灰度耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**灰度*/
    fun handleGrey(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT, bean.inverse)
            addRegulate(CanvasRegulatePopupConfig.KEY_CONTRAST, bean.contrast)
            addRegulate(CanvasRegulatePopupConfig.KEY_BRIGHTNESS, bean.brightness)
            firstApply = bean.imageFilter != LPDataConstant.DATA_MODE_GREY
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                    if (_valueChange) {
                        addBitmapStateToStack(delegate, renderer, undoState)
                    }
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            bean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig.KEY_SHAKE_INVERT,
                                bean.inverse
                            )
                            bean.contrast = getFloatOrDef(
                                CanvasRegulatePopupConfig.KEY_CONTRAST,
                                bean.contrast
                            )
                            bean.brightness = getFloatOrDef(
                                CanvasRegulatePopupConfig.KEY_BRIGHTNESS,
                                bean.brightness
                            )
                            bean.imageFilter = LPDataConstant.DATA_MODE_GREY
                            LTime.tick()
                            val result = toGrayHandle(bitmap, bean)
                            element.updateOriginWidthHeight(
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat(),
                                false
                            )
                            "图片[${bitmap.byteCount.toSizeString()}]转灰度耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**印章*/
    fun handleSeal(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD,
                bean.sealThreshold.toInt()
            )
            firstApply = bean.imageFilter != LPDataConstant.DATA_MODE_SEAL
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                    if (_valueChange) {
                        addBitmapStateToStack(delegate, renderer, undoState)
                    }
                } else {
                    owner.engraveLoadingAsync({
                        val threshold = getIntOrDef(
                            CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD,
                            bean.sealThreshold.toInt()
                        )
                        bean.sealThreshold = threshold.toFloat()
                        operateBitmap.let { bitmap ->
                            bean.imageFilter = LPDataConstant.DATA_MODE_SEAL
                            LTime.tick()
                            val result = toSealHandle(bitmap, bean.sealThreshold)
                            element.updateOriginWidthHeight(
                                bitmap.width.toFloat(),
                                bitmap.height.toFloat(),
                                false
                            )
                            "图片[${bitmap.byteCount.toSizeString()}]转印章耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**图片扭曲*/
    fun handleMesh(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean
        val context = anchor.context

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_MESH_SHAPE,
                bean.meshShape ?: CanvasRegulatePopupConfig.DEFAULT_MESH_SHAPE
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_MIN_DIAMETER,
                bean.minDiameter ?: HawkEngraveKeys.lastMinDiameterPixel
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_MAX_DIAMETER,
                bean.maxDiameter ?: HawkEngraveKeys.lastDiameterPixel
            )
            addRegulate(CanvasRegulatePopupConfig.KEY_SUBMIT)

            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    val minDiameter = getFloatOrDef(
                        CanvasRegulatePopupConfig.KEY_MIN_DIAMETER,
                        HawkEngraveKeys.lastMinDiameterPixel
                    )
                    bean.minDiameter = minDiameter
                    val maxDiameter = getFloatOrDef(
                        CanvasRegulatePopupConfig.KEY_MAX_DIAMETER,
                        HawkEngraveKeys.lastDiameterPixel
                    )
                    bean.maxDiameter = maxDiameter
                    // "CONE" 圆锥
                    // "BALL" 球体
                    val meshShape = getStringOrDef(
                        CanvasRegulatePopupConfig.KEY_MESH_SHAPE,
                        CanvasRegulatePopupConfig.DEFAULT_MESH_SHAPE
                    )
                    bean.meshShape = meshShape
                    bean.isMesh = true //提前赋值
                    owner.engraveLoadingAsync({
                        LTime.tick()
                        val result = ImageProcess.imageMesh(
                            operateBitmap,
                            minDiameter,
                            maxDiameter,
                            meshShape
                        )
                        "图片[${operateBitmap.byteCount.toSizeString()}]扭曲耗时:${LTime.time()}".writePerfLog()

                        result?.let {
                            element.updateOriginBitmap(result, false)
                        }
                        addBitmapStateToStack(delegate, renderer, undoState)

                        result
                    }) {
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**路径填充*/
    fun handlePathFill(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpPathElement() ?: return
        val operatePathList = element.pathList ?: return
        val bean = element.elementBean
        val context = anchor.context

        context.canvasRegulateWindow(anchor) {
            val gcodeFillStepPixel = bean.gcodeFillStep
            val fillAngle = bean.gcodeFillAngle
            addRegulate(CanvasRegulatePopupConfig.KEY_PATH_FILL_LINE_SPACE, gcodeFillStepPixel)
            addRegulate(CanvasRegulatePopupConfig.KEY_PATH_FILL_ANGLE, fillAngle)
            addRegulate(CanvasRegulatePopupConfig.KEY_SUBMIT)
            firstApply = false
            onApplyAction = { dismiss ->
                if (dismiss) {
                    //no
                } else {
                    val gcodeFillStep = getFloatOrDef(
                        CanvasRegulatePopupConfig.KEY_PATH_FILL_LINE_SPACE,
                        gcodeFillStepPixel
                    )
                    val gcodeFillAngle = getFloatOrDef(
                        CanvasRegulatePopupConfig.KEY_PATH_FILL_ANGLE,
                        fillAngle
                    )

                    owner.engraveLoadingAsync({
                        element.updatePathFill(renderer, delegate, gcodeFillStep, gcodeFillAngle)
                    }) {
                        onDismissAction()
                    }
                }
            }
        }
    }

    /**偏移*/
    fun handleOutline(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.getEngraveBitmapData() ?: return
        val context = anchor.context
        var outlineSpan = 2f
        var keepHole = true

        var svgRenderer: CanvasElementRenderer? = null

        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_OUTLINE_OFFSET, outlineSpan)
            addRegulate(CanvasRegulatePopupConfig.KEY_OUTLINE_HOLE, keepHole)
            addRegulate(CanvasRegulatePopupConfig.KEY_SUBMIT)
            firstApply = true
            realTimeApply = true
            onSubmitAction = { dismiss, submit ->
                if (dismiss) {
                    onDismissAction()
                    svgRenderer?.let { svgRenderer ->
                        delegate?.renderManager?.removeAfterRendererList(svgRenderer)
                    }
                } else if (submit) {
                    svgRenderer?.let { svgRenderer ->
                        delegate?.renderManager?.removeAfterRendererList(svgRenderer)
                        if (svgRenderer.lpElementBean()?.data.isNullOrBlank()) {
                            //空数据
                        } else {
                            //有效数据
                            delegate?.renderManager?.addElementRenderer(
                                svgRenderer,
                                false,
                                Reason.user,
                                Strategy.normal
                            )
                        }
                    }
                } else {
                    owner.engraveLoadingAsync({
                        outlineSpan = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE_OFFSET,
                            outlineSpan
                        )
                        keepHole = getBooleanOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE_HOLE,
                            keepHole
                        )
                        operateBitmap.let { bitmap ->
                            LTime.tick()
                            val svgPath = RustBitmapHandle.bitmapOutline(
                                bitmap,
                                outlineSpan.toInt(),
                                keepHole
                            )
                            "图片[${
                                bitmap.byteCount.toSizeString()
                            }]提取轮廓耗时:${LTime.time()}".writePerfLog()

                            if (svgRenderer == null) {
                                val elementBean = LPElementBean().apply {
                                    mtype = LPDataConstant.DATA_TYPE_SVG
                                    this.data = svgPath
                                    paintStyle = Paint.Style.STROKE.toPaintStyleInt()
                                    layerId = LayerHelper.LAYER_CUT
                                }
                                svgRenderer =
                                    LPRendererHelper.parseElementRenderer(elementBean, true)
                            } else {
                                svgRenderer?.lpPathElement()
                                    ?.updateElementPathData(svgPath, svgRenderer)
                            }
                            //需要移动到的目标中心
                            val rendererBounds = renderer.getRendererBounds()
                            var targetCenterX = 0f
                            var targetCenterY = 0f

                            svgRenderer?.lpPathElement()?.pathList?.computePathBounds()?.let {
                                targetCenterX = rendererBounds?.left ?: 0f
                                targetCenterY = rendererBounds?.top ?: 0f

                                targetCenterX += it.centerX() - outlineSpan * 2
                                targetCenterY += it.centerY() - outlineSpan * 2
                            }

                            svgRenderer?.translateCenterTo(
                                targetCenterX,
                                targetCenterY,
                                Reason.code,
                                Strategy.preview,
                                null
                            )
                            svgRenderer
                        }
                    }) {
                        it?.let {
                            delegate?.renderManager?.addAfterRendererList(it)
                        }
                    }
                }
            }
        }
    }

    /**曲线文本*/
    fun handleCurveText(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpTextElement() ?: return
        val bean = element.elementBean
        val context = anchor.context

        val tipRenderer = PathRenderer()
        tipRenderer.pathPaint.color = Color.MAGENTA
        delegate?.renderManager?.addAfterRendererList(tipRenderer)

        //保存状态
        val undoState = element.createStateStack()
        undoState.saveState(renderer, delegate)

        context.canvasRegulateWindow(anchor) {
            val curvature = bean.curvature
            addRegulate(CanvasRegulatePopupConfig.KEY_CURVATURE, curvature)
            addRegulate(CanvasRegulatePopupConfig.KEY_SUBMIT)
            firstApply = curvature != 0f
            realTimeApply = true
            onApplyValueAction = { type ->
                when (type) {
                    APPLY_TYPE_DISMISS -> {
                        undoState.restoreState(renderer, Reason.user, Strategy.undo, delegate)
                        delegate?.renderManager?.removeAfterRendererList(tipRenderer)
                        onDismissAction()
                    }

                    APPLY_TYPE_SUBMIT -> {
                        val redoState = element.createStateStack()
                        redoState.saveState(renderer, delegate)
                        delegate?.addStateToStack(renderer, undoState, redoState)
                        delegate?.renderManager?.removeAfterRendererList(tipRenderer)
                        onDismissAction()
                    }

                    APPLY_TYPE_CHANGE -> {
                        val curvature =
                            getFloatOrDef(CanvasRegulatePopupConfig.KEY_CURVATURE, curvature)
                        element.updateCurvature(curvature, renderer, delegate)
                        val path = element.curveTextDrawInfo?.run {
                            getTextDrawInnerCirclePath().apply {
                                val renderBounds = element.renderProperty.getRenderBounds()
                                val matrix = acquireTempMatrix()
                                val y = if (curvature > 0) {
                                    renderBounds.top + textHeight
                                } else {
                                    renderBounds.bottom
                                }
                                getTranslateMatrix(renderBounds.centerX(), y, matrix)
                                transform(matrix)
                                matrix.release()
                            }
                        }
                        val scale = delegate?.renderViewBox?.getScale() ?: 1f
                        tipRenderer.pathPaint.strokeWidth = dp / scale
                        tipRenderer.updatePath(path, delegate)
                    }
                }
            }
        }
    }

    //endregion---带参数调整对话框---

    /**图片剪裁*/
    fun handleCrop(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.originBitmap ?: return
        val bean = element.elementBean

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        anchor.context.cropDialog {
            cropBitmap = operateBitmap
            onDismissListener = {
                onDismissAction()
            }

            onCropResultAction = { result ->
                result?.let {
                    owner.engraveLoadingAsync({
                        //剪切完之后, 默认黑白处理
                        element.updateOriginBitmap(result, false)
                        addBitmapStateToStack(delegate, renderer, undoState)
                        result
                    }) {
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**图片魔棒*/
    fun handleMagicWand(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.getDrawBitmap() ?: return

        //用来恢复的状态
        val undoState = LPBitmapStateStack().apply { saveState(renderer, delegate) }

        anchor.context.magicWandDialog {
            originBitmap = operateBitmap
            onDismissListener = {
                onDismissAction()
            }

            onMagicResultAction = { result ->
                result?.let {
                    owner.engraveLoadingAsync({
                        //魔棒完之后, 默认黑白处理
                        element.updateOriginBitmap(result, false)
                        addBitmapStateToStack(delegate, renderer, undoState)
                        result
                    }) {
                        renderer.requestUpdatePropertyFlag(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }
}