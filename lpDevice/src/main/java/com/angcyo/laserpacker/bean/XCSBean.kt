package com.angcyo.laserpacker.bean

import android.graphics.Paint
import android.graphics.PointF
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.ex.uuid

/**
 * XTool数据结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/21
 */
data class XCSBean(
    /**所有数据在此, 所有图层*/
    val canvas: List<XCSCanvasBean>? = null
)

data class XCSCanvasBean(
    /**图层内的所有元素*/
    val displays: List<XCSDisplayBean>? = null,
)

data class XCSDisplayBean(
    /**数据类型
     * TEXT
     * PATH
     * BITMAP
     * PEN
     * CIRCLE
     * RECT
     * LINE
     * */
    val type: String? = null,

    /**TEXT: 文本内容*/
    val text: String? = null,

    /**PATH: svg path数据*/
    val dPath: String? = null,

    /**BITMAP: base64协议头的数据*/
    val base64: String? = null,

    /**PEN: 每个点的坐标*/
    val points: List<PointF>? = null,

    /**RECT: 圆角*/
    val radius: Float? = null,

    /**LINE: 结束点*/
    val endPoint: PointF? = null,

    val x: Float? = null,
    val y: Float? = null,
    var width: Float? = null,
    var height: Float? = null,
    val angle: Float? = null,

    var scale: PointF? = null,
    val skew: PointF? = null,
    val pivot: PointF? = null,

    val offsetX: Float? = null,
    val offsetY: Float? = null,

    val isFill: Boolean? = null,
)

/**类型转换*/
fun XCSBean.toElementBeanList(): List<LPElementBean> {
    val result = mutableListOf<LPElementBean>()
    for (can in canvas ?: emptyList()) {
        val groupId = uuid()
        for (dis in can.displays ?: emptyList()) {
            var bean: LPElementBean? = null
            val scale = dis.scale
            if (dis.type == "TEXT") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_TEXT
                bean.text = dis.text
            } else if (dis.type == "PATH") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_SVG
                bean.path = dis.dPath
            } else if (dis.type == "BITMAP") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_BITMAP
                bean.imageOriginal = dis.base64
            } else if (dis.type == "PEN") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_SVG
                bean.path = buildString {
                    var isFirst = true
                    for (point in dis.points ?: emptyList()) {
                        if (isFirst) {
                            append("M${point.x},${point.y}")
                        } else {
                            append("L${point.x},${point.y}")
                        }
                        isFirst = false
                    }
                }
            } else if (dis.type == "CIRCLE") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_OVAL
            } else if (dis.type == "RECT") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_RECT
                bean.rx = dis.radius ?: 0f
                bean.ry = bean.rx
            } else if (dis.type == "LINE") {
                bean = LPElementBean()
                bean.mtype = LPDataConstant.DATA_TYPE_LINE
            }

            bean?.apply {
                this.groupId = groupId
                left = dis.x ?: 0f
                top = dis.y ?: 0f
                width = dis.width ?: 0f
                height = dis.height ?: 0f
                angle = dis.angle ?: 0f
                scaleX = scale?.x ?: 1f
                scaleY = scale?.y ?: 1f
                skewX = dis.skew?.x ?: 0f
                skewY = dis.skew?.y ?: 0f

                paintStyle = if (dis.isFill == true) {
                    Paint.Style.FILL.toPaintStyleInt()
                } else {
                    Paint.Style.STROKE.toPaintStyleInt()
                }

                result.add(this)
            }
        }
    }
    return result
}