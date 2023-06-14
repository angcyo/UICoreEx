package com.angcyo.laserpacker

import android.graphics.Paint
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.library.annotation.MM

/**
 * 数据相关的常量
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/04/02
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
object LPDataConstant {

    /**默认的GCode线距*/
    const val DEFAULT_LINE_SPACE = 5.0f

    /**默认的字间距*/
    @MM
    const val DEFAULT_CHAR_SPACING = 0.5f

    //region ---支持的文件类型---

    /**创作支持的数据类型
     * [com.angcyo.gcode.GCodeHelper.parseGCode]
     * */
    const val GCODE_EXT = ".gcode"

    /**SVG文件
     * [com.angcyo.svg.Svg]
     * */
    const val SVG_EXT = ".svg"

    /**文本后缀, 检查文本内容
     * [String.isGCodeContent]
     * [String.isSvgContent]
     * */
    const val TXT_EXT = ".txt"

    /**工程文件后缀
     * [String.toProjectBean]
     * [com.angcyo.laserpacker.bean.LPProjectBean]
     * */
    const val PROJECT_EXT = ".lp"

    /**2023-04-12 第二版工程结构文件后缀, 使用zip结构存储¬*/
    const val PROJECT_EXT2 = ".lp2"

    /**默认的结构工程文件存储名*/
    const val PROJECT_V1_TEMP_NAME = ".lptemp"
    const val PROJECT_V2_TEMP_NAME = ".lp2temp"

    /**临时存储文件夹*/
    const val PROJECT_V2_TEMP_FOLDER = "lp2temp"

    /**第二版结构中, 工程默认的结构数据放在zip包里面此流中*/
    const val PROJECT_V2_DEFAULT_NAME = ".lpproject"

    /**第二版结构中, 预览图放在zip包里面此流中*/
    const val PROJECT_V2_PREVIEW_NAME = "preview.png"

    /**V2: 所有资源存放的基础路径*/
    const val PROJECT_V2_BASE_URI = "res/"

    /**
     * dxf文件后缀
     * [com.angcyo.kabeja.library.Dxf]*/
    const val DXF_EXT = ".dxf"

    /**单个或者多个[LPElementBean]json结构的数据*/
    const val LPBEAN_EXT = ".lpbean"

    //endregion ---支持的文件类型---

    //region ---数据类型---

    /**数据类型, 真实数据
     * 则[com.angcyo.laserpacker.bean.LPElementBean.data]中存储的是直接可以发送给机器的数据*/
    const val DATA_TYPE_RAW = 0

    /**数据类型, 图片数据*/
    const val DATA_TYPE_BITMAP = 10010

    /**数据类型, 文本*/
    const val DATA_TYPE_TEXT = 10001

    /**数据类型, 二维码*/
    const val DATA_TYPE_QRCODE = 10015

    /**数据类型, 条形码*/
    const val DATA_TYPE_BARCODE = 10016

    /**数据类型, 矩形*/
    const val DATA_TYPE_RECT = 10005

    /**数据类型, 椭圆*/
    const val DATA_TYPE_OVAL = 10006

    /**数据类型, 线条*/
    const val DATA_TYPE_LINE = 10008

    /**数据类型, 钢笔*/
    const val DATA_TYPE_PEN = 10002

    /**数据类型, 画笔*/
    const val DATA_TYPE_BRUSH = 10003

    /**数据类型, SVG数据/导入进来的矢量元素*/
    const val DATA_TYPE_SVG = 10004

    /**数据类型, 多边形*/
    const val DATA_TYPE_POLYGON = 10007

    /**数据类型, 星星*/
    const val DATA_TYPE_PENTAGRAM = 10009

    /**数据类型, 爱心*/
    const val DATA_TYPE_LOVE = 10012

    /**数据类型, 单线字*/
    const val DATA_TYPE_SINGLE_WORD = 10013

    /**数据类型, GCODE数据*/
    const val DATA_TYPE_GCODE = 10020

    /**数据类型, Path数据*/
    const val DATA_TYPE_PATH = 7

    /**数据类型, 一组数据*/
    const val DATA_TYPE_GROUP = 10

    //endregion ---数据类型---

    //region ---数据处理模式---

    /**数数据模式, 黑白, 发送线段数据
     * 对应传输给机器的数据类型:
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_PATH]
     * */
    const val DATA_MODE_BLACK_WHITE = 1

    /**数据模式, 灰度
     * 对应传输给机器的数据类型:
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP]*/
    const val DATA_MODE_GREY = 3

    /**数据模式, 抖动, 发送抖动数据
     * 对应传输给机器的数据类型:
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING]
     * */
    const val DATA_MODE_DITHERING = 5

    /**数据模式, GCode, 发送GCode数据
     * 对应传输给机器的数据类型:
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.ENGRAVE_TYPE_GCODE]
     * */
    const val DATA_MODE_GCODE = 6

    //---算法支持, 但是机器不支持

    /**数据模式, 版画*/
    const val DATA_MODE_PRINT = 4

    /**数据模式, 印章*/
    const val DATA_MODE_SEAL = 2

    //endregion ---数据处理模式---

    //region ---中间数据扩展名---

    /**存放元素预览图的扩展名*/
    const val EXT_PREVIEW = ".png"

    /**存放元素转成数据后, 数据再次预览图的扩展名*/
    const val EXT_DATA_PREVIEW = ".p.png"

    /**图片路径数据*/
    const val EXT_BP = ".bp"

    /**抖动数据*/
    const val EXT_DT = ".dt"

    /**gcode数据*/
    const val EXT_GCODE = ".gcode"

    /**svg数据*/
    const val EXT_SVG = ".svg"

    //endregion ---中间数据扩展名---

    //region ---目录分配---

    /**雕刻缓存文件的文件夹*/
    const val ENGRAVE_FILE_FOLDER = "engrave"

    /**GCode/Svg矢量缓存目录*/
    const val VECTOR_FILE_FOLDER = "vector"

    /**雕刻传输数据缓存文件的文件夹*/
    const val ENGRAVE_TRANSFER_FILE_FOLDER = "transfer"

    /**工程文件存储目录*/
    const val PROJECT_FILE_FOLDER = "project"

    //endregion ---目录分配---

}

