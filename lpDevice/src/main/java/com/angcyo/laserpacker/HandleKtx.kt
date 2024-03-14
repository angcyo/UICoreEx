package com.angcyo.laserpacker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.gcode.GCodeHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.L
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.lastContext
import com.angcyo.library.component.pool.acquireTempPointF
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.computePathBounds
import com.angcyo.library.ex.createPaint
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.getScaleX
import com.angcyo.library.ex.getScaleY
import com.angcyo.library.ex.getSkewX
import com.angcyo.library.ex.getSkewY
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.mapPoint
import com.angcyo.library.ex.size
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
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/12
 */

typealias ElementApplyMatrix = (elementList: List<LPElementBean>?, matrix: Matrix?) -> Unit

object HandleKtx {

    /**作用矩阵*/
    var onElementApplyMatrix: ElementApplyMatrix? = null
}

/**导入svg文件时, 需要放大的倍数
 * svg 中默认是像素单位 283px = 100mm
 * */
val svgScale: Float
    get() = 0.354f.toPixel()

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

/**将SVG数据, 拆成一组[LPElementBean]
 * [svgBounds] 返回svg的bounds
 *
 * ```
 * val beanList: List<LPElementBean>? = parseSvgElementList(text)
 * HandleKtx.onElementApplyMatrix?.invoke(
 *     beanList,
 *     Matrix().apply {
 *         val scale = svgScale
 *         setScale(scale, scale)
 *     }
 * )
 * ```
 *
 * ```
 * val svgBoundsData = SvgBoundsData()
 * val beanList = parseSvgElementList(text, svgBoundsData)
 * svgBoundsData.getBoundsScaleMatrix()?.let {
 *     HandleKtx.onElementApplyMatrix?.invoke(beanList, it)
 * }
 * ```
 * */
fun parseSvgElementList(
    svgText: String?,
    svgBoundsData: SvgBoundsData? = null
): List<LPElementBean>? {
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
            bean.flipX = scaleX < 0 || bean.flipX == true
            bean.flipY = scaleY < 0 || bean.flipY == true
            bean.scaleX = scaleX.absoluteValue  //flip单独控制
            bean.scaleY = scaleY.absoluteValue  //flip单独控制
            bean.skewX = skewX.toDegrees()
            bean.skewY = skewY
        }

        fun initSizeFromRect(bean: LPElementBean, rect: RectF, matrix: Matrix?) {
            rect.sort()
            bean.width = rect.width().toMm()
            bean.height = rect.height().toMm()
            if (matrix == null) {
                bean.left = rect.left.toMm()
                bean.top = rect.top.toMm()
            } else {
                val anchor = acquireTempPointF()
                anchor.set(rect.left, rect.top)
                matrix.mapPoint(anchor)
                bean.left = anchor.x.toMm()
                bean.top = anchor.y.toMm()
                anchor.release()
            }
        }

        /**处理线条元素
         * 因为编辑器不支持竖线, 所有如果是竖线, 要处理成横线
         * */
        fun handleLineElement(bean: LPElementBean) {
            /*if (bean._width == 0f && bean._height > 0) {
                //竖线
                val rotateMatrix = acquireTempMatrix()
                val anchorPoint = acquireTempPointF()
                anchorPoint.set(bean.left, bean.top)
                rotateMatrix.setRotate(-90f, bean.left, bean.top)
                rotateMatrix.mapPoint(anchorPoint)
                bean.left = anchorPoint.x
                bean.top = anchorPoint.y

                bean.width = bean.height
                bean.height = 0f
                bean.angle += 90f

                rotateMatrix.release()
                anchorPoint.release()
            }*/
        }

        /**基础的数据初始化*/
        fun LPElementBean.initBean(drawElement: DrawElement) {
            name = drawElement.dataName
            paintStyle = drawElement.paint.style.toPaintStyleInt()
            val matrix = drawElement.matrix
            qrElement(this, matrix)

            val id = drawElement.svgGroup?.id
            if (HawkEngraveKeys.enableImportSvgGroup && !id.isNullOrBlank()) {
                this.groupId = id
                groupName = this.groupId
            }
        }

        var lastElementHashCode: Int? = null
        sharp.setOnElementListener(object : SvgElementListener() {
            override fun onCanvasDraw(canvas: Canvas, drawElement: DrawElement): Boolean {
                //2023-10-30 同一个元素, 只处理一次. 修复 fill / stroke有2组数据的问题
                if (lastElementHashCode == drawElement.hashCode()) {
                    return true
                }
                lastElementHashCode = drawElement.hashCode()

                svgBoundsData?.svgRect = drawElement.svgRect ?: svgBoundsData?.svgRect
                svgBoundsData?.viewBoxStr = drawElement.viewBoxStr ?: svgBoundsData?.viewBoxStr
                svgBoundsData?.widthStr = drawElement.widthStr ?: svgBoundsData?.widthStr
                svgBoundsData?.heightStr = drawElement.heightStr ?: svgBoundsData?.heightStr
                when (drawElement.type) {
                    DrawElement.DrawType.ROUND_RECT -> LPElementBean().apply {
                        initBean(drawElement)
                        mtype = LPDataConstant.DATA_TYPE_RECT
                        (drawElement.element as? RectF)?.let { rect ->
                            initSizeFromRect(this, rect, drawElement.matrix)
                        }
                        rx = drawElement.rx.toMm()
                        ry = drawElement.ry.toMm()
                    }

                    DrawElement.DrawType.LINE -> LPElementBean().apply {
                        initBean(drawElement)
                        mtype = LPDataConstant.DATA_TYPE_SVG
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
                            path = "M${rect.left},${rect.top}L${rect.right},${rect.bottom}"
                            initSizeFromRect(this, rect, drawElement.matrix)
                            handleLineElement(this)
                        }
                    }

                    DrawElement.DrawType.OVAL -> LPElementBean().apply {
                        initBean(drawElement)
                        mtype = LPDataConstant.DATA_TYPE_OVAL
                        (drawElement.element as? RectF)?.let { rect ->
                            initSizeFromRect(this, rect, drawElement.matrix)
                        }
                    }

                    DrawElement.DrawType.PATH -> LPElementBean().apply {
                        initBean(drawElement)
                        mtype = LPDataConstant.DATA_TYPE_SVG
                        data = drawElement.data

                        val matrix = drawElement.matrix
                        (drawElement.element as? Path)?.let {
                            val rect = acquireTempRectF()
                            it.computePathBounds(rect, true)
                            initSizeFromRect(this, rect, matrix)
                            handleLineElement(this)
                            rect.release()
                        }.elseNull {
                            drawElement.pathBounds?.let { rect ->
                                initSizeFromRect(this, rect, matrix)
                                handleLineElement(this)
                            }
                        }
                    }

                    DrawElement.DrawType.TEXT -> LPElementBean().apply {
                        initBean(drawElement)
                        mtype = LPDataConstant.DATA_TYPE_TEXT
                        fontSize = drawElement.paint.textSize.toMm()

                        (drawElement.element as? Sharp.SvgHandler.SvgText)?.let { text ->
                            this.text = text.text
                            val rect = text.bounds
                            initSizeFromRect(this, rect, drawElement.matrix)
                        }
                    }

                    DrawElement.DrawType.IMAGE -> LPElementBean().apply {
                        mtype = LPDataConstant.DATA_TYPE_BITMAP
                        imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
                        blackThreshold = HawkEngraveKeys.DEFAULT_THRESHOLD
                        initBean(drawElement)

                        val matrix = drawElement.matrix
                        val bitmapMatrix = Matrix()
                        (drawElement.element as? Bitmap)?.let { bitmap ->
                            val wF = bitmap.width.toFloat()
                            val hF = bitmap.height.toFloat()
                            imageOriginal = bitmap.toBase64Data()
                            _imageOriginalBitmap = bitmap
                            val anchor = acquireTempPointF()
                            matrix?.mapPoint(anchor)
                            left = anchor.x.toMm()
                            top = anchor.y.toMm()
                            bitmapMatrix.setScale(
                                wF / wF.toPixel(),
                                hF / hF.toPixel(),
                                anchor.x,
                                anchor.y
                            )
                            anchor.release()
                        }
                        bitmapMatrix.preConcat(matrix)
                        qrElement(this, bitmapMatrix)
                    }

                    else -> null
                }?.apply {
                    this.groupId = this.groupId ?: groupId
                    result.add(this)
                }
                return true
            }
        })
        val sharpPicture = sharp.sharpPicture //触发解析
        if (isDebugType()) {
            val bitmap = sharpPicture.drawable.toBitmap()
            L.d("svg size:${bitmap?.width}x${bitmap?.height}")
        }
        result
    }
}

