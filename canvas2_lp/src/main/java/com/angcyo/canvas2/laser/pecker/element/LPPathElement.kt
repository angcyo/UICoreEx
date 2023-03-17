package com.angcyo.canvas2.laser.pecker.element

import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import com.angcyo.canvas.render.data.RenderParams
import com.angcyo.canvas.render.element.PathElement
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.state.IStateStack
import com.angcyo.canvas.render.util.scaleToSize
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.canvas2.laser.pecker.util.toPaintStyle
import com.angcyo.canvas2.laser.pecker.util.toPaintStyleInt
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex.toRadians
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.isSvgContent
import com.angcyo.svg.Svg
import com.angcyo.vector.VectorWriteHandler
import com.pixplicity.sharp.Sharp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/16
 */
class LPPathElement(override val elementBean: LPElementBean) : PathElement(), ILaserPeckerElement {

    companion object {

        /**默认的形状宽度, cm单位*/
        @MM
        const val SHAPE_DEFAULT_WIDTH = 10f

        /**默认的形状高度, cm单位*/
        @MM
        const val SHAPE_DEFAULT_HEIGHT = 10f

        /**根据类型, 创建简单的形状[Path]*/
        fun createPath(bean: LPElementBean): Path? {
            val width = bean.width.toPixel()
            val height = bean.height.toPixel()
            return when (bean.mtype) {
                //线
                LPConstant.DATA_TYPE_LINE -> Path().apply {
                    moveTo(0f, 0f)
                    lineTo(width, 0f)
                }
                //圆角矩形
                LPConstant.DATA_TYPE_RECT -> Path().apply {
                    val rx = bean.rx.toPixel()
                    val ry = bean.ry.toPixel()
                    addRoundRect(RectF(0f, 0f, width, height), rx, ry, Path.Direction.CW)
                }
                //爱心
                LPConstant.DATA_TYPE_LOVE -> {
                    val lovePath =
                        Sharp.loadPath("M12 21.593c-5.63-5.539-11-10.297-11-14.402 0-3.791 3.068-5.191 5.281-5.191 1.312 0 4.151.501 5.719 4.457 1.59-3.968 4.464-4.447 5.726-4.447 2.54 0 5.274 1.621 5.274 5.181 0 4.069-5.136 8.625-11 14.402")
                    lovePath.scaleToSize(width, height)
                }
                //椭圆
                LPConstant.DATA_TYPE_OVAL -> Path().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        addOval(
                            0f,
                            0f,
                            width,
                            height,
                            VectorWriteHandler.DEFAULT_PATH_DIRECTION
                        )
                    } else {
                        addOval(
                            RectF(0f, 0f, width, height),
                            VectorWriteHandler.DEFAULT_PATH_DIRECTION
                        )
                    }
                }
                //星星
                LPConstant.DATA_TYPE_PENTAGRAM -> Path().apply {
                    val side = bean.side //边数
                    val depth = bean.depth //深度, 决定内圈的半径 (内圈半径 = 固定外圈半径 * (1 - [depth] / 100))
                    val R = min(width, height) / 2 //五角星外圆的半径
                    //val r = min(width, height) / 4 //五角星内圆的半径
                    val r = R * (1 - depth * 1f / 100) //星星内圆的半径

                    val originX = width / 2
                    val originY = height / 2

                    val startRadians = Math.PI / 2 //开始绘制的弧度

                    //从底部中心开始的角度
                    val step = 360f / (side * 2)
                    for (i in 0..(side * 2)) {
                        val radians = startRadians + (step * i).toRadians()
                        val nextX: Double
                        val nextY: Double
                        if (i % 2 == 0) {
                            //内圆
                            nextX = originX + r * cos(radians)
                            nextY = originY + r * sin(radians)
                        } else {
                            //外圆
                            nextX = originX + R * cos(radians)
                            nextY = originY + R * sin(radians)
                        }
                        if (i == 0) {
                            moveTo(nextX.toFloat(), nextY.toFloat())
                        } else {
                            lineTo(nextX.toFloat(), nextY.toFloat())
                        }
                    }
                    close()
                }
                //多边形
                LPConstant.DATA_TYPE_POLYGON -> Path().apply {
                    val side = max(bean.side, 3) //边数 至少需要3边
                    val originX = width / 2
                    val originY = height / 2
                    val angleSum = (side - 2) * 180 //内角和
                    val angleOne = angleSum / side //每条边的角度
                    val a = (angleOne / 2f).toRadians()  //半条边的弧度

                    /*val minR = min(originX, originY) //内圆半径
                    val maxR = minR / sin(a) //外圆半径*/

                    //val c = 2 * minR / tan(a) //每条边的边长

                    val maxR = min(originX, originY)

                    //底部左边为起点
                    val startRadians = Math.PI - a    //开始绘制的弧度

                    for (i in 0..side) {
                        val radians = startRadians + Math.PI * 2 / side * i  //弧度
                        val nextX = originX + maxR * cos(radians)
                        val nextY = originY + maxR * sin(radians)
                        if (i == 0) {
                            moveTo(nextX.toFloat(), nextY.toFloat())
                        } else {
                            lineTo(nextX.toFloat(), nextY.toFloat())
                        }
                    }
                    close()
                }
                else -> null
            }
        }
    }

    override fun createStateStack(): IStateStack = LPPathStateStack()

    override fun requestElementRenderDrawable(renderParams: RenderParams?): Drawable? {
        paint.style = elementBean.paintStyle.toPaintStyle()
        if (pathList == null) {
            parseElementBean()
        }
        return super.requestElementRenderDrawable(renderParams)
    }

    override fun updateBeanToElement(renderer: BaseRenderer) {
        super.updateBeanToElement(renderer)
        if (pathList == null) {
            parseElementBean()
        }
        updateOriginPathList(pathList)
    }

    override fun updateBeanFromElement(renderer: BaseRenderer) {
        super.updateBeanFromElement(renderer)
        elementBean.paintStyle = paint.style.toPaintStyleInt()
    }

    override fun parseElementBean() {
        val data = elementBean.data
        if (!data.isNullOrEmpty()) {
            when (elementBean.mtype) {
                LPConstant.DATA_TYPE_GCODE -> {
                    val gCodeDrawable = GCodeHelper.parseGCode(data, paint)
                    if (gCodeDrawable != null) {
                        pathList = listOf(gCodeDrawable.gCodePath)
                    }
                }
                LPConstant.DATA_TYPE_SVG -> {
                    if (data.isSvgContent()) {
                        //svg标签数据
                        val sharpDrawable = Svg.loadSvgPathDrawable(data, -1, null, paint, 0, 0)
                        if (sharpDrawable != null) {
                            //
                            pathList = sharpDrawable.pathList
                        }
                    } else {
                        //svg纯路径数据
                        val path = Sharp.loadPath(data)
                        pathList = listOf(path)
                    }
                }
            }
        } else {
            createPath(elementBean)?.let { pathList = listOf(it) }
            updateOriginPathList(pathList)
        }
    }
}