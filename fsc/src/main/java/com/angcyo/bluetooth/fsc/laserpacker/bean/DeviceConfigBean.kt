package com.angcyo.bluetooth.fsc.laserpacker.bean

import androidx.annotation.Keep
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.size

/**
 * 设备配置信息结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/11
 */

@Keep
data class DeviceConfigBean(

    var softwareVersionRange: String? = null,
    var hardwareVersionRange: String? = null,

    /**当前设备支持的激光模块类型信息*/
    var laserTypeList: List<LaserTypeInfo>? = null,
    var focalDistance: Int? = 0,
    var name: String? = null,
    var des: String? = null,

    /**是否支持切割图层*/
    var supportCut: Boolean = false,

    /**支持的外设, 决定默认的设置开关flag默认值*/
    var ex: String? = null,

    /**需要旋转的数据外设, 当这些外设的使能开关打开时, 需要旋转数据
     * [ex]*/
    var dataRotateEx: String? = null,

    /**第三轴的模式*/
    var zDirList: String? = null,

    var supportDitheringRange: String? = null,
    /**设备尺寸*/
    @MM
    var widthPhys: Int = 0,
    var heightPhys: Int = 0,
    /**最佳物理尺寸*/
    @MM
    var bestWidthPhys: Int = 0,
    var bestHeightPhys: Int = 0,

    /**最佳尺寸的svg path.
     * [M10,10L100,100] 值是mm单位*/
    @MM
    var bestPhysPath: String? = null,
    /**默认模式下的提示框*/
    var tipPath: String? = null,
    /**z轴连接时限制提示框, 优先处理此字段, 再处理[zMaxWidth]等*/
    var zTipPath: String? = null,
    var sTipPath: String? = null,
    var sRepTipPath: String? = null,
    var rTipPath: String? = null,
    var carTipPath: String? = null,
    var penTipPath: String? = null,

    /**有效范围宽高比例, 在[bestWidthPhys] [bestHeightPhys]中*/
    var validWidthRatio: Float = 0f,
    var validHeightRatio: Float = 0f,
    @MM
    var zMaxWidth: Int = 0,
    var zMaxHeight: Int = 0,
    var rMaxWidth: Int = 0,
    var rMaxHeight: Int = 0,
    var sMaxWidth: Int = 0,
    var sMaxHeight: Int = 0,
    var carMaxWidth: Int = 0,
    var carMaxHeight: Int = 0,
    var penMaxHeight: Int = 0,

    /**2023-11-7 支持的最大速度*/
    var supportMaxSpeed: Int? = null,

    /**未指定配置dpi时. 默认的dpi分辨率列表*/
    var dpiList: List<PxInfo>? = null,

    /**2023-8-11
     * 是否是单模块设备, 单模块设备, 不需要切换模块, 无光源切换, 且材质推荐只有对应模块的残值
     * */
    var isSingleModule: Boolean = false,

    /**
     * [com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys.enablePreviewDebounce]
     * [com.angcyo.canvas2.laser.pecker.engrave.LPPreviewHelper.updatePreviewByRenderer]
     * */
    var enablePreviewDebounce: Boolean = false,

    /**是否要使用GCode切割指令
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.Companion.gcodeData]*/
    var useCutCmd: Boolean = false,

    /**是否要使用GCode切割数据
     * [com.angcyo.gcode.GCodeWriteHandler.enableGCodeCut]*/
    var useCutData: Boolean = false,

    /**切割数据循环次数*/
    var cutLoopCount: Int? = null,

    /**切割数据的宽度*/
    var cutGCodeWidth: Float? = null,

    /**切割数据的高度*/
    var cutGCodeHeight: Float? = null,

    /**是否使用批量雕刻属性*/
    var useBatchEngraveCmd: Boolean? = null,

    /**切片的粒度*/
    var sliceGranularity: Int? = 1,

    /**GCode切片填充的线距*/
    var gcodeLineSpace: Double? = null,

    //2023-5-19 图层信息

    /**每个图层单独对应的[dpiList]
     *
     * [key] 图层id
     * [value] [LayerConfigBean]
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_FILL]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_PICTURE]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_LINE]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_CUT]
     * */
    var layer: HashMap<String, LayerConfigBean>? = null,

    /**
     * 风速级别配置
     * [key] 图层id
     * */
    var pumpMap: HashMap<String, List<PumpConfigBean>>? = null,

    //2023-10-30

    /**出光频率设置列表*/
    var laserFrequencyList: List<Int>? = null,

    /**最大支持的文件发送大小字节*/
    var maxTransferDataSize: Long? = null,
) {

    /**是否有特殊图层配置*/
    fun haveLayerConfig(): Boolean {
        return layer?.isNotEmpty() == true
    }

    /**指定的图层[layerId] 是否有特殊的配置*/
    fun haveLayerConfig(layerId: String): Boolean {
        return layer?.get(layerId) != null
    }

    /**获取指定图层的配置信息
     * [layerId] 图层id
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.findProductLayerSupportPxList]
     * */
    fun getLayerConfig(layerId: String): LayerConfigBean {
        layer?.get(layerId)?.let {
            return it.filterDpiList()
        }
        return LayerConfigBean(layerId, dpiList?.filterModuleDpiList())
    }
}

val _useCutCmd: Boolean
    get() = _deviceConfigBean?.useCutCmd == true

val _useCutData: Boolean
    get() = _deviceConfigBean?.useCutData == true

val _cutLoopCount: Int?
    get() = _deviceConfigBean?.cutLoopCount

val _cutGCodeWidth: Float?
    get() = _deviceConfigBean?.cutGCodeWidth

val _cutGCodeHeight: Float?
    get() = _deviceConfigBean?.cutGCodeHeight
val _sliceGranularity: Int?
    get() = _deviceConfigBean?.sliceGranularity
val _gcodeLineSpace: Double
    get() = _deviceConfigBean?.gcodeLineSpace ?: 0.125

/**将切片数量转换成对应的切片色阶阈值数组*/
fun Int.toSliceLevelList(): List<Int> {
    val max = LibHawkKeys.grayThreshold
    val count = this
    val step = maxOf(1, max / count)

    val list = mutableListOf<Int>()
    while (list.size() < count) {
        val value = maxOf(0, max - step * list.size())
        if (value == list.lastOrNull()) {
            break
        }
        list.add(value)
    }
    return list
}