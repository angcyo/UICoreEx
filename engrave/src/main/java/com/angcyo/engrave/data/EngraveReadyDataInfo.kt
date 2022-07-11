package com.angcyo.engrave.data

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity

/**
 * 雕刻准备数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/06
 */
data class EngraveReadyDataInfo(

    /**来自历史文档的记录*/
    var historyEntity: EngraveHistoryEntity? = null,

    /**真实需要雕刻的数据*/
    var engraveData: EngraveDataInfo? = null,

    //---预览相关属性---

    /**切换不同分辨率, 模式后的预览图片*/
    var optionBitmap: Bitmap? = null,
    /**操作数据时的原始x,y*/
    var optionX: Int = 0,
    var optionY: Int = 0,

    /**数据处理的模式, 比如是GCode数据, 黑白数据, 灰度数据等
     * [com.angcyo.canvas.utils.CanvasConstant.BITMAP_MODE_GREY]
     * [com.angcyo.canvas.utils.CanvasConstant.BITMAP_MODE_BLACK_WHITE]
     * [com.angcyo.canvas.utils.CanvasConstant.BITMAP_MODE_GCODE]
     * */
    var optionMode: Int? = null,

    /**当前数据格式[dataType]下支持的数据其他处理模式[optionMode]*/
    var optionSupportModeList: List<Int>? = null,

    /**当前数据支持像素调整列表*/
    var optionSupportPxList: List<PxInfo>? = null,

    //---记录相关属性---

    /**[data]数据存储的路径, 方便在历史文档中恢复数据*/
    var dataPath: String? = null,
    /**历史文档预览的图片路径*/
    var previewDataPath: String? = null,

    /**Canvas中对应的[com.angcyo.canvas.items.BaseItem]的uuid*/
    var rendererItemUuid: String? = null,
    /**开始雕刻的时间, 毫秒*/
    var startEngraveTime: Long = -1,
    /**结束雕刻的时间, 毫秒*/
    var stopEngraveTime: Long = -1,
    /**当前数据已经雕刻的次数*/
    var printTimes: Int = 0,
)
