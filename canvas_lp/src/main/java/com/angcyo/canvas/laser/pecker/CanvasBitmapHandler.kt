package com.angcyo.canvas.laser.pecker

import android.graphics.RectF
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.items.PictureBitmapItem
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.parseGCode
import com.angcyo.canvas.utils.toMm
import com.angcyo.core.component.file.writeToCache
import com.angcyo.crop.ui.cropDialog
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.rotate
import com.angcyo.library.utils.fileName
import com.angcyo.opencv.OpenCV

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
        renderer: PictureItemRenderer<PictureBitmapItem>,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        originBitmap.let { bitmap ->
                            OpenCV.bitmapToPrint(
                                context,
                                bitmap,
                                getIntOrDef(CanvasRegulatePopupConfig.KEY_PRINT_THRESHOLD, 240)
                            )
                        }
                    }) {
                        it?.let {
                            newItem = PictureBitmapItem(originBitmap, it)
                            newItem?.dataMode = CanvasConstant.DATA_MODE_PRINT

                            renderer.updateRendererItem(newItem!!, beforeBounds)
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
        renderer: PictureItemRenderer<PictureBitmapItem>,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        var boundsRotate = 0f //需要旋转的角度

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_LINE_SPACE)
            addRegulate(CanvasRegulatePopupConfig.KEY_ANGLE)
            addRegulate(CanvasRegulatePopupConfig.KEY_DIRECTION)
            addRegulate(CanvasRegulatePopupConfig.KEY_SUBMIT)

            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        val direction = getIntOrDef(CanvasRegulatePopupConfig.KEY_DIRECTION, 0)
                        //keepBounds = direction == 0 || direction == 2
                        if (direction == 1 || direction == 3) {
                            boundsRotate = 90f
                        }
                        originBitmap.let { bitmap ->
                            OpenCV.bitmapToGCode(
                                context,
                                bitmap,
                                (bitmap.width / 2).toMm().toDouble(),
                                lineSpace = getFloatOrDef(
                                    CanvasRegulatePopupConfig.KEY_LINE_SPACE, 0.125f
                                ).toDouble(),
                                direction = direction,
                                angle = getFloatOrDef(
                                    CanvasRegulatePopupConfig.KEY_ANGLE, 0f
                                ).toDouble()
                            ).let {
                                val gCodeText = it.readText()
                                it.deleteSafe()
                                gCodeText to GCodeHelper.parseGCode(gCodeText)
                            }
                        }
                    }) {
                        it?.let {
                            it.first.writeToCache(
                                CanvasDataHandleOperate.GCODE_CACHE_FILE_FOLDER,
                                fileName(suffix = ".gcode")
                            )

                            newItem = PictureBitmapItem(originBitmap, null, it.second)
                            newItem?.data = it.first // GCode数据放这里
                            newItem?.dataMode = CanvasConstant.DATA_MODE_GCODE

                            renderer.updateRendererItem(
                                newItem!!,
                                beforeBounds.rotate(boundsRotate, result = RectF()),
                                Strategy.normal
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
        renderer: PictureItemRenderer<PictureBitmapItem>,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_BW_INVERT)
            addRegulate(CanvasRegulatePopupConfig.KEY_BW_THRESHOLD)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        originBitmap.let { bitmap ->
                            OpenCV.bitmapToBlackWhite(
                                bitmap,
                                getIntOrDef(CanvasRegulatePopupConfig.KEY_BW_THRESHOLD, 240),
                                if (getBooleanOrDef(
                                        CanvasRegulatePopupConfig.KEY_BW_INVERT, false
                                    )
                                ) {
                                    1
                                } else {
                                    0
                                }
                            )
                        }
                    }) {
                        it?.let {
                            newItem = PictureBitmapItem(originBitmap, it)
                            newItem?.dataMode = CanvasConstant.DATA_MODE_BLACK_WHITE

                            renderer.updateRendererItem(newItem!!, beforeBounds, Strategy.normal)
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
        renderer: PictureItemRenderer<PictureBitmapItem>,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT)
            addRegulate(CanvasRegulatePopupConfig.KEY_CONTRAST)
            addRegulate(CanvasRegulatePopupConfig.KEY_BRIGHTNESS)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        originBitmap.let { bitmap ->
                            OpenCV.bitmapToDithering(
                                context,
                                bitmap,
                                getBooleanOrDef(CanvasRegulatePopupConfig.KEY_SHAKE_INVERT, false),
                                getFloatOrDef(
                                    CanvasRegulatePopupConfig.KEY_CONTRAST,
                                    0f
                                ).toDouble(),
                                getFloatOrDef(
                                    CanvasRegulatePopupConfig.KEY_BRIGHTNESS,
                                    0f
                                ).toDouble(),
                            )
                        }
                    }) {
                        it?.let {
                            newItem = PictureBitmapItem(originBitmap, it)
                            newItem?.dataMode = CanvasConstant.DATA_MODE_DITHERING

                            renderer.updateRendererItem(newItem!!, beforeBounds, Strategy.normal)
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
        renderer: PictureItemRenderer<PictureBitmapItem>
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        owner.loadingAsync({
            originBitmap.let { bitmap ->
                OpenCV.bitmapToGrey(bitmap)
            }
        }) {
            it?.let {
                newItem = PictureBitmapItem(originBitmap, it)
                newItem?.dataMode = CanvasConstant.DATA_MODE_GREY

                renderer.updateRendererItem(newItem!!, beforeBounds, Strategy.normal)
            }
        }
    }

    /**印章*/
    fun handleSeal(
        anchor: View,
        owner: LifecycleOwner,
        renderer: PictureItemRenderer<PictureBitmapItem>,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow2(anchor) {
            addRegulate(CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD)
            onApplyAction = { dismiss ->
                if (dismiss) {
                    onDismissAction()
                } else {
                    owner.loadingAsync({
                        originBitmap.let { bitmap ->
                            OpenCV.bitmapToSeal(
                                context,
                                bitmap,
                                getIntOrDef(CanvasRegulatePopupConfig.KEY_SEAL_THRESHOLD, 240)
                            )
                        }
                    }) {
                        it?.let {
                            newItem = PictureBitmapItem(originBitmap, it)
                            newItem?.dataMode = CanvasConstant.DATA_MODE_SEAL

                            renderer.updateRendererItem(newItem!!, beforeBounds, Strategy.normal)
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
        renderer: PictureItemRenderer<PictureBitmapItem>,
        onDismissAction: () -> Unit = {}
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null
        anchor.context.cropDialog {
            cropBitmap = originBitmap
            onDismissListener = {
                onDismissAction()
            }

            onCropResultAction = {
                it?.let {
                    newItem = PictureBitmapItem(originBitmap, it)
                    newItem?.dataMode = CanvasConstant.DATA_MODE_GREY

                    renderer.updateRendererItem(newItem!!, beforeBounds, Strategy.normal)
                }
            }
        }
    }
}