package com.angcyo.canvas.laser.pecker

import android.graphics.Color
import android.graphics.RectF
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.items.data.DataBitmapItem
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.parseGCode
import com.angcyo.crop.ui.cropDialog
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.engraveLoadingAsync
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex.*
import com.angcyo.library.unit.toMm
import com.angcyo.library.utils.writeToFile
import com.angcyo.opencv.OpenCV
import com.hingin.rn.image.ImageProcess
import kotlin.io.readText

/**
 * 图片编辑处理, 实时改变, 不需要确定按钮, 模式恢复
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/15
 */
object CanvasBitmapHandler {

    /**版画*/
    fun handlePrint(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!

        context.canvasRegulateWindow2(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_PRINT_THRESHOLD,
                item.dataBean.printsThreshold.toInt()
            )
            firstApply = renderer.dataItem?.dataBean?.imageFilter != CanvasConstant.DATA_MODE_PRINT
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            item.dataBean.printsThreshold = getIntOrDef(
                                CanvasRegulatePopupConfig2.KEY_PRINT_THRESHOLD,
                                item.dataBean.printsThreshold.toInt()
                            ).toFloat()
                            OpenCV.bitmapToPrint(
                                context,
                                bitmap.toGrayHandle(Color.WHITE),
                                item.dataBean.printsThreshold.toInt()
                            )
                        }
                    }) {
                        it?.let {
                            item.updateBitmapByMode(
                                it.toBase64Data(),
                                CanvasConstant.DATA_MODE_PRINT,
                                renderer
                            )
                        }
                    }
                }
            }
        }
    }

    /**GCode*/
    fun handleGCode(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!
        val beforeBounds = RectF(renderer.getBounds())

        var boundsRotate = 0f //需要旋转的角度

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig2.KEY_OUTLINE, item.dataBean.gcodeOutline)
            addRegulate(CanvasRegulatePopupConfig2.KEY_LINE_SPACE, item.dataBean.gcodeLineSpace)
            addRegulate(CanvasRegulatePopupConfig2.KEY_ANGLE, item.dataBean.gcodeAngle)
            addRegulate(CanvasRegulatePopupConfig2.KEY_DIRECTION, item.dataBean.gcodeDirection)
            addRegulate(CanvasRegulatePopupConfig2.KEY_SUBMIT)
            firstApply = renderer.dataItem?.dataBean?.imageFilter != CanvasConstant.DATA_MODE_GCODE
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({
                        //direction
                        val direction = getIntOrDef(
                            CanvasRegulatePopupConfig2.KEY_DIRECTION,
                            item.dataBean.gcodeDirection
                        )
                        if (direction == 1 || direction == 3) {
                            boundsRotate = 90f
                        }
                        item.dataBean.gcodeDirection = direction

                        //lineSpace
                        val lineSpace = getFloatOrDef(
                            CanvasRegulatePopupConfig2.KEY_LINE_SPACE,
                            item.dataBean.gcodeLineSpace
                        )
                        item.dataBean.gcodeLineSpace = lineSpace

                        //angle
                        val angle = getFloatOrDef(
                            CanvasRegulatePopupConfig2.KEY_ANGLE,
                            item.dataBean.gcodeAngle
                        )
                        item.dataBean.gcodeAngle = angle

                        //outline
                        val outline = getBooleanOrDef(
                            CanvasRegulatePopupConfig2.KEY_OUTLINE,
                            item.dataBean.gcodeOutline
                        )
                        item.dataBean.gcodeOutline = outline

                        operateBitmap.let { bitmap ->
                            OpenCV.bitmapToGCode(
                                context,
                                bitmap,
                                (beforeBounds.width() / 2).toMm().toDouble(),
                                lineSpace = lineSpace.toDouble(),
                                direction = direction,
                                angle = angle.toDouble(),
                                type = if (outline) 1 else 3
                            ).let {
                                val gCodeText = it.readText()
                                it.deleteSafe()
                                gCodeText to GCodeHelper.parseGCode(gCodeText)
                            }
                        }
                    }) {
                        it?.let {
                            it.first.writeToFile(CanvasDataHandleOperate._defaultGCodeOutputFile())
                            beforeBounds.rotate(boundsRotate)
                            it.second?.gCodeBound?.let {
                                val gcodeWidth = it.width()
                                val gcodeHeight = it.height()
                                var newWidth = gcodeWidth
                                var newHeight = gcodeHeight
                                if (gcodeWidth > gcodeHeight) {
                                    val scale = beforeBounds.width() / gcodeWidth
                                    newWidth = gcodeWidth * scale
                                    newHeight = gcodeHeight * scale
                                } else {
                                    val scale = beforeBounds.height() / gcodeHeight
                                    newWidth = gcodeWidth * scale
                                    newHeight = gcodeHeight * scale
                                }
                                beforeBounds.setWidthHeight(newWidth, newHeight, true)
                            }
                            item.updateBitmapByMode(
                                it.first,
                                CanvasConstant.DATA_MODE_GCODE,
                                renderer,
                                beforeBounds.width(),
                                beforeBounds.height()
                            )
                        }
                    }
                }
            }
        }
    }

    /**黑白画*/
    fun handleBlackWhite(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!

        context.canvasRegulateWindow2(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_BW_INVERT,
                item.dataBean.inverse
            )
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_BW_THRESHOLD,
                item.dataBean.blackThreshold.toInt()
            )
            firstApply =
                renderer.dataItem?.dataBean?.imageFilter != CanvasConstant.DATA_MODE_BLACK_WHITE
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->

                            item.dataBean.blackThreshold = getIntOrDef(
                                CanvasRegulatePopupConfig2.KEY_BW_THRESHOLD,
                                item.dataBean.blackThreshold.toInt()
                            ).toFloat()

                            item.dataBean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig2.KEY_BW_INVERT, item.dataBean.inverse
                            )

                            /*OpenCV.bitmapToBlackWhite(
                                bitmap,
                                item.dataBean.blackThreshold.toInt(),
                                if (item.dataBean.inverse) 1 else 0
                            )*/
                            bitmap.toBlackWhiteHandle(
                                item.dataBean.blackThreshold.toInt(),
                                item.dataBean.inverse
                            )
                        }
                    }) {
                        it?.let {
                            item.updateBitmapByMode(
                                it.toBase64Data(),
                                CanvasConstant.DATA_MODE_BLACK_WHITE,
                                renderer
                            )
                        }
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
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig2.KEY_SHAKE_INVERT, item.dataBean.inverse)
            addRegulate(CanvasRegulatePopupConfig2.KEY_CONTRAST, item.dataBean.contrast)
            addRegulate(CanvasRegulatePopupConfig2.KEY_BRIGHTNESS, item.dataBean.brightness)
            firstApply =
                renderer.dataItem?.dataBean?.imageFilter != CanvasConstant.DATA_MODE_DITHERING
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            item.dataBean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig2.KEY_SHAKE_INVERT,
                                item.dataBean.inverse
                            )

                            item.dataBean.contrast = getFloatOrDef(
                                CanvasRegulatePopupConfig2.KEY_CONTRAST,
                                item.dataBean.contrast
                            )

                            item.dataBean.brightness = getFloatOrDef(
                                CanvasRegulatePopupConfig2.KEY_BRIGHTNESS,
                                item.dataBean.brightness
                            )

                            /*BitmapGraphicsParser.handleDithering(
                                bitmap, item.dataBean.inverse,
                                item.dataBean.contrast.toDouble(),
                                item.dataBean.brightness.toDouble()
                            )*/

                            //灰度, 抖动图, 使用灰度显示
                            bitmap.toGrayHandle(
                                item.dataBean.inverse,
                                item.dataBean.contrast,
                                item.dataBean.brightness
                            )
                        }
                    }) {
                        it?.let {
                            item.updateBitmapByMode(
                                it.toBase64Data(),
                                CanvasConstant.DATA_MODE_DITHERING,
                                renderer
                            )
                        }
                    }
                }
            }
        }
    }

    /**灰度*/
    fun handleGrey(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig2.KEY_SHAKE_INVERT, item.dataBean.inverse)
            addRegulate(CanvasRegulatePopupConfig2.KEY_CONTRAST, item.dataBean.contrast)
            addRegulate(CanvasRegulatePopupConfig2.KEY_BRIGHTNESS, item.dataBean.brightness)
            firstApply =
                renderer.dataItem?.dataBean?.imageFilter != CanvasConstant.DATA_MODE_GREY
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({
                        operateBitmap.let { bitmap ->
                            item.dataBean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig2.KEY_SHAKE_INVERT,
                                item.dataBean.inverse
                            )

                            item.dataBean.contrast = getFloatOrDef(
                                CanvasRegulatePopupConfig2.KEY_CONTRAST,
                                item.dataBean.contrast
                            )

                            item.dataBean.brightness = getFloatOrDef(
                                CanvasRegulatePopupConfig2.KEY_BRIGHTNESS,
                                item.dataBean.brightness
                            )

                            //灰度
                            bitmap.toGrayHandle(
                                item.dataBean.inverse,
                                item.dataBean.contrast,
                                item.dataBean.brightness
                            )
                        }
                    }) {
                        it?.let {
                            item.updateBitmapByMode(
                                it.toBase64Data(),
                                CanvasConstant.DATA_MODE_GREY,
                                renderer
                            )
                        }
                    }
                }
            }
        }

        /*owner.engraveLoadingAsync({
            operateBitmap.toGrayHandle()
        }) {
            it?.let {
                item.updateBitmapByMode(
                    it.toBase64Data(),
                    CanvasConstant.DATA_MODE_GREY,
                    renderer
                )
            }
        }*/
    }

    /**印章*/
    fun handleSeal(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!

        context.canvasRegulateWindow2(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_SEAL_THRESHOLD,
                item.dataBean.sealThreshold.toInt()
            )
            firstApply = renderer.dataItem?.dataBean?.imageFilter != CanvasConstant.DATA_MODE_SEAL
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.engraveLoadingAsync({
                        item.dataBean.sealThreshold = getIntOrDef(
                            CanvasRegulatePopupConfig2.KEY_SEAL_THRESHOLD,
                            item.dataBean.sealThreshold.toInt()
                        ).toFloat()
                        operateBitmap.let { bitmap ->
                            OpenCV.bitmapToSeal(
                                context,
                                bitmap.toGrayHandle(Color.WHITE),
                                item.dataBean.sealThreshold.toInt()
                            )
                        }
                    }) {
                        it?.let {
                            item.updateBitmapByMode(
                                it.toBase64Data(),
                                CanvasConstant.DATA_MODE_SEAL,
                                renderer
                            )
                        }
                    }
                }
            }
        }
    }

    /**图片剪裁*/
    fun handleCrop(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        //val beforeBounds = RectF(renderer.getBounds())

        anchor.context.cropDialog {
            cropBitmap = originBitmap
            onDismissListener = {
                onDismissAction()
            }

            onCropResultAction = { result ->
                result?.let {
                    owner.engraveLoadingAsync({
                        //剪切完之后, 默认黑白处理
                        val filter = result.toBlackWhiteBitmap(item.dataBean.blackThreshold.toInt())
                        item.updateBitmapOriginal(
                            result.toBase64Data(),
                            filter,
                            CanvasConstant.DATA_MODE_BLACK_WHITE,
                            renderer,
                            result.width.toFloat(),
                            result.height.toFloat(),
                        )
                    })
                }
            }
        }
    }

    /**图片扭曲*/
    fun handleMesh(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap

        context.canvasRegulateWindow2(anchor) {
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_MESH_SHAPE,
                CanvasRegulatePopupConfig2.DEFAULT_MESH_SHAPE
            )
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_MIN_DIAMETER,
                HawkEngraveKeys.lastMinDiameterPixel
            )
            addRegulate(
                CanvasRegulatePopupConfig2.KEY_MAX_DIAMETER,
                HawkEngraveKeys.lastDiameterPixel
            )
            addRegulate(CanvasRegulatePopupConfig2.KEY_SUBMIT)

            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    val minDiameter = getFloatOrDef(
                        CanvasRegulatePopupConfig2.KEY_MIN_DIAMETER,
                        HawkEngraveKeys.lastMinDiameterPixel
                    )
                    val maxDiameter = getFloatOrDef(
                        CanvasRegulatePopupConfig2.KEY_MAX_DIAMETER,
                        HawkEngraveKeys.lastDiameterPixel
                    )
                    // "CONE" 圆锥
                    // "BALL" 球体
                    val shape = getStringOrDef(
                        CanvasRegulatePopupConfig2.KEY_MESH_SHAPE,
                        CanvasRegulatePopupConfig2.DEFAULT_MESH_SHAPE
                    )
                    owner.engraveLoadingAsync({
                        originBitmap?.let {
                            ImageProcess.imageMesh(
                                originBitmap,
                                minDiameter,
                                maxDiameter,
                                shape
                            )
                        }
                    }) {
                        it?.let {
                            owner.engraveLoadingAsync({
                                //剪切完之后, 默认背白处理
                                val filter =
                                    it.toBlackWhiteBitmap(item.dataBean.blackThreshold.toInt())
                                item.updateBitmapMesh(
                                    filter,
                                    CanvasConstant.DATA_MODE_BLACK_WHITE,
                                    shape, minDiameter, maxDiameter, renderer
                                )
                            })
                        }
                    }
                }
            }
        }
    }

    /**路径填充*/
    fun handlePathFill(
        anchor: View,
        owner: LifecycleOwner,
        renderer: DataItemRenderer,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        context.canvasRegulateWindow2(anchor) {
            val gcodeFillStepPixel = item.dataBean.gcodeFillStep
            addRegulate(CanvasRegulatePopupConfig2.KEY_PATH_FILL_LINE_SPACE, gcodeFillStepPixel)
            addRegulate(CanvasRegulatePopupConfig2.KEY_SUBMIT)
            firstApply = false
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    val gcodeFillStep = getFloatOrDef(
                        CanvasRegulatePopupConfig2.KEY_PATH_FILL_LINE_SPACE,
                        gcodeFillStepPixel
                    )
                    item.updatePathFill(gcodeFillStep, renderer)
                }
            }
        }
    }
}