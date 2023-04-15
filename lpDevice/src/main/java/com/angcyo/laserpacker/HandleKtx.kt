package com.angcyo.laserpacker

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.angcyo.bitmap.handle.BitmapHandle
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.gcode.GCodeDrawable
import com.angcyo.gcode.GCodeHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.createPaint
import com.angcyo.library.ex.toBase64Data
import com.angcyo.library.ex.toDrawable
import com.angcyo.svg.Svg
import com.pixplicity.sharp.SharpDrawable

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

fun Bitmap?.toBitmapItemData(action: LPElementBean.() -> Unit = {}): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_BITMAP
    bean.imageOriginal = toBase64Data()
    bean.action()
    return bean
}

/**第二版, 直接使用图片对象*/
fun Bitmap?.toBitmapElementBeanV2(
    bmpThreshold: Int = HawkEngraveKeys.lastBWThreshold.toInt(),
    invert: Boolean = false
): LPElementBean? {
    this ?: return null
    val bean = LPElementBean()
    bean.mtype = LPDataConstant.DATA_TYPE_BITMAP
    bean._imageOriginalBitmap = this
    bean._srcBitmap =
        BitmapHandle.toBlackWhiteHandle(this, bmpThreshold, invert)
    return bean
}

/**将[Bitmap]转换成[LPElementBean]数据结构*/
fun Bitmap?.toBlackWhiteBitmapItemData(): LPElementBean? {
    val bitmap = this ?: return null
    return toBitmapItemData {
        imageFilter = LPDataConstant.DATA_MODE_BLACK_WHITE //默认黑白处理
        blackThreshold = HawkEngraveKeys.lastBWThreshold
        src = bitmap.toBlackWhiteBitmap(HawkEngraveKeys.lastBWThreshold.toInt())
    }
}