package com.angcyo.engrave.data

/**
 * 传输数据任务信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
data class TransferTaskData(
    /**雕刻图层信息*/
    val layerInfo: EngraveLayerInfo,
    /**图层对应的数据, 有些图层会合并数据/有些不会*/
    val transferDataList: List<TransferDataInfo>
)
