package com.angcyo.bluetooth.fsc.laserpacker.bean

import androidx.annotation.Keep
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.PxInfo
import com.angcyo.library.annotation.MM

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

    /**支持的外设*/
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

    /**未指定配置dpi时. 默认的dpi分辨率列表*/
    var dpiList: List<PxInfo>? = null,

    /**2023-8-11
     * 是否是单模块设备, 单模块设备, 不需要切换模块, 无光源切换, 且材质推荐只有对应模块的残值
     * */
    var isSingleModule: Boolean = false,

    /**
     * [com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys.enablePreviewDebounce]
     * */
    var enablePreviewDebounce: Boolean = false,

    /**是否要使用GCode切割指令
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.Companion.gcodeData]*/
    var useCutCmd: Boolean = false,

    /**是否要使用GCode切割数据
     * [com.angcyo.gcode.GCodeWriteHandler.enableGCodeCut]*/
    var useCutData: Boolean = false,

    //2023-5-19 图层信息

    /**每个图层单独对应的[dpiList]
     *
     * [key] 图层id
     * [value] [LayerConfigBean]
     *
     * [com.angcyo.laserpacker.device.LayerHelper.LAYER_FILL]
     * [com.angcyo.laserpacker.device.LayerHelper.LAYER_PICTURE]
     * [com.angcyo.laserpacker.device.LayerHelper.LAYER_LINE]
     * [com.angcyo.laserpacker.device.LayerHelper.LAYER_CUT]
     * */
    var layer: HashMap<String, LayerConfigBean>? = null
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
     * [layerId] 图层id*/
    fun getLayerConfig(layerId: String): LayerConfigBean {
        layer?.get(layerId)?.let {
            return it.filterDpiList()
        }
        return LayerConfigBean(layerId, dpiList?.filterModuleDpiList())
    }
}

val _useCutData: Boolean
    get() = _deviceConfigBean?.useCutData == true

val _useCutCmd: Boolean
    get() = _deviceConfigBean?.useCutCmd == true