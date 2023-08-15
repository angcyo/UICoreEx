package com.angcyo.laserpacker.device

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_CUT
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_FILL
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_LINE
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.LAYER_PICTURE
import com.angcyo.core.vmApp
import com.angcyo.http.base.toJson
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.bean.TransferLayerConfigBean
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfig
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfigList

/**
 * 图层助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/19
 */
object LayerHelper {

    /**图层, 以及图层顺序
     * 先 填充->抖动->GCode */
    val engraveLayerList = listOf(
        EngraveLayerInfo(LAYER_FILL, _string(R.string.engrave_layer_fill), showDpiConfig = true),
        EngraveLayerInfo(
            LAYER_PICTURE,
            _string(R.string.engrave_layer_bitmap),
            showDpiConfig = true
        ),
        EngraveLayerInfo(LAYER_LINE, _string(R.string.engrave_layer_line)),
        EngraveLayerInfo(LAYER_CUT, _string(R.string.engrave_layer_cut))
    )

    private val _resultLayerList = mutableListOf<EngraveLayerInfo>()

    /**获取参数能够描述的图层信息*/
    fun getEngraveLayerInfo(layerId: String? = null, mode: Int? = null): EngraveLayerInfo? {
        return if (layerId != null) {
            engraveLayerList.find { it.layerId == layerId }
        } else {
            engraveLayerList.find { it.layerId == mode.toLayerId() }
        }
    }

    /**图层列表
     * [includeCutLayer] 是否要切割图层*/
    fun getEngraveLayerList(includeCutLayer: Boolean = vmApp<DeviceStateModel>().haveCutLayer()): List<EngraveLayerInfo> {
        _resultLayerList.clear()
        _resultLayerList.addAll(engraveLayerList)
        if (!includeCutLayer) {
            //移除切割图层
            _resultLayerList.removeCutLayer()
        }
        return _resultLayerList
    }

    /**获取图层最后一次的dpi*/
    fun updateAllLayerDpi(dpi: Float): String? {
        for (layer in engraveLayerList) {
            HawkEngraveKeys.updateLayerDpi(layer.layerId, layer.layerId.filterLayerDpi(dpi))
        }
        return HawkEngraveKeys.lastDpiLayerJson
    }

    /**当前图层, 是否要显示dpi分辨率配置*/
    fun showDpiConfig(layerId: String?): Boolean {
        val laserInfo =
            vmApp<DeviceStateModel>().getDeviceLaserModule(LaserPeckerHelper.LASER_TYPE_BLUE)
        if (laserInfo?.isNotLaserModule() == true) {
            //不是激光模块
            return false
        }
        return getEngraveLayerInfo(layerId)?.showDpiConfig ?: false
    }

    /**根据最后一次的dpi, 过滤一下产品支持的dpi配置*/
    fun getProductLayerSupportPxJson(): String {
        val list = mutableListOf<TransferLayerConfigBean>()
        for (layer in engraveLayerList) {
            val layerId = layer.layerId
            val lastDpi = HawkEngraveKeys.getLastLayerDpi(layerId)
            val pxList = LaserPeckerHelper.findProductLayerSupportPxList(layerId)
            val find = pxList.find { it.dpi == lastDpi }
            if (find == null) {
                //最后一次的dpi, 不被支持
                list.add(
                    TransferLayerConfigBean(
                        layerId,
                        layerId.filterLayerDpi(LaserPeckerHelper.DPI_254)
                    )
                )
            } else {
                //最后一次的dpi, 支持
                list.add(TransferLayerConfigBean(layerId, layerId.filterLayerDpi(lastDpi)))
            }
        }
        return list.toJson()!!
    }

    /**获取产品支持的最后一次的图层dpi*/
    fun getProductLastLayerDpi(layerId: String?): Float {
        return getProductLayerSupportPxJson().getLayerConfig(layerId)?.dpi
            ?: LaserPeckerHelper.DPI_254
    }
}

/**移除切割图层*/
fun MutableList<EngraveLayerInfo>.removeCutLayer(): MutableList<EngraveLayerInfo> {
    removeAll { it.layerId == LAYER_CUT }
    return this
}

/**将数据模式, 转换成对应的图层id
 * [LPDataConstant.DATA_MODE_BLACK_WHITE]
 * [LPDataConstant.DATA_MODE_GREY]*/
fun Int?.toLayerId(): String? {
    return when (this) {
        LPDataConstant.DATA_MODE_BLACK_WHITE -> LAYER_FILL
        LPDataConstant.DATA_MODE_GREY, LPDataConstant.DATA_MODE_DITHERING -> LAYER_PICTURE
        LPDataConstant.DATA_MODE_GCODE -> LAYER_LINE
        else -> null
    }
}

/**[EngraveLayerInfo]*/
fun String?.toLayerInfo() = LayerHelper.getEngraveLayerInfo(layerId = this)

/**将图层id转换成对应的数据模式*/
fun String?.toDataMode(): Int {
    return when (this) {
        LAYER_PICTURE -> LPDataConstant.DATA_MODE_DITHERING
        LAYER_LINE, LAYER_CUT -> LPDataConstant.DATA_MODE_GCODE
        //LayerHelper.LAYER_FILL
        else -> LPDataConstant.DATA_MODE_BLACK_WHITE
    }
}

/**更新所有图层的dpi
 * [com.angcyo.laserpacker.device.filterLayerDpi]*/
fun String?.updateAllLayerConfig(dpi: Float): String? {
    val result = mutableListOf<TransferLayerConfigBean>()
    val list = this?.getLayerConfigList()
    if (list != null) {
        result.addAll(list)
    }
    for (bean in result) {
        bean.dpi = bean.layerId.filterLayerDpi(dpi)
    }
    return result.toJson()
}