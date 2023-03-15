package com.angcyo.canvas2.laser.pecker

import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup
import com.angcyo.canvas.CanvasRenderView
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.bean.LPElementBean
import com.angcyo.canvas2.laser.pecker.util.LPConstant
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter._dslAdapter
import com.angcyo.dsladapter.drawRight
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.gcode.GCodeHelper
import com.angcyo.library.ex._color
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex.colorChannel
import com.angcyo.svg.Svg
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.createPaint
import com.angcyo.widget.recycler.DslRecyclerView
import com.pixplicity.sharp.SharpDrawable

/** 扩展方法
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/03
 */

//---

/**画板视图*/
val DslViewHolder.canvasView: CanvasRenderView?
    get() = v(R.id.canvas_view)

/**画板核心操作代理类*/
val DslViewHolder.canvasDelegate: CanvasRenderDelegate?
    get() = canvasView?.delegate

//---

/**画板的操作item容器*/
val DslViewHolder.canvasItemRv: DslRecyclerView?
    get() = v(R.id.canvas_item_view)

/**[canvasItemRv]*/
val DslViewHolder.canvasItemAdapter: DslAdapter?
    get() = canvasItemRv?._dslAdapter

//---

/**具体元素的编辑控制布局, 包含[canvasControlRv]*/
val DslViewHolder.canvasControlLayout: ViewGroup?
    get() = v(R.id.canvas_control_layout)

/**具体的属性控制item容器*/
val DslViewHolder.canvasControlRv: DslRecyclerView?
    get() = v(R.id.canvas_control_view)

/**[canvasControlRv]*/
val DslViewHolder.canvasControlAdapter: DslAdapter?
    get() = canvasControlRv?._dslAdapter

//---

fun DslAdapterItem.drawCanvasRight(
    insertRight: Int = _dimen(R.dimen.lib_line),
    offsetTop: Int = _dimen(R.dimen.lib_drawable_padding),
    offsetBottom: Int = _dimen(R.dimen.lib_drawable_padding),
    color: Int = _color(R.color.canvas_dark_gray)
) {
    drawRight(insertRight, offsetTop, offsetBottom, color)
}

//---

/**扩展*/
fun GCodeHelper.parseGCode(gCodeText: String?): GCodeDrawable? =
    parseGCode(gCodeText, createPaint(Color.BLACK))

/**扩展*/
fun parseSvg(svgText: String?): SharpDrawable? = if (svgText.isNullOrEmpty()) {
    null
} else {
    Svg.loadSvgPathDrawable(svgText, -1, null, createPaint(Color.BLACK), 0, 0)
}

/**从图片中, 获取雕刻需要用到的像素信息*/
fun Bitmap.engraveColorBytes(channelType: Int = Color.RED): ByteArray {
    return colorChannel(channelType) { color, channelValue ->
        if (color == Color.TRANSPARENT) {
            0xFF //255 白色像素, 白色在纸上不雕刻, 在金属上雕刻
        } else {
            channelValue
        }
    }
}

//---

/**[com.angcyo.canvas.data.CanvasProjectItemBean.mtype]类型转成字符串*/
fun Int?.toTypeNameString() = when (this) {
    LPConstant.DATA_TYPE_BITMAP -> "Bitmap"
    LPConstant.DATA_TYPE_TEXT -> "Text"
    LPConstant.DATA_TYPE_QRCODE -> "QRCode"
    LPConstant.DATA_TYPE_BARCODE -> "BarCode"
    LPConstant.DATA_TYPE_RECT -> "Rect"
    LPConstant.DATA_TYPE_OVAL -> "Oval"
    LPConstant.DATA_TYPE_LINE -> "Line"
    LPConstant.DATA_TYPE_PEN -> "Pen"
    LPConstant.DATA_TYPE_BRUSH -> "Brush"
    LPConstant.DATA_TYPE_SVG -> "Svg"
    LPConstant.DATA_TYPE_POLYGON -> "Polygon"
    LPConstant.DATA_TYPE_PENTAGRAM -> "Pentagram"
    LPConstant.DATA_TYPE_LOVE -> "Love"
    LPConstant.DATA_TYPE_SINGLE_WORD -> "SingleWord"
    LPConstant.DATA_TYPE_GCODE -> "GCode"
    LPConstant.DATA_TYPE_PATH -> "Path"
    LPConstant.DATA_TYPE_RAW -> "Raw"
    else -> "Unknown"
}

/**构建元素的名称*/
fun List<LPElementBean>.generateName() {
    forEach {
        it.generateName(this)
    }
}