/**是否是GCode类型*/
fun String?.isGCodeType(): Boolean {
    this ?: return false
    return endsWith(LPDataConstant.EXT_GCODE, true) ||
            endsWith(".nc", true) ||
            endsWith(".gc", true)
}

/**
 * 支持[com.angcyo.laserpacker.bean.LPElementBean]
 * 支持[com.angcyo.laserpacker.bean.LPProjectBean]
 * 支持[List<LPProjectBean>]
 * */
typealias CanvasOpenDataType = Any

/**json字符串转换成[LPProjectBean]*/
fun String.toProjectBean() = fromJson<LPProjectBean>()

/**json字符串转换成[LPElementBean]*/
fun String.toElementBean() = fromJson<LPElementBean>()

/**json字符串转换成[List<LPElementBean>]*/
fun String.toElementBeanList() =
    fromJson<List<LPElementBean>>(listType(LPElementBean::class.java))

//---

fun Int.toDataTypeStr() = when (this) {
    LPDataConstant.DATA_TYPE_BITMAP -> "Bitmap"
    LPDataConstant.DATA_TYPE_QRCODE -> "QrCode"
    LPDataConstant.DATA_TYPE_BARCODE -> "BarCode"
    LPDataConstant.DATA_TYPE_TEXT -> "Text"
    LPDataConstant.DATA_TYPE_SVG -> "Svg"
    LPDataConstant.DATA_TYPE_GCODE -> "GCode"
    LPDataConstant.DATA_TYPE_PATH -> "Path"
    LPDataConstant.DATA_TYPE_GROUP -> "Group"
    else -> "DataType-${this}"
}

//---

/**对齐方式*/
fun Paint.Align.toAlignString(): String = when (this) {
    Paint.Align.CENTER -> "center"
    Paint.Align.LEFT -> "left"
    Paint.Align.RIGHT -> "right"
    else -> "left"
}

