package com.angcyo.laserpacker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bitmap.handle.BuildConfig
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.gcode.GCodeHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.L
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.lastContext
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.abs
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.createPaint
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.getScaleX
import com.angcyo.library.ex.getScaleY
import com.angcyo.library.ex.getSkewX
import com.angcyo.library.ex.getSkewY
import com.angcyo.library.ex.toBase64Data
import com.angcyo.library.ex.toBitmap
import com.angcyo.library.ex.toDegrees
import com.angcyo.library.ex.toDrawable
import com.angcyo.library.ex.uuid
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.toPixel
import com.angcyo.svg.DrawElement
import com.angcyo.svg.Svg
import com.angcyo.svg.SvgElementListener
import com.pixplicity.sharp.Sharp
import com.pixplicity.sharp.SharpDrawable
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/12
 */

/**扩展*/
@Deprecated("请使用性能更好的Jni方法:[String.toGCodePath]")
fun GCodeHelper.parseGCode(gCodeText: String?): GCodeDrawable? =
    parseGCode(gCodeText, createPaint(Color.BLACK))

/**将GCode字符串, 转换成Android的[Path]
 * [Path.toDrawable]*/
fun String.toGCodePath() = BitmapHandle.parseGCode(this, lastContext)

/**[toGCodePath]*/
fun String.toGCodePathDrawable(paint: Paint = createPaint(), overrideSize: Float? = null) =
    toGCodePath().toDrawable(overrideSize, paint)

/**[toGCodePath]*/
fun String.toGCodePathBitmap(
    paint: Paint = createPaint(),
    overrideSize: Float? = null,
    bgColor: Int = Color.TRANSPARENT
) = toGCodePath().toBitmap(overrideSize, paint, bgColor)

/**扩展*/
fun parseSvg(svgText: String?): SharpDrawable? = if (svgText.isNullOrEmpty()) {
    null
} else {
    Svg.loadSvgPathDrawable(svgText, -1, null, createPaint(Color.BLACK), 0, 0)
}

/**转成黑白图片*/
fun Bitmap?.toBlackWhiteBitmap(bmpThreshold: Int, invert: Boolean = false): String? {
    this ?: return null
    /*return OpenCV.bitmapToBlackWhite(
        this,
        bmpThreshold,
        if (invert) 1 else 0
    ).toBase64Data()*/
    //toBlackWhiteHandle(bmpThreshold, invert)
    val bitmap = BitmapHandle.toBlackWhiteHandle(this, bmpThreshold, invert)
    return bitmap?.toBase64Data()
}

