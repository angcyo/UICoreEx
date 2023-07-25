package com.angcyo.canvas2.laser.pecker.engrave.config

import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity

/**
 * 雕刻所有配置提供者
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/20
 */
interface IEngraveConfigProvider {

    //region ---传输配置---

    /**获取流程的传输配置*/
    fun getTransferConfig(flowLayoutHelper: BaseFlowLayoutHelper): TransferConfigEntity

    /**保存传输配置时的回调*/
    fun onSaveTransferConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        configEntity: TransferConfigEntity
    ) {
    }

    //endregion ---传输配置---

    //region ---雕刻参数配置---

    /**获取雕刻应用的材质信息, 每个图层都有对应的材质*/
    fun getEngraveMaterialList(flowLayoutHelper: BaseFlowLayoutHelper): List<MaterialEntity>

    /**[getEngraveMaterialList]*/
    fun getEngraveMaterial(flowLayoutHelper: BaseFlowLayoutHelper, layerId: String): MaterialEntity

    /**获取流程对应的所有图层雕刻参数*/
    fun getEngraveConfigList(flowLayoutHelper: BaseFlowLayoutHelper): List<EngraveConfigEntity>

    /**获取指定图层的雕刻参数
     * [getEngraveConfigList]*/
    fun getEngraveConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        layerId: String
    ): EngraveConfigEntity

    /**保存雕刻配置时的回调*/
    fun onSaveEngraveConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        configEntity: EngraveConfigEntity
    ) {
    }

    /**开始雕刻前的回调*/
    fun onStartEngrave(flowLayoutHelper: BaseFlowLayoutHelper) {}

    //endregion ---雕刻参数配置---

    //region ---单元素雕刻参数配置---

    /**获取单元素的雕刻参数配置*/
    fun getEngraveElementConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        element: LPElementBean
    ): EngraveConfigEntity

    /**获取单元素的雕刻参数配置*/
    fun onSaveEngraveElementConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        element: LPElementBean?,
        configEntity: EngraveConfigEntity?
    ) {
    }

    //endregion ---单元素雕刻参数配置---


}