package com.angcyo.laserpacker.bean

import android.graphics.Bitmap
import android.graphics.Paint
import android.widget.LinearLayout
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._productName
import com.angcyo.core.vmApp
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.ensurePrintPrecision
import com.angcyo.laserpacker.device.toLayerId
import com.angcyo.laserpacker.toAlignString
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.laserpacker.toTypeNameString
import com.angcyo.library.L
import com.angcyo.library.annotation.Implementation
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex.connect
import com.angcyo.library.ex.toDC
import com.angcyo.library.unit.toPixel
import com.angcyo.library.utils.uuid
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfig
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
import kotlin.math.max

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

    /**数据原始的宽高, 线条的长度
     * 如果是图片元素, 则存储的就是图片宽高
     * */
    @MM
    var width: Float? = null,

    /**[width]*/
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
    var uuid: String? = uuid(),

    /**图层代表图标, 如果有base64图片*/
    var icon: String? = null,

    /**数据类型, 线条类型的长度放在[width]属性中
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_TEXT]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_RECT]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_OVAL]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_LINE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_TYPE_SVG]
     * */
    var mtype: Int = -1,

    /**图层名称, 如果不指定, 则通过[mtype]类型获取*/
    var name: String? = null,

    /**相同id的视为在同一组 string `2023-1-6`*/
    var groupId: String? = null,

    /**[groupId] 需要显示的名称*/
    var groupName: String? = null,

    /**填充颜色, 形状的颜色
     * ```
     * #ff00ff
     * ```
     * */
    var fill: String? = null,

    /**描边颜色, 如果有
     * ```
     * #ff00ff
     * ```
     * */
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
     * [path]
     * */
    var data: String? = null,

    /**[LPDataConstant.PROJECT_V2_BASE_URI]*/
    var dataUri: String? = null,

    /**是否可见*/
    var isVisible: Boolean = true,

    /**是否锁定了图层*/
    var isLock: Boolean = false,

    //endregion ---公共属性---

    //region ---文本类型---

    /**文本的内容, 变量文本的内容
     * [LPDataConstant.DATA_TYPE_QRCODE]
     * [LPDataConstant.DATA_TYPE_BARCODE]
     * */
    var text: String? = null,

    /**二维码编码格式, 编码格式（qrcode）, 编码格式（code128）
     * [com.google.zxing.BarcodeFormat.QR_CODE]
     * [com.google.zxing.BarcodeFormat.CODE_128]
     * */
    var coding: String? = null,

    /**QrCode纠错级别
     * L M Q H*/
    var eclevel: String? = "H",

    /**QR 码掩码图案 [0~8)
     * -1:自动
     * */
    var qrMaskPattern: Int? = -1,

    /**
     * PDF_417纠错程度:[0~8]
     * Aztec纠错:[0~100]
     * */
    var errorLevel: Int? = null,

    /**
     * 字符显示样式: top:显示在条码上 bottom:显示在条码上 none:不显示 `2023-9-9`
     * */
    var textShowStyle: String? = null,

    /**文本的对齐方式
     * [Paint.Align.toAlignString]
     * */
    var textAlign: String? = Paint.Align.LEFT.toAlignString(),

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

    /**2023-6-2
     * 曲线文本曲率[-360~360], 0表示正常文本*/
    var curvature: Float = 0f,

    /**变量模板类型*/
    var variables: List<LPVariableBean>? = null,

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

    /**SVG路径数据
     * "M0,0L100,100Z"
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

    /** 原图数据 (data:image/xxx;base64,xxx) 需要协议头*/
    var imageOriginal: String? = null,

    /**[LPDataConstant.PROJECT_V2_BASE_URI]*/
    var imageOriginalUri: String? = null,

    /**滤镜后显示图 string, 带协议头
     * [data] gcode数据*/
    var src: String? = null,

    /**[LPDataConstant.PROJECT_V2_BASE_URI]*/
    var srcUri: String? = null,

    /**图片滤镜
     * 图片滤镜 'black'(黑白) | 'seal'(印章) | 'gray'(灰度) | 'prints'(版画) | 'Jitter(抖动)' | 'gcode'
     * imageFilter 图片滤镜 1:黑白 | 2:印章 | 3:灰度 | 4:版画 | 5:抖动 | 6:gcode | 7:2D浮雕 `2023-10-7`
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
    var blackThreshold: Float = HawkEngraveKeys.lastBWThreshold,

    /**印章阈值*/
    var sealThreshold: Float = HawkEngraveKeys.lastSealThreshold,

    /**版画阈值*/
    var printsThreshold: Float = HawkEngraveKeys.lastPrintThreshold,

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

    /**切片的数量*/
    var sliceCount: Int = 0,

    /**每一层下降的高度 mm单位*/
    @MM
    var sliceHeight: Float = HawkEngraveKeys.minSliceHeight,

    /**切片的粒度,
     * 1:每1个色阶1层,
     * 10:每10个色阶1层,
     * */
    @Implementation
    var sliceGranularity: Int = 0,

    /**2d浮雕强度[1~20]*/
    var reliefStrength: Int = 1,

    //endregion ---图片数据---

    //region ---雕刻参数---

    /**
     * [LPDataConstant.DATA_TYPE_RAW] 真实数据的雕刻类型, 发给机器的数据类型
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
     * */
    @Implementation
    var engraveType: Int = 0x10,

    /**GCode/黑白数据的行数*/
    @Implementation
    var lines: Int = -1,

    /**发给机器的数据索引
     * 当数据没有改变时, 相同的索引不必重复发送数据给机器
     * [com.angcyo.engrave2.transition.EngraveTransitionHelper.initTransferDataIndex]
     * */
    var index: Int? = null,

    /**数据对应的dpi*/
    var dpi: Float? = null,

    /**[com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity]*/

    /**[com.angcyo.laserpacker.LPTransferData.generateEngraveConfig]
     * [com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper.generateEngraveConfig]
     * */
    //材质唯一码
    var materialCode: String? = null,
    //材质key
    var materialKey: String? = null,
    //材质名
    var materialName: String? = null,
    //激光类型
    var printType: Int? = null,
    //加速级别
    var printPrecision: Int? = null,
    var printPower: Int? = null,
    var printDepth: Int? = null,
    var printCount: Int? = null,
    //风速级别
    var pump: Int? = null,

    /**2023-11-4
     * 是否要使用激光器出光频率
     * */
    var useLaserFrequency: Boolean = false,

    /**2023-10-30
     * 激光器出光频率，取值范围为26-60，单位kHz
     * 默认60kHz
     * */
    var laserFrequency: Int? = null,

    /**是否是切割*/
    var isCut: Boolean = false,

    /**图层id, 只在切割图层时需要赋值, 其他图层会通过[_layerMode]获取*/
    var layerId: String? = null,

    //endregion ---雕刻参数---

    //region ---私有属性---

    /**V2中直接使用图片对象, 而不是转换成base64损耗性能*/
    @Transient var _imageOriginalBitmap: Bitmap? = null, //原图
    @Transient var _srcBitmap: Bitmap? = null, //滤镜后的图

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
     * [com.angcyo.laserpacker.LPDataConstant.DATA_MODE_BLACK_WHITE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_MODE_GCODE]
     * [com.angcyo.laserpacker.LPDataConstant.DATA_MODE_DITHERING]
     *
     * [com.angcyo.laserpacker.device.data.EngraveLayerInfo]
     * */
    @Transient
    var _dataMode: Int? = null,

    /**是否处于调试模式下*/
    var _debug: Boolean? = null

    //endregion ---私有属性---
) {

    /**数据处理的模式, 处理成机器需要的数据. 通常情况下和雕刻图层一致
     *
     * [LPDataConstant.DATA_MODE_BLACK_WHITE]
     *
     * [LPDataConstant.DATA_MODE_GCODE]
     *
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
            when {
                mtype == LPDataConstant.DATA_TYPE_BITMAP -> if (isNeedSlice) LPDataConstant.DATA_MODE_GCODE /*切片走gcode*/ else
                    when (imageFilter) {
                        LPDataConstant.DATA_MODE_PRINT,
                        LPDataConstant.DATA_MODE_SEAL,
                        LPDataConstant.DATA_MODE_BLACK_WHITE -> LPDataConstant.DATA_MODE_BLACK_WHITE

                        //2D浮雕/灰度图片
                        LPDataConstant.DATA_MODE_GREY, LPDataConstant.DATA_MODE_RELIEF -> LPDataConstant.DATA_MODE_GREY

                        LPDataConstant.DATA_MODE_DITHERING -> if (vmApp<LaserPeckerModel>().isSupportDithering() && !HawkEngraveKeys.forceGrey) {
                            //支持抖动
                            LPDataConstant.DATA_MODE_DITHERING
                        } else {
                            //不支持抖动, 则发送灰度图片
                            LPDataConstant.DATA_MODE_GREY
                        }

                        else -> imageFilter
                    }

                isRenderTextElement -> if (paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                    //描边文本, 走GCode
                    LPDataConstant.DATA_MODE_GCODE
                } else {
                    //否则就是黑白画
                    LPDataConstant.DATA_MODE_BLACK_WHITE
                }

                is1DCodeElement || is2DCodeElement -> LPDataConstant.DATA_MODE_BLACK_WHITE
                //填充线/描边线, 都是GCode
                isLineShape -> LPDataConstant.DATA_MODE_GCODE
                isPathElement -> if (paintStyle == Paint.Style.STROKE.toPaintStyleInt()) {
                    //描边SVG, 走GCode
                    LPDataConstant.DATA_MODE_GCODE
                } else {
                    //否则就是黑白画
                    LPDataConstant.DATA_MODE_BLACK_WHITE
                }

                else -> null
            }
        }

    /**图层id
     * [_layerMode] 图层模式对应的图层id
     * */
    val _layerId: String?
        get() {
            val result = layerId ?: _layerMode?.toLayerId()
            return if (result == LaserPeckerHelper.LAYER_LINE && isCut) {
                LaserPeckerHelper.LAYER_CUT
            } else {
                isCut = false
                result
            }
        }

    /**是否配置了雕刻参数*/
    val _isConfigEngraveParams: Boolean
        get() = printType != null &&
                printPower != null &&
                printDepth != null &&
                printCount != null &&
                dpi != null /*&&
                printPrecision != null*/

    /**[com.angcyo.canvas2.laser.pecker.element.LPPathElement]*/
    val isPathElement: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_PATH ||
                mtype == LPDataConstant.DATA_TYPE_SVG ||
                mtype == LPDataConstant.DATA_TYPE_GCODE ||
                mtype == LPDataConstant.DATA_TYPE_LINE ||
                mtype == LPDataConstant.DATA_TYPE_RECT ||
                mtype == LPDataConstant.DATA_TYPE_OVAL ||
                mtype == LPDataConstant.DATA_TYPE_LOVE ||
                mtype == LPDataConstant.DATA_TYPE_POLYGON ||
                mtype == LPDataConstant.DATA_TYPE_PENTAGRAM ||
                mtype == LPDataConstant.DATA_TYPE_PEN

    /**是否是渲染文本的元素*/
    val isRenderTextElement: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_TEXT ||
                mtype == LPDataConstant.DATA_TYPE_VARIABLE_TEXT

    /**二维码类型元素*/
    val is2DCodeElement: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_QRCODE ||
                mtype == LPDataConstant.DATA_TYPE_VARIABLE_QRCODE

    /**一维码类型元素*/
    val is1DCodeElement: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_BARCODE ||
                mtype == LPDataConstant.DATA_TYPE_VARIABLE_BARCODE

    /**是否是变量元素*/
    val isVariableElement: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_VARIABLE_TEXT ||
                mtype == LPDataConstant.DATA_TYPE_VARIABLE_QRCODE ||
                mtype == LPDataConstant.DATA_TYPE_VARIABLE_BARCODE

    /**当前元素结构是否支持切片*/
    val isSupportSliceElement: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_BITMAP &&
                (imageFilter == LPDataConstant.DATA_MODE_DITHERING ||
                        imageFilter == LPDataConstant.DATA_MODE_GREY)

    /**当前数据是否需要切片*/
    val isNeedSlice: Boolean
        get() = isSupportSliceElement && sliceCount > 0

    /**是否要显示条码字符*/
    val isShowBarcodeText: Boolean
        get() = textShowStyle == LPDataConstant.TEXT_SHOW_STYLE_TOP || textShowStyle == LPDataConstant.TEXT_SHOW_STYLE_BOTTOM

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

    /**2023-10-19
     * 高度为0的svg也视为线条
     * */
    val isLineShape: Boolean
        get() = mtype == LPDataConstant.DATA_TYPE_LINE /*||
                (mtype == LPDataConstant.DATA_TYPE_SVG && _height == 0f)*/

    /**是否是时间变量文本*/
    val isDateTimeVariable: Boolean
        get() = isVariableElement && variables?.find { it.isDateTimeVariable } != null

    /**是否是自动时间变量文本*/
    val isAutoDateTimeVariable: Boolean
        get() = isVariableElement && variables?.find { it.isDateTimeVariable && it.auto } != null

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

    /**清空数据索引, 并且清除雕刻参数*/
    fun clearIndex(reason: String, clearEngraveParams: Boolean) {
        L.i("清空索引[$reason]:${index} 清除参数[${clearEngraveParams.toDC()}]")
        index = null
        if (clearEngraveParams) {
            dpi = null
            materialCode = null
            materialKey = null
            materialName = null
            printType = null
            printPrecision = null
            printPower = null
            printDepth = null
            printCount = null
        }
    }

    fun initEngraveParamsIfNeed() {
        if (!_isConfigEngraveParams) {
            initEngraveParams()
        }
    }

    /**初始化雕刻参数*/
    fun initEngraveParams() {
        val layerId = _layerId
        val layerConfig = LayerHelper.getProductLayerSupportPxJson().getLayerConfig(layerId)
        dpi = layerConfig?.dpi ?: dpi ?: LaserPeckerHelper.DPI_254

        //材质
        val materialList =
            MaterialHelper.getLayerMaterialList(layerId ?: LaserPeckerHelper.LAYER_LINE, dpi!!)
        val material = materialList.firstOrNull()
        val customMaterial =
            MaterialHelper.createCustomLayerMaterialList().find { it.layerId == layerId }
        materialCode = material?.code ?: customMaterial?.code
        materialKey = material?.key ?: customMaterial?.key
        materialName = material?.name ?: customMaterial?.name

        //获取最后一次相同图层的雕刻参数
        val productName = _productName
        val last = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                EngraveConfigEntity_.productName.equal("$productName")
                    .and(EngraveConfigEntity_.layerId.equal(layerId ?: ""))
            )
        }

        //功率
        printPower =
            material?.power ?: last?.power ?: customMaterial?.power ?: HawkEngraveKeys.lastPower
        printDepth =
            material?.depth ?: last?.depth ?: customMaterial?.depth ?: HawkEngraveKeys.lastDepth
        printCount = max(1, material?.count ?: customMaterial?.count ?: 1)

        //光源
        printType = (material?.type ?: last?.type ?: customMaterial?.type
        ?: DeviceHelper.getProductLaserType()).toInt()
        printPrecision = (material?.precision ?: last?.precision ?: customMaterial?.precision
        ?: HawkEngraveKeys.lastPrecision).ensurePrintPrecision()
    }

    //region ---variable----

    /**获取变量所对应的文本*/
    fun getVariableText(): String? {
        return variables?.getVariableText()
    }

    /**更新变量所对应的文本*/
    fun updateVariableText(): String? {
        text = getVariableText()
        return text
    }

    /**雕刻完成之后, 更新变量文本*/
    fun updateVariableTextAfterEngrave(onlyDateTime: Boolean) {
        for (bean in variables ?: emptyList()) {
            bean.updateAfterEngrave(onlyDateTime)
        }
        text = getVariableText()
    }

    /**复制当前bean的文本属性到另一个bean*/
    fun copyTextProperty(to: LPElementBean = LPElementBean(mtype = LPDataConstant.DATA_TYPE_TEXT)): LPElementBean {
        to.text = text
        to.fontFamily = fontFamily
        to.orientation = orientation
        to.charSpacing = charSpacing
        to.lineSpacing = lineSpacing
        to.fontSize = fontSize
        to.isCompactText = isCompactText
        to.textAlign = textAlign
        to.textColor = textColor
        to.curvature = curvature

        to.underline = underline
        to.linethrough = linethrough
        to.fontWeight = fontWeight
        to.fontStyle = fontStyle
        to.paintStyle = paintStyle
        return to
    }

    /**复制当前bean的变量属性到另一个bean*/
    fun copyVariableProperty(to: LPElementBean = LPElementBean(mtype = LPDataConstant.DATA_TYPE_VARIABLE_TEXT)): LPElementBean {
        to.mtype = mtype

        to.variables = variables
        to.coding = coding
        to.eclevel = eclevel
        to.qrMaskPattern = qrMaskPattern
        to.errorLevel = errorLevel

        to.textShowStyle = textShowStyle
        return to
    }

    //endregion ---variable----
}

/**获取变量所对应的文本*/
fun List<LPVariableBean>.getVariableText(): String {
    return connect("")
}
