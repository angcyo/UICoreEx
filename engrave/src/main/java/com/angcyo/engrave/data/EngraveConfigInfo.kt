package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.transition.EngraveTransitionManager

/**
 * 雕刻参数配置信息
 * 比如: 选择的材质
 * 不同图层之间的雕刻参数
 *
 * [com.angcyo.engrave.model.TransferModel.createEngraveConfigInfo]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/30
 */
data class EngraveConfigInfo(
    /**所有待雕刻数据和对应的参数配置*/
    val engraveDataParamList: MutableList<EngraveDataParam> = mutableListOf()
) {

    /**获取有效数据的图层信息*/
    fun getLayerList(): List<EngraveLayerInfo> {
        val result = mutableListOf<EngraveLayerInfo>()
        engraveDataParamList.forEach { dataParam ->
            EngraveTransitionManager.engraveLayerList.find { it.mode == dataParam.layerMode }?.let {
                result.add(it)
            }
        }
        return result
    }

    /**获取雕刻图层下的雕刻数据和配置信息*/
    fun getEngraveDataByLayerMode(layerMode: Int): EngraveDataParam? {
        return engraveDataParamList.find { it.layerMode == layerMode }
    }

    /**根据指定的索引, 获取对应的雕刻参数和对应点雕刻数据, 此时返回的数据中[dataList]只有一个值 */
    fun getEngraveParamAndData(index: Int): EngraveDataParam? {
        var currentIndex = 0
        engraveDataParamList.forEach { dataParam ->
            dataParam.dataList.forEach { dataInfo ->
                if (currentIndex == index) {
                    return dataParam.copy().apply {
                        dataList = listOf(dataInfo)
                    }
                } else {
                    currentIndex++
                }
            }
        }
        return null
    }

    /**获取总进度*/
    fun getTotalProgress(): Int {
        var result = 0
        engraveDataParamList.forEach { dataParam ->
            dataParam.dataList.forEach {
                result += 100 * dataParam.time
            }
        }
        return result
    }

    /**获取当前索引数据之前的总进度*/
    fun getBeforeTotalProgress(index: Int): Int {
        var result = 0
        engraveDataParamList.forEach { dataParam ->
            dataParam.dataList.forEach {
                if (it.index == index) {
                    return result
                }
                result += 100 * dataParam.time
            }
        }
        return result
    }
}

/**各个图层的雕刻参数*/
data class EngraveDataParam(
    /**图层模式
     * [com.angcyo.engrave.data.EngraveLayerInfo.mode]
     * */
    var layerMode: Int,
    /**材质名称*/
    var materialName: String,
    /**图层内,待雕刻的数据集合*/
    var dataList: List<TransferDataInfo>,
    //---
    //l_type：雕刻激光类型选择，0为1064nm激光 (白光-雕)，1为450nm激光 (蓝光-烧)。(L3max新增)
    var type: Byte = LaserPeckerHelper.LASER_TYPE_BLUE,
    /**雕刻物体直径, 这里用像素作为单位
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.diameter]
     *
     * [com.angcyo.engrave.data.EngraveDataInfo.width]
     * [com.angcyo.engrave.data.EngraveDataInfo.height]
     * */
    var diameterPixel: Float = 0f,
    /**雕刻精度[1~5]*/
    var precision: Int = 1,
    //---
    //功率 100% [0~100]
    var power: Int = 100,
    //打印深度 10% [0~100]
    //com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd.toHexCommandString
    var depth: Int = 10,
    //打印次数, 最大255
    var time: Int = 1,
)
