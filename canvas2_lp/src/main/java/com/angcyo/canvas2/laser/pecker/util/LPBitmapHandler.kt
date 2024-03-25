package com.angcyo.canvas2.laser.pecker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.bluetooth.fsc.laserpacker.bean._isAutoCnc
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasElementRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas.render.renderer.PathRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig.Companion.APPLY_TYPE_CHANGE
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig.Companion.APPLY_TYPE_DISMISS
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig.Companion.APPLY_TYPE_SUBMIT
import com.angcyo.canvas2.laser.pecker.dialog.canvasRegulateWindow
import com.angcyo.canvas2.laser.pecker.dialog.magicWandDialog
import com.angcyo.canvas2.laser.pecker.dslitem.control.PathOpItem
import com.angcyo.canvas2.laser.pecker.element.LPBitmapStateStack
import com.angcyo.core.CoreApplication
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.vmApp
import com.angcyo.crop.ui.cropDialog
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper._defaultGCodeOutputFile
import com.angcyo.laserpacker.device.ble.DeviceSettingFragment
import com.angcyo.laserpacker.device.engraveLoadingAsync
import com.angcyo.laserpacker.toGCodePath
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.L
import com.angcyo.library.LTime
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.canvas.core.Reason
import com.angcyo.library.component.Strategy
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.pool.acquireTempMatrix
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.addBgColor
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.dp
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.toHexColorString
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.writeToFile
import com.angcyo.opencv.OpenCV
import com.angcyo.rust.handle.RustBitmapHandle
import com.angcyo.toSVGStrokeContentVectorStr
import com.angcyo.widget.span.span
import com.hingin.lp1.hiprint.rust.LdsCore
import com.hingin.rn.image.ImageProcess
import java.io.File
import kotlin.math.roundToInt

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
            alphaBgColor = if (HawkEngraveKeys.enableRemoveBWAlpha) if (inverse) Color.BLACK else LibHawkKeys.bgAlphaColor else Color.TRANSPARENT,
            alphaThreshold = LibHawkKeys.alphaThreshold,
            whiteReplaceColor = if (HawkEngraveKeys.enableBitmapHandleBgAlpha) Color.TRANSPARENT else LibHawkKeys.bgAlphaColor
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
            LibHawkKeys.bgAlphaColor,
            LibHawkKeys.bgAlphaThreshold
        )
    }

    /**2D浮雕处理*/
    fun toReliefHandle(
        bitmap: Bitmap,
        strength: Float = 0f,
        invert: Boolean = false,
    ): Bitmap? {
        return RustBitmapHandle.bitmapRelief(bitmap, strength, invert)
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
            type = if (bean.gcodeOutline && bean.gcodeLineSpace < LPDataConstant.DEFAULT_MIN_LINE_SPACE)
                2 /*仅轮廓*/ else if (bean.gcodeOutline) 1 /*轮廓+填充*/ else 3,
            /*不需要轮廓*/
            autoLaser = _isAutoCnc,
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
                            element.updateImageFilter(LPDataConstant.DATA_MODE_PRINT)
                            LTime.tick()
                            val result = toPrint(context, bitmap, bean.printsThreshold)
                            element.updateBeanWidthHeightFromBitmap(bitmap, false)
                            "图片[${bitmap.byteCount.toSizeString()}]转版画耗时:${LTime.time()}\n${bean}".writePerfLog()
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
        val operateBitmap =
            if (HawkEngraveKeys.enableBitmapFlowHandle) element.getDrawBitmap() else element.originBitmap
        operateBitmap ?: return
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
                            }]转GCode耗时:${LTime.time()}\n${bean}".writePerfLog()
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
                            element.updateImageFilter(LPDataConstant.DATA_MODE_BLACK_WHITE)
                            LTime.tick()
                            val result = toBlackWhiteHandle(bitmap, bean)
                            element.updateBeanWidthHeightFromBitmap(bitmap, false)
                            "图片[${bitmap.byteCount.toSizeString()}]转黑白耗时:${LTime.time()}\n${bean}".writePerfLog()
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
                            element.updateImageFilter(LPDataConstant.DATA_MODE_DITHERING)
                            LTime.tick()
                            val result = toGrayHandle(bitmap, bean)
                            element.updateBeanWidthHeightFromBitmap(bitmap, false)
                            "图片[${bitmap.byteCount.toSizeString()}]转灰度耗时:${LTime.time()}\n${bean}".writePerfLog()
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
                            element.updateImageFilter(LPDataConstant.DATA_MODE_GREY)
                            LTime.tick()
                            val result = toGrayHandle(bitmap, bean)
                            element.updateBeanWidthHeightFromBitmap(bitmap, false)
                            "图片[${bitmap.byteCount.toSizeString()}]转灰度耗时:${LTime.time()}\n${bean}".writePerfLog()
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
                            element.updateImageFilter(LPDataConstant.DATA_MODE_SEAL)
                            LTime.tick()
                            val result = toSealHandle(bitmap, bean.sealThreshold)
                            element.updateBeanWidthHeightFromBitmap(bitmap, false)
                            "图片[${bitmap.byteCount.toSizeString()}]转印章耗时:${LTime.time()}\n${bean}".writePerfLog()
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
                        "图片[${operateBitmap.byteCount.toSizeString()}]扭曲耗时:${LTime.time()}\n${bean}".writePerfLog()

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
                    onDismissAction()
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
                        element.updatePathFill(
                            renderer,
                            delegate,
                            gcodeFillStep,
                            gcodeFillAngle
                        )
                    }) {
                        onDismissAction()
                    }
                }
            }
        }
    }

    /**图片偏移*/
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

        var svgRenderer: CanvasElementRenderer? = null

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_OUTLINE_OFFSET,
                HawkEngraveKeys.lastOutlineSpan
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_OUTLINE_HOLE,
                HawkEngraveKeys.lastOutlineKeepHole
            )
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
                        val elementBean = svgRenderer.lpElementBean()
                        delegate?.renderManager?.removeAfterRendererList(svgRenderer)
                        if (elementBean?.data.isNullOrBlank()) {
                            //空数据
                        } else {
                            //有效数据
                            elementBean?.stroke = null // 清空颜色
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
                        HawkEngraveKeys.lastOutlineSpan = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE_OFFSET,
                            HawkEngraveKeys.lastOutlineSpan
                        )
                        HawkEngraveKeys.lastOutlineKeepHole = getBooleanOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE_HOLE,
                            HawkEngraveKeys.lastOutlineKeepHole
                        )
                        operateBitmap.let { bitmap ->
                            LTime.tick()
                            val svgPath = RustBitmapHandle.bitmapOutline(
                                bitmap,
                                HawkEngraveKeys.lastOutlineSpan.toInt(),
                                HawkEngraveKeys.lastOutlineKeepHole
                            )
                            "图片[${
                                bitmap.byteCount.toSizeString()
                            }]提取轮廓耗时:${LTime.time()}".writePerfLog()

                            if (svgRenderer == null) {
                                val elementBean = LPElementBean().apply {
                                    mtype = LPDataConstant.DATA_TYPE_SVG
                                    this.data = svgPath
                                    paintStyle = Paint.Style.STROKE.toPaintStyleInt()
                                    dataMode = LPDataConstant.DATA_MODE_GCODE
                                    isCut = vmApp<DeviceStateModel>().haveCutLayer()
                                    /*layerId = if (vmApp<DeviceStateModel>().haveCutLayer()) {
                                        LaserPeckerHelper.LAYER_CUT
                                    } else {
                                        LaserPeckerHelper.LAYER_LINE
                                    }*/
                                    stroke = Color.MAGENTA.toHexColorString()
                                }
                                svgRenderer =
                                    LPRendererHelper.parseElementRenderer(elementBean, false)
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

                                val outlineSpan = HawkEngraveKeys.lastOutlineSpan
                                if (outlineSpan > 0) {
                                    targetCenterX += it.centerX() - outlineSpan * 2
                                    targetCenterY += it.centerY() - outlineSpan * 2
                                } else {
                                    targetCenterX += it.centerX() + outlineSpan
                                    targetCenterY += it.centerY() + outlineSpan
                                    //targetCenterX = rendererBounds?.centerX() ?: 0f
                                    //targetCenterY = rendererBounds?.centerY() ?: 0f
                                }
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

    /**矢量/路径偏移, 支持多个路径
     *
     * [CanvasGroupRenderer]
     * */
    fun handlePathOffset(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner?,
        renderer: BaseRenderer?,
        onDismissAction: () -> Unit = {}
    ) {
        owner ?: return
        renderer ?: return
        val pathList = PathOpItem.getAllElementDrawPath(renderer);
        if (pathList.isEmpty()) {
            return
        }

        // pathList 对应的svg path路径数据, 在首次触发时生成.
        var pathSvgPath: String? = null

        val context = anchor.context
        var svgRenderer: CanvasElementRenderer? = null

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_OUTLINE_OFFSET,
                HawkEngraveKeys.lastOutlineSpan
            )
            /*addRegulate(
                CanvasRegulatePopupConfig.KEY_OUTLINE_HOLE,
                HawkEngraveKeys.lastOutlineKeepHole
            )*/
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
                        val elementBean = svgRenderer.lpElementBean()
                        delegate?.renderManager?.removeAfterRendererList(svgRenderer)
                        if (elementBean?.data.isNullOrBlank()) {
                            //空数据
                        } else {
                            //有效数据
                            elementBean?.stroke = null // 清空颜色
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
                        HawkEngraveKeys.lastOutlineSpan = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE_OFFSET,
                            HawkEngraveKeys.lastOutlineSpan
                        )
                        /*HawkEngraveKeys.lastOutlineKeepHole = getBooleanOrDef(
                            CanvasRegulatePopupConfig.KEY_OUTLINE_HOLE,
                            HawkEngraveKeys.lastOutlineKeepHole
                        )*/

                        //公差
                        val svgTolerance = LibHawkKeys.svgTolerance

                        if (pathSvgPath.isNullOrBlank()) {
                            pathSvgPath =
                                pathList.toSVGStrokeContentVectorStr(pathStep = svgTolerance)
                            //pathSvgPath = pathList.toSvgPathContent(LibHawkKeys.svgTolerance)
                        }

                        pathSvgPath?.let { pathString ->
                            LTime.tick()

                            @Pixel
                            val outlineSpan = HawkEngraveKeys.lastOutlineSpan.toPixel()

                            val svgPath = LdsCore.pathOffset(
                                pathString,
                                outlineSpan.toDouble(),
                                10.0,
                                svgTolerance.toDouble()
                            )
                            "矢量偏移[${
                                svgPath?.length?.toSizeString()
                            }]耗时:${LTime.time()}".writePerfLog()

                            if (svgRenderer == null) {
                                val elementBean = LPElementBean().apply {
                                    mtype = LPDataConstant.DATA_TYPE_SVG
                                    this.data = svgPath
                                    paintStyle = Paint.Style.STROKE.toPaintStyleInt()
                                    dataMode = LPDataConstant.DATA_MODE_GCODE
                                    isCut = vmApp<DeviceStateModel>().haveCutLayer()
                                    /*layerId = if (vmApp<DeviceStateModel>().haveCutLayer()) {
                                        LaserPeckerHelper.LAYER_CUT
                                    } else {
                                        LaserPeckerHelper.LAYER_LINE
                                    }*/
                                    stroke = Color.MAGENTA.toHexColorString()
                                }
                                svgRenderer =
                                    LPRendererHelper.parseElementRenderer(elementBean, false)
                            } else {
                                svgRenderer?.lpPathElement()
                                    ?.updateElementPathData(svgPath, svgRenderer)
                            }
                            //需要移动到的目标中心
                            val rendererBounds = renderer.getRendererBounds()
                            var targetX = rendererBounds?.left ?: 0f
                            var targetY = rendererBounds?.top ?: 0f

                            svgRenderer?.lpPathElement()?.pathList?.computePathBounds()?.let {
                                targetX = it.left
                                targetY = it.top
                            }

                            svgRenderer?.translateLeftTo(
                                targetX,
                                targetY,
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

    /**位图临摹*/
    fun handleTracer(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.getEngraveBitmapData() ?: return
        val context = anchor.context

        var svgRenderer: CanvasElementRenderer? = null

        context.canvasRegulateWindow(anchor) {
            val helpUrl = DeviceSettingFragment.getHelpUrl(
                _deviceSettingBean?.tracerHelpUrl,
                _deviceSettingBean?.tracerHelpUrlZh
            )
            popupTitle = span {
                append(_string(R.string.canvas_tracer))
                if (helpUrl != null) {
                    append(" ")
                    appendDrawable(_drawable(R.drawable.rotate_flag_help_svg))
                }
            }
            if (helpUrl != null) {
                onInitLayout = { window, viewHolder ->
                    viewHolder.click(R.id.lib_title_text_view) {
                        CoreApplication.onOpenUrlAction?.invoke(helpUrl)
                    }
                }
            }

            addRegulate(
                CanvasRegulatePopupConfig.KEY_TRACER_FILTER,
                HawkEngraveKeys.lastTracerFilter
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_TRACER_CORNER,
                HawkEngraveKeys.lastTracerCorner
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_TRACER_LENGTH,
                HawkEngraveKeys.lastTracerLength
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_TRACER_SPLICE,
                HawkEngraveKeys.lastTracerSplice
            )
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
                        val elementBean = svgRenderer.lpElementBean()
                        delegate?.renderManager?.removeAfterRendererList(svgRenderer)
                        if (elementBean?.data.isNullOrBlank()) {
                            //空数据
                        } else {
                            //有效数据
                            elementBean?.stroke = null // 清空颜色
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
                        HawkEngraveKeys.lastTracerFilter = getIntOrDef(
                            CanvasRegulatePopupConfig.KEY_TRACER_FILTER,
                            HawkEngraveKeys.lastTracerFilter
                        )
                        HawkEngraveKeys.lastTracerCorner = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_TRACER_CORNER,
                            HawkEngraveKeys.lastTracerCorner
                        )
                        HawkEngraveKeys.lastTracerLength = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_TRACER_LENGTH,
                            HawkEngraveKeys.lastTracerLength
                        )
                        HawkEngraveKeys.lastTracerSplice = getFloatOrDef(
                            CanvasRegulatePopupConfig.KEY_TRACER_SPLICE,
                            HawkEngraveKeys.lastTracerSplice
                        )
                        operateBitmap.let { bitmap ->
                            LTime.tick()
                            val svgPath = RustBitmapHandle.bitmapTracer(
                                bitmap,
                                HawkEngraveKeys.lastTracerFilter,
                                HawkEngraveKeys.lastTracerCorner,
                                HawkEngraveKeys.lastTracerLength,
                                HawkEngraveKeys.lastTracerSplice
                            )
                            "图片[${
                                bitmap.byteCount.toSizeString()
                            }]位图临摹廓耗时:${LTime.time()}".writePerfLog()

                            if (svgRenderer == null) {
                                val elementBean = LPElementBean().apply {
                                    mtype = LPDataConstant.DATA_TYPE_SVG
                                    this.data = svgPath
                                    paintStyle = Paint.Style.STROKE.toPaintStyleInt()
                                    dataMode = LPDataConstant.DATA_MODE_GCODE
                                    isCut = vmApp<DeviceStateModel>().haveCutLayer()
                                    /*layerId = if (vmApp<DeviceStateModel>().haveCutLayer()) {
                                        LaserPeckerHelper.LAYER_CUT
                                    } else {
                                        LaserPeckerHelper.LAYER_LINE
                                    }*/
                                    stroke = Color.MAGENTA.toHexColorString()
                                }
                                svgRenderer =
                                    LPRendererHelper.parseElementRenderer(elementBean, false)
                            } else {
                                svgRenderer?.lpPathElement()
                                    ?.updateElementPathData(svgPath, svgRenderer)
                            }
                            //需要移动到的目标中心
                            val rendererBounds = renderer.getRendererBounds()
                            val targetCenterX = rendererBounds?.centerX() ?: 0f
                            val targetCenterY = rendererBounds?.centerY() ?: 0f

                            //移动中点位置到指定的坐标
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
            var curvature = bean.curvature
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
                        curvature =
                            getFloatOrDef(CanvasRegulatePopupConfig.KEY_CURVATURE, curvature)
                        element.updateCurvature(curvature, renderer, delegate)
                        val path = element.curveTextDrawInfo?.run {
                            val baseRect = acquireTempRectF()
                            element.renderProperty.getBaseRect(baseRect)
                            /*val centerX = baseRect.centerX()
                            val centerY = if (curvature > 0) {
                                baseRect.top + textHeight + innerRadius + drawOffsetY
                            } else {
                                baseRect.top - innerRadius + drawOffsetY
                            }*/
                            val centerX = curveCx
                            val centerY = curveCy
                            val resultPath = Path()
                            resultPath.addCircle(
                                centerX,
                                centerY,
                                innerRadius,
                                Path.Direction.CW
                            )

                            val matrix = acquireTempMatrix()
                            element.renderProperty.getRenderMatrix(matrix)
                            resultPath.transform(matrix)

                            baseRect.release()
                            matrix.release()

                            resultPath
                        }
                        val scale = delegate?.renderViewBox?.getScale() ?: 1f
                        tipRenderer.pathPaint.strokeWidth = dp / scale
                        tipRenderer.updatePath(path, delegate)
                    }
                }
            }
        }
    }


    /**切片, 只调整切片对应的数量, 和渲染效果没有区别, 但是要触发更新index*/
    fun handleSlice(
        delegate: CanvasRenderDelegate?,
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val bean = renderer.lpElementBean() ?: return
        val element = renderer.lpBitmapElement() ?: return
        val operateBitmap = element.getEngraveBitmapData() ?: return
        val context = anchor.context

        val colors = BitmapHandle.getChannelValueList(operateBitmap).map { it.toUByte().toInt() }
        val maxSliceCount = 255 - (colors.minOrNull() ?: 255)
        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SLICE_HEIGHT, bean.sliceHeight)
            addRegulate(
                CanvasRegulatePopupConfig.KEY_SLICE,
                if (bean.sliceCount <= 0) maxSliceCount else bean.sliceCount
            )
            setProperty(CanvasRegulatePopupConfig.KEY_SLICE_MAX, maxSliceCount)
            firstApply = false
            onApplyAction = { dismiss ->
                bean.sliceHeight =
                    getFloatOrDef(CanvasRegulatePopupConfig.KEY_SLICE_HEIGHT, bean.sliceHeight)
                bean.sliceCount =
                    getIntOrDef(CanvasRegulatePopupConfig.KEY_SLICE, bean.sliceCount)
                if (isDebugType()) {
                    //测试切片后的阈值
                    val sliceHeight = (bean.sliceHeight * 10).roundToInt() / 10f //切片高度
                    val thresholdList =
                        EngraveTransitionHelper.getSliceThresholdList(colors, bean.sliceCount)
                    L.i(buildString {
                        append("切片[${bean.sliceHeight}/${sliceHeight}][${bean.sliceCount}/${maxSliceCount}]:")
                        append("${colors}->")
                        append("$thresholdList")
                    })
                }
                if (dismiss) {
                    renderer.requestUpdatePropertyFlag(Reason.user.apply {
                        controlType = BaseControlPoint.CONTROL_TYPE_DATA
                    }, delegate)
                    onDismissAction()
                }
            }
        }
    }

    /**2D浮雕*/
    fun handleRelief(
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
            val helpUrl = DeviceSettingFragment.getHelpUrl(
                _deviceSettingBean?.reliefHelpUrl,
                _deviceSettingBean?.reliefHelpUrlZh
            )
            popupTitle = span {
                append(_string(R.string.canvas_2d_relief))
                if (helpUrl != null) {
                    append(" ")
                    appendDrawable(_drawable(R.drawable.rotate_flag_help_svg))
                }
            }
            if (helpUrl != null) {
                onInitLayout = { window, viewHolder ->
                    viewHolder.click(R.id.lib_title_text_view) {
                        CoreApplication.onOpenUrlAction?.invoke(helpUrl)
                    }
                }
            }


            addRegulate(CanvasRegulatePopupConfig.KEY_RELIEF_INVERT, bean.inverse)
            addRegulate(CanvasRegulatePopupConfig.KEY_RELIEF_STRENGTH, bean.reliefStrength)
            firstApply = bean.imageFilter != LPDataConstant.DATA_MODE_RELIEF
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
                                CanvasRegulatePopupConfig.KEY_RELIEF_INVERT,
                                bean.inverse
                            )
                            bean.reliefStrength = getIntOrDef(
                                CanvasRegulatePopupConfig.KEY_RELIEF_STRENGTH,
                                bean.reliefStrength
                            )
                            element.updateImageFilter(LPDataConstant.DATA_MODE_RELIEF)
                            LTime.tick()
                            val result =
                                toReliefHandle(bitmap, bean.reliefStrength.toFloat(), bean.inverse)
                            element.updateBeanWidthHeightFromBitmap(bitmap, false)
                            "图片[${bitmap.byteCount.toSizeString()}]转2D浮雕耗时:${LTime.time()}\n${bean}".writePerfLog()
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

    //endregion---带参数调整对话框---

    //region---图片其他处理---

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
                    val bitmap = if (HawkEngraveKeys.enableRemoveBWAlpha) result
                    else result.addBgColor(if (bean.imageFilter == LPDataConstant.DATA_MODE_SEAL) Color.BLACK else Color.WHITE)
                    owner.engraveLoadingAsync({
                        element.updateOriginBitmap(bitmap, false)
                        addBitmapStateToStack(delegate, renderer, undoState)
                        bitmap
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
                    val elementBean = element.elementBean
                    //2023-10-12 清除相关属性数据
                    elementBean.inverse = false
                    elementBean.brightness = 0f
                    elementBean.contrast = 0f

                    owner.engraveLoadingAsync({
                        element.updateOriginBitmap(result, false)
                        addBitmapStateToStack(delegate, renderer, undoState)
                        element.renderBitmap = result //2023-6-15 透明图片, 障眼法
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

    //endregion---图片其他处理---
}