package com.angcyo.engrave.data

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.library.annotation.Implementation
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity

/**
 * 雕刻准备数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/06
 */
data class EngraveReadyInfo(

    //---item相关属性---

    /**Canvas中对应的[com.angcyo.canvas.items.BaseItem]的uuid
     * 准备雕刻此[BaseItem]对应的数据*/
    var itemUuid: String? = null,

    /**数据类型, 文本 图片 svg gcode等
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_BITMAP]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_SVG]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_GCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_TYPE_PATH]
     * */
    var dataType: Int = 0,

    /**数据处理的模式, 比如是GCode数据, 黑白数据, 灰度数据等
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GREY]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
     * */
    var dataMode: Int = 0,

    //

    /**标识是来自历史文档的记录*/
    var historyEntity: EngraveHistoryEntity? = null,

    //---数据相关---

    /**切换不同分辨率, 模式后的图片
     * 用来在雕刻时预览效果, 暂时未使用*/
    @Implementation
    var dataBitmap: Bitmap? = null,

    /**[engraveData]数据存储的路径, 方便在历史文档中恢复数据
     * 如果index未找到数据, 则使用此路径的数据重发数据
     * [ByteArray] 数据路径*/
    var dataPath: String? = null,

    /**操作数据时的原始x,y*/
    var dataX: Int = 0,
    var dataY: Int = 0,

    /**真实需要雕刻的数据*/
    var engraveData: EngraveDataInfo? = null,

    //---预览相关属性---

    /**当前数据格式[dataType]下支持的数据其他处理模式[dataMode]*/
    @Implementation
    var dataSupportModeList: List<Int>? = null,

    /**当前数据支持像素调整列表*/
    @Implementation
    var dataSupportPxList: List<PxInfo>? = null,

    //---记录相关属性---

    /**历史文档预览的图片路径*/
    @Implementation
    var previewDataPath: String? = null,

    /**开始雕刻的时间, 毫秒*/
    var startEngraveTime: Long = -1,
    /**结束雕刻的时间, 毫秒*/
    var stopEngraveTime: Long = -1,
    /**当前数据已经雕刻的次数*/
    var printTimes: Int = 0,
)
