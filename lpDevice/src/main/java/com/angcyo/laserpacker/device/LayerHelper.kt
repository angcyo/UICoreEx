package com.angcyo.laserpacker.device

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.core.vmApp
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.library.ex._string

/**
 * 图层助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/19
 */
object LayerHelper {

    /**填充图层id*/
    const val LAYER_FILL = "layerFill"

    /**图片图层id*/
    const val LAYER_PICTURE = "layerPicture"

    /**线条图层id*/
    const val LAYER_LINE = "layerLine"

    /**切割图层id*/
    const val LAYER_CUT = "layerCut"

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
}

/**移除切割图层*/
fun MutableList<EngraveLayerInfo>.removeCutLayer(): MutableList<EngraveLayerInfo> {
    removeAll { it.layerId == LayerHelper.LAYER_CUT }
    return this
}

/**将数据模式, 转换成对应的图层id
 * [LPDataConstant.DATA_MODE_BLACK_WHITE]
 * [LPDataConstant.DATA_MODE_GREY]*/
fun Int?.toLayerId(): String? {
    return when (this) {
        LPDataConstant.DATA_MODE_BLACK_WHITE -> LayerHelper.LAYER_FILL
        LPDataConstant.DATA_MODE_GREY, LPDataConstant.DATA_MODE_DITHERING -> LayerHelper.LAYER_PICTURE
        LPDataConstant.DATA_MODE_GCODE -> LayerHelper.LAYER_LINE
        else -> null
    }
}

/**[EngraveLayerInfo]*/
fun String?.toLayerInfo() = LayerHelper.getEngraveLayerInfo(layerId = this)

/**将图层id转换成对应的数据模式*/
fun String?.toDataMode(): Int {
    return when (this) {
        LayerHelper.LAYER_PICTURE -> LPDataConstant.DATA_MODE_DITHERING
        LayerHelper.LAYER_LINE, LayerHelper.LAYER_CUT -> LPDataConstant.DATA_MODE_GCODE
        //LayerHelper.LAYER_FILL
        else -> LPDataConstant.DATA_MODE_BLACK_WHITE
    }
}