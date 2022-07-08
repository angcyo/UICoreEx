package com.angcyo.canvas.laser.pecker

import android.graphics.Bitmap
import android.graphics.RectF
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.items.renderer.IItemRenderer
import com.angcyo.canvas.utils.*
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.rotate
import com.angcyo.opencv.OpenCV

/**
 * 图片编辑处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/15
 */
object CanvasBitmapHandler {

    /**版画*/
    fun handlePrint(anchor: View, owner: LifecycleOwner, renderer: IItemRenderer<*>) {
        val context = anchor.context
        val originBitmap = renderer.getRenderBitmap()
        val beforeBitmap = renderer.getRenderBitmap(false)
        var result: Bitmap? = null
        val beforeBounds = RectF(renderer.getBounds())
        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_THRESHOLD)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    beforeBitmap?.let {
                        renderer.updateRenderBitmap(it, Strategy.redo, keepBounds = beforeBounds)
                    }
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap?.let { bitmap ->
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
                                result = it
                                renderer.updateRenderBitmap(
                                    it,
                                    if (preview) Strategy.preview else Strategy.normal,
                                    keepBounds = beforeBounds,
                                    holdData = hashMapOf(CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_PRINT)
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        result?.let {
                            renderer.onlySetRenderBitmap(beforeBitmap)
                            renderer.updateRenderBitmap(
                                it,
                                Strategy.normal,
                                keepBounds = beforeBounds,
                                holdData = hashMapOf(CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_PRINT)
                            )
                        }
                    }
                }
            }
        }
    }

    /**GCode*/
    fun handleGCode(anchor: View, owner: LifecycleOwner, renderer: IItemRenderer<*>) {
        val context = anchor.context
        val originBitmap = renderer.getRenderBitmap()
        val beforeBitmap = renderer.getRenderBitmap(false)
        val beforeDrawable = renderer.getRenderDrawable()

        var result: Pair<String?, GCodeDrawable?>? = null
        var boundsRotate = 0f //需要旋转的角度
        val beforeBounds = RectF(renderer.getBounds())

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_LINE_SPACE)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_ANGLE)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_DIRECTION)
            livePreview = false

            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    beforeBitmap?.let {
                        renderer.updateRenderBitmap(it, Strategy.redo, keepBounds = beforeBounds)
                    }
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            val direction = getIntOrDef(CanvasRegulatePopupConfig.KEY_DIRECTION, 0)
                            //keepBounds = direction == 0 || direction == 2
                            if (direction == 1 || direction == 3) {
                                boundsRotate = 90f
                            }
                            originBitmap?.let { bitmap ->
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
                                result = it
                                renderer.updateItemDrawable(
                                    it.second,
                                    if (preview) Strategy.preview else Strategy.normal,
                                    beforeBounds.rotate(boundsRotate),
                                    hashMapOf(
                                        CanvasDataHandleOperate.KEY_GCODE to it.first,
                                        CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_GCODE
                                    ),
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        result?.let {
                            renderer.onlySetRenderDrawable(beforeDrawable)
                            renderer.updateItemDrawable(
                                it.second,
                                Strategy.normal,
                                beforeBounds.rotate(boundsRotate),
                                hashMapOf(
                                    CanvasDataHandleOperate.KEY_GCODE to it.first,
                                    CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_GCODE
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    /**黑白画*/
    fun handleBlackWhite(anchor: View, owner: LifecycleOwner, renderer: IItemRenderer<*>) {
        val context = anchor.context
        val originBitmap = renderer.getRenderBitmap()
        val beforeBitmap = renderer.getRenderBitmap(false)

        var result: Bitmap? = null
        val beforeBounds = RectF(renderer.getBounds())

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_INVERT)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_THRESHOLD)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    beforeBitmap?.let {
                        renderer.updateRenderBitmap(it, Strategy.redo, keepBounds = beforeBounds)
                    }
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap?.let { bitmap ->
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
                                result = it
                                renderer.updateRenderBitmap(
                                    it,
                                    if (preview) Strategy.preview else Strategy.normal,
                                    keepBounds = beforeBounds,
                                    holdData = hashMapOf(
                                        CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_BLACK_WHITE
                                    )
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        result?.let {
                            renderer.onlySetRenderBitmap(beforeBitmap)
                            renderer.updateRenderBitmap(
                                it, Strategy.normal,
                                keepBounds = beforeBounds,
                                holdData = hashMapOf(
                                    CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_BLACK_WHITE
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**抖动*/
    fun handleDithering(anchor: View, owner: LifecycleOwner, renderer: IItemRenderer<*>) {
        val context = anchor.context

        val originBitmap = renderer.getRenderBitmap()
        val beforeBitmap = renderer.getRenderBitmap(false)

        var result: Bitmap? = null
        val beforeBounds = RectF(renderer.getBounds())

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_INVERT)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_CONTRAST)
            addRegulate(CanvasRegulatePopupConfig.REGULATE_BRIGHTNESS)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    beforeBitmap?.let {
                        renderer.updateRenderBitmap(it, Strategy.redo, keepBounds = beforeBounds)
                    }
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap?.let { bitmap ->
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
                                result = it
                                renderer.updateRenderBitmap(
                                    it,
                                    if (preview) Strategy.preview else Strategy.normal,
                                    keepBounds = beforeBounds,
                                    holdData = hashMapOf(
                                        CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_DITHERING
                                    )
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        result?.let {
                            renderer.onlySetRenderBitmap(beforeBitmap)
                            renderer.updateRenderBitmap(
                                it,
                                Strategy.normal,
                                keepBounds = beforeBounds,
                                holdData = hashMapOf(
                                    CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_DITHERING
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /**灰度*/
    fun handleGrey(anchor: View, owner: LifecycleOwner, renderer: IItemRenderer<*>) {
        val beforeBounds = RectF(renderer.getBounds())
        owner.loadingAsync({
            renderer.getRenderBitmap()?.let { bitmap ->
                OpenCV.bitmapToGrey(bitmap)
            }
        }) {
            it?.let {
                renderer.updateRenderBitmap(
                    it,
                    keepBounds = beforeBounds,
                    holdData = hashMapOf(
                        CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_GREY
                    )
                )
            }
        }
    }

    /**印章*/
    fun handleSeal(anchor: View, owner: LifecycleOwner, renderer: IItemRenderer<*>) {
        val context = anchor.context

        val originBitmap = renderer.getRenderBitmap()
        val beforeBitmap = renderer.getRenderBitmap(false)

        var result: Bitmap? = null
        val beforeBounds = RectF(renderer.getBounds())

        context.canvasRegulateWindow(anchor) {
            itemRenderer = renderer
            addRegulate(CanvasRegulatePopupConfig.REGULATE_THRESHOLD)
            onApplyAction = { preview, cancel, valueChanged ->
                if (cancel) {
                    beforeBitmap?.let {
                        renderer.updateRenderBitmap(it, Strategy.redo, keepBounds = beforeBounds)
                    }
                } else {
                    if (valueChanged) {
                        owner.loadingAsync({
                            originBitmap?.let { bitmap ->
                                OpenCV.bitmapToSeal(
                                    context, bitmap,
                                    getIntOrDef(
                                        CanvasRegulatePopupConfig.KEY_THRESHOLD, 240
                                    )
                                )
                            }
                        }) {
                            it?.let {
                                result = it
                                renderer.updateRenderBitmap(
                                    it,
                                    if (preview) Strategy.preview else Strategy.normal,
                                    keepBounds = beforeBounds,
                                    holdData = hashMapOf(
                                        CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_SEAL
                                    )
                                )
                            }
                        }
                    } else if (!preview) {
                        //使用上一次的结果
                        result?.let {
                            renderer.onlySetRenderBitmap(beforeBitmap)
                            renderer.updateRenderBitmap(
                                it, Strategy.normal,
                                keepBounds = beforeBounds,
                                holdData = hashMapOf(
                                    CanvasDataHandleOperate.KEY_DATA_MODE to CanvasConstant.BITMAP_MODE_SEAL
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}