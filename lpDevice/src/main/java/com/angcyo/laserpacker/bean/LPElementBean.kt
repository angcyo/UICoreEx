package com.angcyo.laserpacker.bean

import android.graphics.Paint
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.laserpacker.toTypeNameString
import com.angcyo.library.annotation.Implementation
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.unit.toPixel

/**
 * 数据元素存储的结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
data class LPElementBean(

    //region ---bounds---

    /**数据所在位置*/
    @MM
    var left: Float = 0f,

    @MM
    var top: Float = 0f,

    /**数据原始的宽高, 线条的长度*/
    @MM
    var width: Float? = null,

    @MM
    var height: Float? = null,

    /**自动雕刻下的重力属性, 相对于设备最佳范围bounds, 如果设置了此属性
     * [left] [top] 就是变成offset偏移量
     * [com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo.bounds]
     * [android.view.Gravity.LEFT] 3
     * [android.view.Gravity.RIGHT] 5
     * [android.view.Gravity.TOP] 48
     * [android.view.Gravity.BOTTOM] 80
     * [android.view.Gravity.CENTER_HORIZONTAL] 1
     * [android.view.Gravity.CENTER_VERTICAL] 16
     * [android.view.Gravity.CENTER] 17
     * 左上: 51
     * 左下: 83
     * 右上: 53
     * 右下: 85
     * 居中: 17
     * */
    var gravity: Int? = null,

    /**旋转的度数, 角度单位*/
    var angle: Float = 0f,

    /**数据绘制时的缩放比例*/
    var scaleX: Float? = null,

    var scaleY: Float? = null,

    /**是否水平翻转*/
    var flipX: Boolean? = null,

    /**是否垂直翻转*/
    var flipY: Boolean? = null,

    /**数据绘制时的倾斜度数, 角度单位.
     * 先缩放, 再倾斜. 然后旋转绘制
     *
     * ```
     * postSkew(tan(skewX.toRadians()).toFloat(), tan(skewY.toRadians()).toFloat(), rect.left, rect.top)
     * ```
     * */
    var skewX: Float? = null,

    /**这是值始终为0*/
    var skewY: Float? = null,

    //endregion ---bounds---

    //region ---公共属性---

    /**数据id*/
    var id: Long = -1,

    /**数据唯一标识符*/
    var uuid: String? = null,

    /**图层代表图标, 如果有base64图片*/
    var icon: String? = null,

    /**数据类型, 线条类型的长度放在[width]属性中
     * [com.angcyo.canvas2.laser.pecker.util.LPConstant.DATA_TYPE_TEXT]
     * [com.angcyo.canvas2.laser.pecker.util.LPConstant.DATA_TYPE_RECT]
     * [com.angcyo.canvas2.laser.pecker.util.LPConstant.DATA_TYPE_OVAL]
     * [com.angcyo.canvas2.laser.pecker.util.LPConstant.DATA_TYPE_LINE]
     * */
    var mtype: Int = -1,

    /**图层名称, 如果不指定, 则通过[mtype]类型获取*/
    var name: String? = null,

    /**相同id的视为在同一组 string `2023-1-6`*/
    var groupId: String? = null,

    /**填充颜色, 形状的颜色*/
    var fill: String? = null,

    /**描边颜色*/
    var stroke: String? = null,

    /**
     * 0 [Paint.Style.FILL]
     * 1 [Paint.Style.STROKE]
     * 2 [Paint.Style.FILL_AND_STROKE]
     * */
    var paintStyle: Int = Paint.Style.FILL.toPaintStyleInt(),

    /**原始的数据, 如svg文件内容, gcode文件内容
     * [LPDataConstant.DATA_TYPE_RAW] 真实数据的类型
     * [Charsets.ISO_8859_1]
     * */
    var data: String? = null,

    /**是否可见*/
    var isVisible: Boolean = true,

    /**是否锁定了图层*/
    var isLock: Boolean = false,

    //endregion ---公共属性---

    //region ---文本类型---

    /**文本的内容
     * [LPDataConstant.DATA_TYPE_QRCODE]
     * [LPDataConstant.DATA_TYPE_BARCODE]
     * */
    var text: String? = null,

    /**二维码编码格式, 编码格式（qrcode）, 编码格式（code128）*/
    @Implementation
    var coding: String? = null,

    /**纠错级别*/
    @Implementation
    var eclevel: String? = null,

    /**文本的对齐方式
     * [Paint.Align.toAlignString]
     * */
    var textAlign: String? = null,

    /**字体大小*/
    @MM
    var fontSize: Float = 10f,

    /**字间距*/
    @MM
    var charSpacing: Float = LPDataConstant.DEFAULT_CHAR_SPACING,

    /**行间距*/
    @MM
    var lineSpacing: Float = LPDataConstant.DEFAULT_CHAR_SPACING,

    /**字体名称*/
    var fontFamily: String? = null,

    /**是否加粗*/
    var fontWeight: String? = null,

    /**是否斜体*/
    var fontStyle: String? = null,

    /**下划线*/
    var underline: Boolean = false,

    /**删除线*/
    var linethrough: Boolean = false,

    /**文本排列方向*/
    var orientation: Int = LinearLayout.HORIZONTAL,

    /**绘制紧凑文本, 这种绘制方式的文本边框留白少*/
    var isCompactText: Boolean = true,

    /**文本颜色*/
    var textColor: String? = null,

    //endregion ---文本类型---

    //region ---形状---

    /**水平圆角半径, 矩形/椭圆
     * 椭圆矩形的宽度 = [rx] * 2
     * */
    @MM
    var rx: Float = 0f,

    /**垂直圆角半径, 矩形/椭圆
     * 椭圆矩形的高度 = [ry] * 2
     * */
    @MM
    var ry: Float = 0f,

    /**星星/多边形的边数 5 [3-50]
     * [com.angcyo.canvas.graphics.PolygonGraphicsParser]
     * [com.angcyo.canvas.graphics.PentagramGraphicsParser]
     * */
    var side: Int = 3,

    /**星星的深度 40 [1-100], 深度越大内圈半径越小
     * 固定外圈半径, 那么 内圈半径 = 固定外圈半径 * (1-[depth] / 100)
     *
     * [com.angcyo.canvas.graphics.PentagramGraphicsParser]
     * */
    var depth: Int = 40,

    //endregion ---形状---

    //region ---SVG path数据---

    /**SVG数据
     * "[['M',0,0],['L',11,11]]"
     * */
    var path: String? = null,

    /**虚线线宽*/
    @MM
    var dashWidth: Float = 1f,

    /**虚线线距*/
    @MM
    var dashGap: Float = 1f,

    //endregion ---SVG path数据---

    //region ---图片数据---

    /** 原图数据 (data:image/xxx;base64,xxx) */
    var imageOriginal: String? = null,

    /**滤镜后显示图 string, 带协议头
     * [data] gcode数据*/
    var src: String? = null,

    /**图片滤镜
     * 图片滤镜 'black'(黑白) | 'seal'(印章) | 'gray'(灰度) | 'prints'(版画) | 'Jitter(抖动)' | 'gcode'
     * imageFilter 图片滤镜 1:黑白 | 2:印章 | 3:灰度 | 4:版画 | 5:抖动 | 6:gcode `2022-9-21`
     *
     * [LPDataConstant.DATA_MODE_BLACK_WHITE]
     * [LPDataConstant.DATA_MODE_SEAL]
     * [LPDataConstant.DATA_MODE_GREY]
     * [LPDataConstant.DATA_MODE_PRINT]
     * [LPDataConstant.DATA_MODE_DITHERING]
     * [LPDataConstant.DATA_MODE_GCODE]
     *
     * */
    var imageFilter: Int = LPDataConstant.DATA_MODE_GREY,

    /** 对比度*/
    var contrast: Float = 0f,

    /**亮度*/
    var brightness: Float = 0f,

    /**黑白阈值*/
    var blackThreshold: Float = LPDataConstant.DEFAULT_THRESHOLD,

    /**印章阈值*/
    var sealThreshold: Float = LPDataConstant.DEFAULT_THRESHOLD,

    /**版画阈值*/
    var printsThreshold: Float = LPDataConstant.DEFAULT_THRESHOLD,

    /**是否反色*/
    var inverse: Boolean = false,

    /**gcode线距*/
    var gcodeLineSpace: Float = 5f,

    /**gcode填充线的角度[0-90]*/
    var gcodeAngle: Float = 0f,

    /**gcode方向 0:0 1:90 2:180 3:270*/
    @Deprecated("使用angle属性代替")
    var gcodeDirection: Int = 0,

    /**gcode是否需要轮廓*/
    var gcodeOutline: Boolean = true,

    /**2022-12-30 矢量使用GCode线段填充线距(毫米), 大于0时生效
     * [paintStyle] == 1 笔的样式必须是描边时, 才有效
     * */
    @MM
    var gcodeFillStep: Float = 0f,

    /**路径填充的角度
     * [gcodeFillStep]*/
    var gcodeFillAngle: Float = 0f,

    /**是否扭曲*/
    var isMesh: Boolean = false,

    /**最小直径, 最大直径*/
    @MM
    var minDiameter: Float? = null,

    @MM
    var maxDiameter: Float? = null,

    /**扭曲类型,
     * "CONE" 圆锥
     * "BALL" 球体
     * */
    var meshShape: String? = null,

    //endregion ---图片数据---

    //region ---雕刻参数---

    /**发给机器的数据索引
     * 当数据没有改变时, 相同的索引不必重复发送数据给机器
     * [com.angcyo.engrave.transition.IEngraveTransition.initTransferDataIndex]
     * */
    var index: Int? = null,

    /**数据对应的dpi*/
    var dpi: Float? = null,

    /**
     * [LPDataConstant.DATA_TYPE_RAW] 真实数据的雕刻类型, 发给机器的数据类型
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
     * */
    var engraveType: Int = 0x10,

    /**GCode/黑白数据的行数*/
    var lines: Int = -1,

    /**[com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity]*/

    /**[com.angcyo.engrave.EngraveFlowDataHelper.generateEngraveConfig]*/
    //材质key
    var materialKey: String? = null,
    //激光类型
    var printType: Int? = null,
    //加速级别
    var printPrecision: Int? = null,
    var printPower: Int? = null,
    var printDepth: Int? = null,
    var printCount: Int? = null,

    //endregion ---雕刻参数---

    //region ---私有属性---

    /**强行指定要雕刻的数据模式
     * [_layerMode]
     *
     * [LPDataConstant.DATA_MODE_GREY]
     * [LPDataConstant.DATA_MODE_BLACK_WHITE]
     * [LPDataConstant.DATA_MODE_DITHERING]
     * [LPDataConstant.DATA_MODE_GCODE]
     * */
    var dataMode: Int? = null,

    /**旧的兼容数据
     * 数据处理的模式, 处理成机器需要的数据. 通常情况下和雕刻图层一致
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING]
     *
     * [com.angcyo.canvas.graphics.IGraphicsParser.initDataModeWithPaintStyle]
     *
     * [com.angcyo.engrave.data.EngraveLayerInfo]
     * */
    @Transient
    var _dataMode: Int? = null,

    /**是否处于调试模式下*/
    var _debug: Boolean? = null

    //endregion ---私有属性---
) {

    /**数据处理的模式, 处理成机器需要的数据. 通常情况下和雕刻图层一致
     * [LPDataConstant.DATA_MODE_BLACK_WHITE]
     * [LPDataConstant.DATA_MODE_GCODE]
     * [LPDataConstant.DATA_MODE_DITHERING]
     *
     * [com.angcyo.laserpacker.device.data.EngraveLayerInfo]
     * */
    val _layerMode: Int?
        get() = if (dataMode == LPDataConstant.DATA_MODE_BLACK_WHITE ||
            dataMode == LPDataConstant.DATA_MODE_GCODE ||
            dataMode == LPDataConstant.DATA_MODE_GREY ||
            dataMode == LPDataConstant.DATA_MODE_DITHERING
        ) {
            dataMode
        } else {
            when (mtype) {
                LPDataConstant.DATA_TYPE_BITMAP -> when (imageFilter) {
                    LPDataConstant.DATA_MODE_PRINT,
                    LPDataConstant.DATA_MODE_SEAL,
                    LPDataConstant.DATA_MODE_BLACK_WHITE -> LPDataConstant.DATA_MODE_BLACK_WHITE
                    LPDataConstant.DATA_MODE_DITHERING -> if (vmApp<LaserPeckerModel>().isSupportDithering() && !HawkEngraveKeys.forceGrey) {
                        //支持抖动
                        LPDataConstant.DATA_MODE_DITHERING
                    } else {
                        //不支持抖动, 则发送灰度图片
                        LPDataConstant.DATA_MODE_GREY
                    }
                    else -> imageFilter
                }
                LPDataConstant.DATA_TYPE_TEXT -> if (paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                    //描边文本, 走GCode
                    LPDataConstant.DATA_MODE_GCODE
                } else {
                    //否则就是黑白画
                    LPDataConstant.DATA_MODE_BLACK_WHITE
                }
                LPDataConstant.DATA_TYPE_QRCODE,
                LPDataConstant.DATA_TYPE_BARCODE -> LPDataConstant.DATA_MODE_BLACK_WHITE
                //填充线/描边线, 都是GCode
                LPDataConstant.DATA_TYPE_LINE -> LPDataConstant.DATA_MODE_GCODE
                LPDataConstant.DATA_TYPE_RECT,
                LPDataConstant.DATA_TYPE_OVAL,
                LPDataConstant.DATA_TYPE_LOVE,
                LPDataConstant.DATA_TYPE_POLYGON,
                LPDataConstant.DATA_TYPE_PENTAGRAM,
                LPDataConstant.DATA_TYPE_PEN,
                LPDataConstant.DATA_TYPE_PATH,
                LPDataConstant.DATA_TYPE_SVG,
                LPDataConstant.DATA_TYPE_GCODE -> if (paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                    //描边SVG, 走GCode
                    LPDataConstant.DATA_MODE_GCODE
                } else {
                    //否则就是黑白画
                    LPDataConstant.DATA_MODE_BLACK_WHITE
                }
                else -> null
            }
        }

    /**原始的宽高, 毫米*/
    @MM
    val _width: Float
        get() = width ?: 0f

    @MM
    val _height: Float
        get() = height ?: 0f

    /**缩放后的宽高, 像素*/
    @Pixel
    val _widthScalePixel: Float
        get() = _width.toPixel() * _scaleX

    @Pixel
    val _heightScalePixel: Float
        get() = _height.toPixel() * _scaleY

    //---

    val _scaleX: Float
        get() = scaleX ?: 1f

    val _scaleY: Float
        get() = scaleY ?: 1f

    //---

    val _flipX: Boolean
        get() = flipX ?: false

    val _flipY: Boolean
        get() = flipY ?: false

    //翻转其实就是反向缩放
    val _flipScaleX: Float
        get() = if (_flipX) -1f else 1f

    val _flipScaleY: Float
        get() = if (_flipY) -1f else 1f

    //---

    /**倾斜, 角度*/
    val _skewX: Float
        get() = skewX ?: 0f

    val _skewY: Float
        get() = skewY ?: 0f

    //---

    val isLineShape: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_LINE

    /**构建一个图层名, 当前的元素, 在[list]中的不重名名称*/
    fun generateName(list: List<LPElementBean>) {
        if (name == null) {
            if (mtype >= 0) {
                val typeName = mtype.toTypeNameString()
                generateName(list, typeName)
            }
        }
    }

    /**分配一个新的名字
     * [baseName] 基础名字*/
    private fun generateName(list: List<LPElementBean>, baseName: String, index: Int? = null) {
        val newName = if (index == null) baseName else "$baseName $index" //需要检测的新名字
        val find = list.find { it != this && it.name == newName }
        if (find == null) {
            //未重名
            name = newName
        } else {
            //重名了
            generateName(list, baseName, if (index == null) 2 else index + 1)
        }
    }
}
