package com.angcyo.canvas.laser.pecker

import android.graphics.RectF
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.data.toMm
import com.angcyo.canvas.items.data.DataBitmapItem
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.parseGCode
import com.angcyo.core.component.file.writeToCache
import com.angcyo.crop.ui.cropDialog
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.loadingAsync
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.rotate
import com.angcyo.library.ex.toBase64Data
import com.angcyo.library.utils.fileNameTime
import com.angcyo.opencv.OpenCV
import com.hingin.rn.image.ImageProcess

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
            addRegulate(CanvasRegulatePopupConfig2.KEY_PRINT_THRESHOLD)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        operateBitmap.let { bitmap ->
                            item.dataBean.printsThreshold = getIntOrDef(
                                CanvasRegulatePopupConfig2.KEY_PRINT_THRESHOLD,
                                item.dataBean.printsThreshold.toInt()
                            ).toFloat()
                            OpenCV.bitmapToPrint(
                                context,
                                bitmap,
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
            addRegulate(CanvasRegulatePopupConfig2.KEY_LINE_SPACE)
            addRegulate(CanvasRegulatePopupConfig2.KEY_ANGLE)
            addRegulate(CanvasRegulatePopupConfig2.KEY_DIRECTION)
            addRegulate(CanvasRegulatePopupConfig2.KEY_SUBMIT)

            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        val direction = getIntOrDef(
                            CanvasRegulatePopupConfig2.KEY_DIRECTION,
                            item.dataBean.gcodeDirection
                        )
                        if (direction == 1 || direction == 3) {
                            boundsRotate = 90f
                        }
                        item.dataBean.gcodeDirection = direction

                        val lineSpace = getFloatOrDef(
                            CanvasRegulatePopupConfig2.KEY_LINE_SPACE,
                            item.dataBean.gcodeLineSpace
                        )
                        item.dataBean.gcodeLineSpace = lineSpace

                        val angle = getFloatOrDef(
                            CanvasRegulatePopupConfig2.KEY_ANGLE,
                            item.dataBean.gcodeAngle
                        )
                        item.dataBean.gcodeAngle = angle

                        operateBitmap.let { bitmap ->
                            OpenCV.bitmapToGCode(
                                context,
                                bitmap,
                                (bitmap.width / 2).toMm().toDouble(),
                                lineSpace = lineSpace.toDouble(),
                                direction = direction,
                                angle = angle.toDouble()
                            ).let {
                                val gCodeText = it.readText()
                                it.deleteSafe()
                                gCodeText to GCodeHelper.parseGCode(gCodeText)
                            }
                        }
                    }) {
                        it?.let {
                            it.first.writeToCache(
                                CanvasConstant.VECTOR_FILE_FOLDER,
                                fileNameTime(suffix = ".gcode")
                            )
                            beforeBounds.rotate(boundsRotate)
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
            addRegulate(CanvasRegulatePopupConfig2.KEY_BW_INVERT)
            addRegulate(CanvasRegulatePopupConfig2.KEY_BW_THRESHOLD)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        operateBitmap.let { bitmap ->

                            item.dataBean.blackThreshold = getIntOrDef(
                                CanvasRegulatePopupConfig2.KEY_BW_THRESHOLD,
                                item.dataBean.blackThreshold.toInt()
                            ).toFloat()

                            item.dataBean.inverse = getBooleanOrDef(
                                CanvasRegulatePopupConfig2.KEY_BW_INVERT, item.dataBean.inverse
                            )

                            OpenCV.bitmapToBlackWhite(
                                bitmap,
                                item.dataBean.blackThreshold.toInt(),
                                if (item.dataBean.inverse) 1 else 0
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

    /**抖动*/
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
            addRegulate(CanvasRegulatePopupConfig2.KEY_SHAKE_INVERT)
            addRegulate(CanvasRegulatePopupConfig2.KEY_CONTRAST)
            addRegulate(CanvasRegulatePopupConfig2.KEY_BRIGHTNESS)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
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

                            OpenCV.bitmapToDithering(
                                context,
                                bitmap,
                                item.dataBean.inverse,
                                item.dataBean.contrast.toDouble(),
                                item.dataBean.brightness.toDouble(),
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
        renderer: DataItemRenderer
    ) {
        val item = renderer.getRendererRenderItem() as? DataBitmapItem ?: return
        val context = anchor.context
        val operateBitmap = item.operateBitmap!!

        owner.loadingAsync({
            operateBitmap.let { bitmap ->
                OpenCV.bitmapToGrey(bitmap)
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
            addRegulate(CanvasRegulatePopupConfig2.KEY_SEAL_THRESHOLD)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        item.dataBean.sealThreshold = getIntOrDef(
                            CanvasRegulatePopupConfig2.KEY_SEAL_THRESHOLD,
                            item.dataBean.sealThreshold.toInt()
                        ).toFloat()
                        operateBitmap.let { bitmap ->
                            OpenCV.bitmapToSeal(
                                context,
                                bitmap,
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

        anchor.context.cropDialog {
            cropBitmap = originBitmap
            onDismissListener = {
                onDismissAction()
            }

            onCropResultAction = {
                it?.let {
                    owner.loadingAsync({
                        //剪切完之后, 默认背白处理
                        val filter = it.toBlackWhiteBitmap(item.dataBean.blackThreshold.toInt())
                        item.updateBitmapOriginal(
                            it.toBase64Data(),
                            filter,
                            CanvasConstant.DATA_MODE_BLACK_WHITE,
                            renderer
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
            addRegulate(CanvasRegulatePopupConfig2.KEY_MESH_SHAPE)
            addRegulate(CanvasRegulatePopupConfig2.KEY_MIN_DIAMETER)
            addRegulate(CanvasRegulatePopupConfig2.KEY_MAX_DIAMETER)
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
                    owner.loadingAsync({
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
                            owner.loadingAsync({
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
}