package com.angcyo.canvas2.laser.pecker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.core.Reason
import com.angcyo.canvas.render.core.Strategy
import com.angcyo.canvas.render.core.component.BaseControlPoint
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.dialog.CanvasRegulatePopupConfig
import com.angcyo.canvas2.laser.pecker.dialog.canvasRegulateWindow
import com.angcyo.canvas2.laser.pecker.element.LPBitmapStateStack
import com.angcyo.canvas2.laser.pecker.parseGCode
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.crop.ui.cropDialog
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.engraveLoadingAsync
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.LTime
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.unit.toMm
import com.angcyo.library.utils.writeToFile
import com.angcyo.opencv.OpenCV
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
            alphaThreshold = LibHawkKeys.alphaThreshold
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
        return OpenCV.bitmapToPrint(context, toGrayHandle(bitmap)!!, printsThreshold.toInt())
    }

    /**印章处理*/
    fun toSeal(context: Context, bitmap: Bitmap, sealThreshold: Float): Bitmap? {
        //先黑白画?还是后黑白画?
        return OpenCV.bitmapToSeal(context, bitmap, sealThreshold.toInt())
    }

    /**转GCode处理*/
    fun toGCode(context: Context, bitmap: Bitmap, bean: LPElementBean): File {
        return OpenCV.bitmapToGCode(
            context,
            bitmap,
            bitmap.width.toMm().toDouble(),
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
            LPBitmapStateStack().apply { saveState(renderer) },
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD,
                bean.printsThreshold.toInt()
            )
            firstApply = bean.imageFilter != LPConstant.DATA_MODE_PRINT
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
                            bean.imageFilter = LPConstant.DATA_MODE_PRINT
                            LTime.tick()
                            val result = toPrint(context, bitmap, bean.printsThreshold)
                            "图片[${bitmap.byteCount.toSizeString()}]转版画耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdateDrawable(Reason.user.apply {
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

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
                            val result = gCodeText to GCodeHelper.parseGCode(gCodeText)
                            "解析GCode数据[${gCodeText.length.toSizeString()}]耗时:${LTime.time()}".writePerfLog()
                            gCodeText.writeToFile(CanvasDataHandleOperate._defaultGCodeOutputFile())

                            element.updateOriginBitmapGCode(
                                result.second?.gCodePath?.run { listOf(this) },
                                result.first,
                                false
                            )
                            //stack
                            addBitmapStateToStack(delegate, renderer, undoState)

                            result
                        }
                    }) {
                        renderer.requestUpdateDrawableAndProperty(Reason.user.apply {
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_BW_INVERT,
                bean.inverse
            )
            addRegulate(
                CanvasRegulatePopupConfig.KEY_BW_THRESHOLD,
                bean.blackThreshold.toInt()
            )
            firstApply = bean.imageFilter != LPConstant.DATA_MODE_BLACK_WHITE
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
                            bean.imageFilter = LPConstant.DATA_MODE_BLACK_WHITE
                            LTime.tick()
                            val result = toBlackWhiteHandle(bitmap, bean)
                            "图片[${bitmap.byteCount.toSizeString()}]转黑白耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdateDrawable(Reason.user.apply {
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT, bean.inverse)
            addRegulate(CanvasRegulatePopupConfig.KEY_CONTRAST, bean.contrast)
            addRegulate(CanvasRegulatePopupConfig.KEY_BRIGHTNESS, bean.brightness)
            firstApply = bean.imageFilter != LPConstant.DATA_MODE_DITHERING
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
                            bean.imageFilter = LPConstant.DATA_MODE_DITHERING
                            LTime.tick()
                            val result = toGrayHandle(bitmap, bean)
                            "图片[${bitmap.byteCount.toSizeString()}]转灰度耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdateDrawable(Reason.user.apply {
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT, bean.inverse)
            addRegulate(CanvasRegulatePopupConfig.KEY_CONTRAST, bean.contrast)
            addRegulate(CanvasRegulatePopupConfig.KEY_BRIGHTNESS, bean.brightness)
            firstApply = bean.imageFilter != LPConstant.DATA_MODE_GREY
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
                            bean.imageFilter = LPConstant.DATA_MODE_GREY
                            LTime.tick()
                            val result = toGrayHandle(bitmap, bean)
                            "图片[${bitmap.byteCount.toSizeString()}]转灰度耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdateDrawable(Reason.user.apply {
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

        context.canvasRegulateWindow(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD,
                bean.sealThreshold.toInt()
            )
            firstApply = bean.imageFilter != LPConstant.DATA_MODE_SEAL
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
                            bean.imageFilter = LPConstant.DATA_MODE_SEAL
                            LTime.tick()
                            val result = toSeal(context, bitmap, bean.sealThreshold)
                            "图片[${bitmap.byteCount.toSizeString()}]转印章耗时:${LTime.time()}".writePerfLog()
                            result
                        }
                    }) { result ->
                        element.renderBitmap = result
                        renderer.requestUpdateDrawable(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

        anchor.context.cropDialog {
            cropBitmap = operateBitmap
            onDismissListener = {
                onDismissAction()
            }

            onCropResultAction = { result ->
                result?.let {
                    owner.engraveLoadingAsync({
                        //剪切完之后, 默认黑白处理
                        element.updateOriginBitmapSrc(delegate, renderer, result, false)
                        addBitmapStateToStack(delegate, renderer, undoState)
                        result
                    }) {
                        renderer.requestUpdateDrawableAndProperty(Reason.user.apply {
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
        val undoState = LPBitmapStateStack().apply { saveState(renderer) }

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
                            element.updateOriginBitmapSrc(delegate, renderer, result, false)
                        }
                        addBitmapStateToStack(delegate, renderer, undoState)

                        result
                    }) {
                        renderer.requestUpdateDrawable(Reason.user.apply {
                            controlType = BaseControlPoint.CONTROL_TYPE_DATA
                        }, delegate)
                    }
                }
            }
        }
    }

    /**路径填充*/
    fun handlePathFill(
        anchor: View,
        owner: LifecycleOwner,
        renderer: BaseRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        /*val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        context.canvasRegulateWindow2(anchor) {
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
                        item.updatePathFill(gcodeFillStep, gcodeFillAngle, renderer)
                    }) {
                        onDismissAction()
                    }
                }
            }
        }*/
    }

    //endregion---带参数调整对话框---

}