/**将SVG数据, 拆成一组[LPElementBean]*/
fun parseSvgElementList(svgText: String?): List<LPElementBean>? {
    return if (svgText.isNullOrEmpty()) {
        null
    } else {
        val result = mutableListOf<LPElementBean>()
        val groupId = uuid()
        val sharp = Sharp.loadString(svgText)

        /**[com.angcyo.canvas.render.core.component.CanvasRenderProperty.qrDecomposition]*/
        fun qrElement(bean: LPElementBean, matrix: Matrix?) {
            matrix ?: return
            if (!matrix.isIdentity) {
                L.d("qrElement:$matrix")
            }
            val sx = matrix.getScaleX()
            val sy = matrix.getScaleY()
            val angle = atan2(matrix.getSkewY(), sx).toDegrees()
            val denom = sx.pow(2f) + matrix.getSkewY().pow(2f)

            val scaleX = sqrt(denom)
            val scaleY = (sx * sy - matrix.getSkewX() * matrix.getSkewY()) / scaleX

            val skewX =
                atan2((sx * matrix.getSkewX() + matrix.getSkewY() * sy), denom) //x倾斜的角度, 弧度单位
            val skewY = 0.0f//y倾斜的角度, 弧度单位

            bean.angle = (angle + 360) % 360
            bean.flipX = scaleX < 0
            bean.flipY = scaleY < 0
            bean.scaleX = scaleX.absoluteValue  //flip单独控制
            bean.scaleY = scaleY.absoluteValue  //flip单独控制
            bean.skewX = skewX.toDegrees()
            bean.skewY = skewY
        }

        sharp.setOnElementListener(object : SvgElementListener() {
            override fun onCanvasDraw(canvas: Canvas, drawElement: DrawElement): Boolean {
                when (drawElement.type) {
                    DrawElement.DrawType.ROUND_RECT -> LPElementBean().apply {
                        val matrix = drawElement.matrix
                        mtype = LPDataConstant.DATA_TYPE_RECT
                        name = drawElement.dataName
                        paintStyle = drawElement.paint.style.toPaintStyleInt()
                        (drawElement.element as? RectF)?.let { rect ->
                            width = rect.width().toMm()
                            height = rect.height().toMm()
                            left = rect.left.toMm()
                            top = rect.top.toMm()
                        }
                        rx = drawElement.rx.toMm()
                        ry = drawElement.ry.toMm()
                        qrElement(this, matrix)
                    }

                    DrawElement.DrawType.LINE -> LPElementBean().apply {
                        val matrix = drawElement.matrix
                        paintStyle = drawElement.paint.style.toPaintStyleInt()
                        mtype = LPDataConstant.DATA_TYPE_SVG
                        name = drawElement.dataName
                        /*mtype = LPDataConstant.DATA_TYPE_LINE*/
                        (drawElement.element as? RectF)?.let { rect ->
                            /*angle = VectorHelper.angle(
                                rect.left,
                                rect.top,
                                rect.right,
                                rect.bottom
                            )
                            width = VectorHelper.spacing(
                                rect.left,
                                rect.top,
                                rect.right,
                                rect.bottom
                            ).toMm()
                            height = 0f
                            left = rect.left.toMm()
                            top = rect.top.toMm()*/
                            val l = min(rect.left, rect.right)
                            val t = min(rect.top, rect.bottom)

                            path = "M${rect.left},${rect.top}L${rect.right},${rect.bottom}"
                            width = rect.width().abs().toMm()
                            height = rect.height().abs().toMm()
                            left = l.toMm()
                            top = t.toMm()
                        }
                        qrElement(this, matrix)
                    }

                    DrawElement.DrawType.OVAL -> LPElementBean().apply {
                        val matrix = drawElement.matrix
                        mtype = LPDataConstant.DATA_TYPE_OVAL
                        name = drawElement.dataName
                        paintStyle = drawElement.paint.style.toPaintStyleInt()
                        (drawElement.element as? RectF)?.let { rect ->
                            width = rect.width().toMm()
                            height = rect.height().toMm()
                            left = rect.left.toMm()
                            top = rect.top.toMm()
                        }
                        qrElement(this, matrix)
                    }

                    DrawElement.DrawType.PATH -> LPElementBean().apply {
                        val matrix = drawElement.matrix
                        mtype = LPDataConstant.DATA_TYPE_SVG
                        name = drawElement.dataName
                        paintStyle = drawElement.paint.style.toPaintStyleInt()
                        data = drawElement.data

                        (drawElement.element as? Path)?.run {
                            if (matrix != null)
                                Path(this).apply { transform(matrix) }
                            else this
                        }?.let {
                            val rect = acquireTempRectF()
                            it.computePathBounds(rect, true)
                            width = rect.width().toMm()
                            height = rect.height().toMm()
                            left = rect.left.toMm()
                            top = rect.top.toMm()
                            rect.release()
                        }.elseNull {
                            drawElement.pathBounds?.let { rect ->
                                width = rect.width().toMm()
                                height = rect.height().toMm()
                                left = rect.left.toMm()
                                top = rect.top.toMm()
                            }
                        }

                        qrElement(this, matrix)
                    }

                    DrawElement.DrawType.TEXT -> LPElementBean().apply {
                        val matrix = drawElement.matrix
                        mtype = LPDataConstant.DATA_TYPE_TEXT
                        name = drawElement.dataName
                        paintStyle = drawElement.paint.style.toPaintStyleInt()
                        fontSize = drawElement.paint.textSize.toMm()

                        (drawElement.element as? Sharp.SvgHandler.SvgText)?.let { text ->
                            this.text = text.text
                            val rect = text.bounds
                            left = rect.left.toMm()
                            top = rect.top.toMm()
                        }
                        qrElement(this, matrix)
                    }

                    else -> null
                }?.apply {
                    this.groupId = groupId
                    result.add(this)
                }
                return true
            }
        })
        val sharpPicture = sharp.sharpPicture //触发解析
        if (BuildConfig.DEBUG) {
            val bitmap = sharpPicture.drawable.toBitmap()
            L.d("svg size:${bitmap?.width}x${bitmap?.height}")
        }
        result
    }
}

//---

/**GCode数据转[LPElementBean]*/
fun String?.toGCodeElementBean(): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_GCODE
    bean.data = this
    bean.paintStyle = Paint.Style.STROKE.toPaintStyleInt()
    return bean
}

/**SVG数据转[LPElementBean]*/
fun String?.toSvgElementBean(): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_SVG
    bean.data = this
    bean.paintStyle = Paint.Style.STROKE.toPaintStyleInt()
    return bean
}

/**第二版, 直接使用图片对象*/
fun Bitmap?.toBitmapElementBeanV2(
    bmpThreshold: Int? = null, //不指定阈值时, 自动从图片中获取
    invert: Boolean = false
): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_BITMAP
    bean.imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
    bean._imageOriginalBitmap = this
    val threshold = bmpThreshold ?: BitmapHandle.getBitmapThreshold(this)
    HawkEngraveKeys.lastBWThreshold = threshold.toFloat()
    bean.blackThreshold = HawkEngraveKeys.lastBWThreshold
    bean.sealThreshold = bean.blackThreshold
    bean.printsThreshold = bean.blackThreshold
    bean._srcBitmap =
        BitmapHandle.toBlackWhiteHandle(this, HawkEngraveKeys.lastBWThreshold.toInt(), invert)
    bean.scaleX = 1 / 1f.toPixel()
    bean.scaleY = bean.scaleX
    return bean
}

/**第二版, 直接使用图片对象*/
fun List<Bitmap>?.toBitmapElementBeanListV2(
    bmpThreshold: Int? = null, //不指定阈值时, 自动从图片中获取
    invert: Boolean = false
): List<LPElementBean>? {
    this ?: return null
    val result = mutableListOf<LPElementBean>()

    @MM
    var top = 0f
    forEach {
        it.toBitmapElementBeanV2(bmpThreshold, invert)?.let { bean ->
            bean.width = it.width.toFloat()
            bean.height = it.height.toFloat()
            bean.top = top
            top += it.height.toMm()
            result.add(bean)
        }
    }
    return result
}