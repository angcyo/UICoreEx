package com.angcyo.canvas.laser.pecker

import android.graphics.Color
import android.graphics.RectF
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.items.PictureBitmapItem
import com.angcyo.canvas.items.renderer.PictureItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.canvas.utils.createPaint
import com.angcyo.canvas.utils.toMm
import com.angcyo.core.component.file.writeToCache
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
        renderer: PictureItemRenderer<PictureBitmapItem>
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_THRESHOLD)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    renderer.updateRendererItem(item, beforeBounds, Strategy.redo)
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap.let { bitmap ->
                                OpenCV.bitmapToPrint(
                                    context,
                                    bitmap,
                                    getIntOrDef(
                                        CanvasRegulatePopupConfig.KEY_THRESHOLD, 240
                                    )
                                )
                            }
                        }) {
                            it?.let {
                                newItem = PictureBitmapItem(originBitmap, it)
                                newItem?.dataMode = CanvasConstant.DATA_MODE_PRINT

                                renderer.updateRendererItem(
                                    newItem!!,
                                    beforeBounds,
                                    if (preview) Strategy.preview else Strategy.normal
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        newItem?.let {
                            renderer.rendererItem = item
                            renderer.updateRendererItem(it, beforeBounds, Strategy.normal)
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
        renderer: PictureItemRenderer<PictureBitmapItem>
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        var boundsRotate = 0f //需要旋转的角度

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_LINE_SPACE)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_ANGLE)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_DIRECTION)
            livePreview = false

            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    renderer.updateRendererItem(item, beforeBounds, Strategy.redo)
                } else {
                    if (valueChanged) {
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
                                    gCodeText to GCodeHelper.parseGCode(
                                        gCodeText,
                                        createPaint(Color.BLACK)
                                    )
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
                                    if (preview) Strategy.preview else Strategy.normal
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        newItem?.let {
                            renderer.rendererItem = item
                            renderer.updateRendererItem(
                                it,
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
        renderer: PictureItemRenderer<PictureBitmapItem>
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_INVERT)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_THRESHOLD)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    renderer.updateRendererItem(item, beforeBounds, Strategy.redo)
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap.let { bitmap ->
                                OpenCV.bitmapToBlackWhite(
                                    bitmap,
                                    getIntOrDef(
                                        CanvasRegulatePopupConfig.KEY_THRESHOLD, 240
                                    ),
                                    if (getBooleanOrDef(
                                            CanvasRegulatePopupConfig.KEY_INVERT, false
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

                                renderer.updateRendererItem(
                                    newItem!!,
                                    beforeBounds,
                                    if (preview) Strategy.preview else Strategy.normal
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        newItem?.let {
                            renderer.rendererItem = item
                            renderer.updateRendererItem(it, beforeBounds, Strategy.normal)
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
        renderer: PictureItemRenderer<PictureBitmapItem>
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_INVERT)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_CONTRAST)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_BRIGHTNESS)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    renderer.updateRendererItem(item, beforeBounds, Strategy.redo)
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap.let { bitmap ->
                                OpenCV.bitmapToDithering(
                                    context, bitmap,
                                    getBooleanOrDef(
                                        CanvasRegulatePopupConfig.KEY_INVERT, false
                                    ),
                                    getFloatOrDef(
                                        CanvasRegulatePopupConfig.KEY_CONTRAST, 0f
                                    ).toDouble(),
                                    getFloatOrDef(
                                        CanvasRegulatePopupConfig.KEY_BRIGHTNESS, 0f
                                    ).toDouble(),
                                )
                            }
                        }) {
                            it?.let {
                                newItem = PictureBitmapItem(originBitmap, it)
                                newItem?.dataMode = CanvasConstant.DATA_MODE_DITHERING

                                renderer.updateRendererItem(
                                    newItem!!,
                                    beforeBounds,
                                    if (preview) Strategy.preview else Strategy.normal
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        newItem?.let {
                            renderer.rendererItem = item
                            renderer.updateRendererItem(it, beforeBounds, Strategy.normal)
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
        renderer: PictureItemRenderer<PictureBitmapItem>
    ) {
        val item = renderer.getRendererRenderItem() ?: return
        val context = anchor.context
        val originBitmap = item.originBitmap
        val beforeBounds = RectF(renderer.getBounds())

        var newItem: PictureBitmapItem? = null

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_THRESHOLD)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    renderer.updateRendererItem(item, beforeBounds, Strategy.redo)
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap.let { bitmap ->
                                OpenCV.bitmapToSeal(
                                    context, bitmap,
                                    getIntOrDef(
                                        CanvasRegulatePopupConfig.KEY_THRESHOLD, 240
                                    )
                                )
                            }
                        }) {
                            it?.let {
                                newItem = PictureBitmapItem(originBitmap, it)
                                newItem?.dataMode = CanvasConstant.DATA_MODE_SEAL

                                renderer.updateRendererItem(
                                    newItem!!,
                                    beforeBounds,
                                    if (preview) Strategy.preview else Strategy.normal
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        newItem?.let {
                            renderer.rendererItem = item
                            renderer.updateRendererItem(it, beforeBounds, Strategy.normal)
                        }
                    }
                }
            }
        }
    }
}