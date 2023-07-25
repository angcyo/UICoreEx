package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.BitmapElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas.render.util.RenderHelper
import com.angcyo.canvas2.laser.pecker.util.LPBitmapHandler
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.component.file.writeToLog
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper._defaultGCodeOutputFile
import com.angcyo.laserpacker.toGCodePath
import com.angcyo.library.LTime
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.app
import com.angcyo.library.ex.deleteSafe
import com.angcyo.library.ex.toBase64Data
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.library.ex.toSizeString
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.writeToFile

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/07
 */
class LPBitmapElement(override val elementBean: LPElementBean) : BitmapElement(),
    ILaserPeckerElement {

    /**图片转成的[Path]数据, 原始数据*/
    var pathList: List<Path>? = null

    override fun updateBeanToElement(renderer: BaseRenderer?) {
        super.updateBeanToElement(renderer)
        parseElementBean()
    }

    override fun createStateStack(): IStateStack = LPBitmapStateStack()

    override fun getDrawPathList(): List<Path>? = pathList

    /**图片的像素大小和元素描述的大小不一致, 这里需要在绘制的时候, 额外放大一下*/
    private val bitmapMatrix = Matrix()

    override fun getRenderBitmapMatrix(bitmap: Bitmap): Matrix? {
        return if (elementBean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
            null
        } else {
            val sx = elementBean._width / bitmap.width.toFloat()
            val sy = elementBean._height / bitmap.height.toFloat()
            if (sx != 0f && sy != 0f) {
                bitmapMatrix.setScale(sx, sy)
            } else {
                bitmapMatrix.reset()
            }
            bitmapMatrix
        }
    }

    /**[originBitmap]*/
    fun getRenderOriginBitmap(): Bitmap? {
        val bitmap = originBitmap ?: return null
        val matrix = getRenderBitmapMatrix(bitmap) ?: return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun requestElementDrawable(
        renderer: BaseRenderer?,
        renderParams: RenderParams?
    ): Drawable? {
        if (getDrawBitmap() == null && getDrawPathList() == null) {
            return null
        }
        return createPictureDrawable(renderParams) {
            onRenderInside(renderer, this, renderParams ?: RenderParams())
        }
    }

    override fun onRenderInside(renderer: BaseRenderer?, canvas: Canvas, params: RenderParams) {
        if (elementBean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            params.updateDrawPathPaintStrokeWidth(paint)
            renderPath(canvas, paint, false, getDrawPathList(), params._renderMatrix)
            return
        }
        val bitmap = renderBitmap ?: originBitmap
        if (bitmap == null) {
            parseElementBean()
        }
        super.onRenderInside(renderer, canvas, params)
    }

    override fun parseElementBean() {
        if (originBitmap == null) {
            originBitmap =
                elementBean.imageOriginal?.toBitmapOfBase64()?.apply {
                    updateBeanWidthHeight(width.toFloat(), height.toFloat())
                }
        }
        if (renderBitmap == null) {
            if (elementBean.imageFilter != LPDataConstant.DATA_MODE_GCODE) {
                //非GCode算法
                val bitmap = originBitmap
                if (elementBean.src.isNullOrBlank()) {
                    //滤镜图片为空, 则自动运用算法
                    if (bitmap != null) {
                        LTime.tick()
                        renderBitmap = when (elementBean.imageFilter) {
                            LPDataConstant.DATA_MODE_PRINT -> LPBitmapHandler.toPrint(
                                app(),
                                bitmap,
                                elementBean.printsThreshold
                            )

                            LPDataConstant.DATA_MODE_BLACK_WHITE -> LPBitmapHandler.toBlackWhiteHandle(
                                bitmap,
                                elementBean
                            )

                            LPDataConstant.DATA_MODE_DITHERING -> LPBitmapHandler.toGrayHandle(
                                bitmap,
                                elementBean
                            )

                            LPDataConstant.DATA_MODE_GREY -> LPBitmapHandler.toGrayHandle(
                                bitmap,
                                elementBean
                            )

                            LPDataConstant.DATA_MODE_SEAL -> LPBitmapHandler.toSealHandle(
                                bitmap,
                                elementBean.sealThreshold
                            )

                            else -> null
                        }
                        "图片[${bitmap.byteCount.toSizeString()}]算法处理[${elementBean.imageFilter}]耗时:${LTime.time()}".writePerfLog()
                    }
                } else {
                    renderBitmap = elementBean.src?.toBitmapOfBase64()
                }
            }
        }
        if (pathList == null) {
            if (elementBean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
                //GCode数据
                var gcode = elementBean.data ?: elementBean.src
                val bitmap = originBitmap
                if (gcode.isNullOrBlank() && bitmap != null) {
                    //如果未保存GCode数据, 则自动应用算法

                    LTime.tick()
                    val gcodeFile = LPBitmapHandler.toGCode(app(), bitmap, elementBean)
                    "图片[${
                        bitmap.byteCount.toSizeString()
                    }]转GCode耗时:${LTime.time()}".writePerfLog()
                    val gCodeText = gcodeFile.readText()
                    gcodeFile.deleteSafe()
                    elementBean.data = gCodeText
                    gcode = gCodeText

                    gCodeText.writeToFile(_defaultGCodeOutputFile())
                }

                //再次判断
                if (gcode.isNullOrBlank()) {
                    "GCode数据为空, 无法渲染...".writeToLog()
                } else {
                    LTime.tick()
                    /*val result = GCodeHelper.parseGCode(gcode)
                    result?.gCodePath?.let { pathList = listOf(it) }*/
                    gcode.toGCodePath()?.let { pathList = listOf(it) }
                    "解析GCode数据[${gcode.length.toSizeString()}]耗时:${LTime.time()}".writePerfLog()
                }
            }
        }
    }

    /**更新GCode数据*/
    fun updateOriginBitmapGCode(
        pathList: List<Path>?,
        gcode: String?,
        keepVisibleSize: Boolean = true
    ) {
        elementBean.imageFilter = LPDataConstant.DATA_MODE_GCODE
        elementBean.data = gcode
        this.pathList = pathList
        val bounds = RenderHelper.computePathBounds(pathList)
        updateOriginWidthHeight(bounds.width(), bounds.height(), keepVisibleSize)
    }

    /**更新原始图片的宽高
     * [updateOriginWidthHeight]*/
    @Pixel
    fun updateOriginBitmapWidthHeight(
        newWidth: Int,
        newHeight: Int,
        keepVisibleSize: Boolean = true
    ) {
        if (elementBean.mtype == LPDataConstant.DATA_TYPE_BITMAP) {
            updateOriginWidthHeight(
                newWidth.toPixel(),
                newHeight.toPixel(),
                keepVisibleSize
            )
        } else {
            updateOriginWidthHeight(
                newWidth.toFloat(),
                newHeight.toFloat(),
                keepVisibleSize
            )
        }
    }

    override fun updateOriginBitmap(bitmap: Bitmap, keepVisibleSize: Boolean) {
        this.originBitmap = bitmap
        updateOriginBitmapWidthHeight(
            bitmap.width,
            bitmap.height,
            keepVisibleSize
        )
        //更新原图, 默认是黑白画处理

        ILaserPeckerElement.updateElementBeanWidthHeight(
            elementBean,
            bitmap.width.toFloat(),
            bitmap.height.toFloat()
        )

        //2023-5-8 移除默认处理
        elementBean.src = null
        elementBean.data = null
        renderBitmap = null
        pathList = null
        //elementBean.imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE
        //renderBitmap = LPBitmapHandler.toBlackWhiteHandle(bitmap, elementBean)
        parseElementBean()
        if (elementBean.imageFilter == LPDataConstant.DATA_MODE_GCODE) {
            val bounds = RenderHelper.computePathBounds(pathList)
            updateOriginWidthHeight(bounds.width(), bounds.height(), keepVisibleSize)
        }
    }

    /**更新原始图片, 并且自动处理成默认的黑白数据, 以及转成对应的base64数据*/
    @Deprecated("V2工程结构, 不再使用BASE64数据")
    fun updateOriginBitmapSrc(
        delegate: CanvasRenderDelegate?,
        renderer: BaseRenderer,
        bitmap: Bitmap,
        keepVisibleSize: Boolean = true
    ) {
        updateOriginBitmap(bitmap, keepVisibleSize)
        delegate?.asyncManager?.addAsyncTask(renderer) {
            elementBean.imageOriginal = bitmap.toBase64Data()
        }
    }
}