//---

/**文本数据转[LPElementBean]*/
fun String?.toTextElementBean(): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_TEXT
    bean.text = this
    bean.paintStyle = Paint.Style.FILL.toPaintStyleInt()
    return bean
}

/**GCode数据转[LPElementBean]*/
fun String?.toGCodeElementBean(): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_GCODE
    bean.data = this
    bean.paintStyle = Paint.Style.STROKE.toPaintStyleInt()
    return bean
}

/**SVG数据转[LPElementBean]
 * [String.toSvgElementBeanData]*/
fun String?.toSvgElementBean(): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_SVG
    bean.data = this
    bean.paintStyle = Paint.Style.STROKE.toPaintStyleInt()

    if (HawkEngraveKeys.enableImportGroup ||
        lines().size() <= HawkEngraveKeys.autoEnableImportGroupLines ||
        length <= HawkEngraveKeys.autoEnableImportGroupLength
    ) {
        val svgBoundsData = SvgBoundsData()
        val beanList = listOf(bean)
        svgBoundsData.getBoundsScaleMatrix()?.let {
            HandleKtx.onElementApplyMatrix?.invoke(beanList, it)
        }
    }
    return bean
}

/**第二版, 直接使用图片对象*/
fun Bitmap?.toBitmapElementBeanV2(
    bmpThreshold: Int? = null, //不指定阈值时, 自动从图片中获取
    invert: Boolean = false,
    imageFilter: Int? = null, //强制指定图片处理方式
): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_BITMAP
    bean._imageOriginalBitmap = this
    val threshold = bmpThreshold ?: BitmapHandle.getBitmapThreshold(this)
    HawkEngraveKeys.lastBWThreshold = threshold.toFloat()
    bean.blackThreshold = HawkEngraveKeys.lastBWThreshold
    bean.sealThreshold = bean.blackThreshold
    bean.printsThreshold = bean.blackThreshold
    bean.scaleX = 1 / 1f.toPixel()
    bean.scaleY = bean.scaleX

    //bean.imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
    //bean._srcBitmap = BitmapHandle.toBlackWhiteHandle(this, HawkEngraveKeys.lastBWThreshold.toInt(), invert)
    // 2024-3-11 默认抖动处理
    bean.imageFilter = imageFilter ?: LPDataConstant.DATA_MODE_DITHERING //默认黑白处理
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