fun String?.toPaintAlign(): Paint.Align = when (this) {
    "center" -> Paint.Align.CENTER
    "left" -> Paint.Align.LEFT
    "right" -> Paint.Align.RIGHT
    else -> Paint.Align.LEFT
}

//---

/**画笔样式*/
fun Paint.Style.toPaintStyleInt(): Int = when (this) {
    Paint.Style.FILL -> 0
    Paint.Style.STROKE -> 1
    Paint.Style.FILL_AND_STROKE -> 2
    else -> 0
}

fun Int?.toPaintStyle(): Paint.Style = when (this) {
    0 -> Paint.Style.FILL
    1 -> Paint.Style.STROKE
    2 -> Paint.Style.FILL_AND_STROKE
    else -> Paint.Style.FILL
}

//---

/**是否加粗*/
fun LPElementBean.isBold() = fontWeight == "bold"

/**是否斜体*/
fun LPElementBean.isItalic() = fontStyle == "italic"

//--

/**[com.angcyo.laserpacker.bean.LPElementBean.mtype]类型转成字符串*/
fun Int.toTypeNameString() = when (this) {
    LPDataConstant.DATA_TYPE_BITMAP -> "Bitmap"
    LPDataConstant.DATA_TYPE_TEXT -> "Text"
    LPDataConstant.DATA_TYPE_QRCODE -> "QRCode"
    LPDataConstant.DATA_TYPE_BARCODE -> "BarCode"
    LPDataConstant.DATA_TYPE_RECT -> "Rect"
    LPDataConstant.DATA_TYPE_OVAL -> "Oval"
    LPDataConstant.DATA_TYPE_LINE -> "Line"
    LPDataConstant.DATA_TYPE_PEN -> "Pen"
    LPDataConstant.DATA_TYPE_BRUSH -> "Brush"
    LPDataConstant.DATA_TYPE_SVG -> "Svg"
    LPDataConstant.DATA_TYPE_POLYGON -> "Polygon"
    LPDataConstant.DATA_TYPE_PENTAGRAM -> "Pentagram"
    LPDataConstant.DATA_TYPE_LOVE -> "Love"
    LPDataConstant.DATA_TYPE_SINGLE_WORD -> "SingleWord"
    LPDataConstant.DATA_TYPE_GCODE -> "GCode"
    LPDataConstant.DATA_TYPE_PATH -> "Path"
    LPDataConstant.DATA_TYPE_RAW -> "Raw"
    else -> "Unknown"
}

//---

/**数据模式int, 转成对应的字符串*/
fun Int.toDataModeStr() = when (this) {
    LPDataConstant.DATA_MODE_PRINT -> "版画"
    LPDataConstant.DATA_MODE_BLACK_WHITE -> "黑白"
    LPDataConstant.DATA_MODE_DITHERING -> "抖动"
    LPDataConstant.DATA_MODE_GREY -> "灰度"
    LPDataConstant.DATA_MODE_SEAL -> "印章"
    LPDataConstant.DATA_MODE_GCODE -> "GCode"
    else -> "DataMode-${this}"
}

/**将雕刻类型字符串化*/
fun Int.toEngraveDataTypeStr() = when (this) {
    DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING -> "抖动"
    DataCmd.ENGRAVE_TYPE_GCODE -> "GCode"
    DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> "图片线段"
    DataCmd.ENGRAVE_TYPE_BITMAP -> "图片"
    else -> "未知"
}

/**构建元素的名称*/
fun List<LPElementBean>.generateName() {
    forEach {
        it.generateName(this)
    }
}

/**构建一个分组的名称*/
fun List<LPElementBean>.generateGroupName(baseName: String = "Group", index: Int? = null): String {
    val list = this
    val newName = if (index == null) baseName else "$baseName $index" //需要检测的新名字
    val find = list.find { it.groupName == newName }
    return if (find == null) {
        //未重名
        newName
    } else {
        //重名了
        generateGroupName(baseName, if (index == null) 2 else index + 1)
    }
}
