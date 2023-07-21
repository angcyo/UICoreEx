package com.angcyo.canvas2.laser.pecker.engrave.config

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper
import com.angcyo.core.component.file.writeToLog
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.laserpacker.bean.LPElementBean
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.library.L
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveAllEntity

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/20
 */
class EngraveConfigProvider : IEngraveConfigProvider {

    override fun getTransferConfig(flowLayoutHelper: BaseFlowLayoutHelper): TransferConfigEntity {
        val delegate = flowLayoutHelper.engraveCanvasFragment?.renderDelegate
        val entity = LPEngraveHelper.generateTransferConfig(
            flowLayoutHelper.flowTaskId,//此时的flowTaskId可以为空
            delegate
        )
        flowLayoutHelper.projectBean?.apply {
            entity.name = file_name ?: entity.name
            entity.layerJson = getTransferLayerJson()
        }
        return entity
    }

    override fun onSaveTransferConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        configEntity: TransferConfigEntity
    ) {
        flowLayoutHelper.projectBean?.apply {
            file_name = configEntity.name
            updateOptionsFromTransferLayer(configEntity.getLayerConfigList())
        }
    }

    override fun getEngraveMaterial(flowLayoutHelper: BaseFlowLayoutHelper): MaterialEntity {
        val taskId = flowLayoutHelper.flowTaskId
        //默认选中材质, 获取任务之前已经选中的材质, 如果有
        var materialEntity = EngraveFlowDataHelper.findTaskMaterial(taskId)
        if (materialEntity == null) {
            val projectBean = flowLayoutHelper.projectBean
            val projectMaterialList = projectBean?.getProjectMaterialList()
            if (projectMaterialList.isNullOrEmpty()) {
                //未初始化材质信息, 默认使用第一个
                val lastMaterial = EngraveFlowDataHelper.findLastMaterial()
                materialEntity =
                    if (lastMaterial != null && MaterialHelper.materialList.find { it.key == lastMaterial.key } != null) {
                        //上一次设备推荐的材质, 在列表中
                        lastMaterial
                    } else {
                        //使用列表中第一个
                        MaterialHelper.materialList.firstOrNull()
                            ?: MaterialHelper.createCustomMaterial().first()
                    }
            } else {
                //工程中自带的材质信息
                for (entity in projectMaterialList) {
                    entity.taskId = taskId
                }
                projectMaterialList.lpSaveAllEntity()
                MaterialHelper.initMaterial()//重新初始化材质
                materialEntity = projectMaterialList.first()
            }

            "任务:${taskId} 默认材质:$materialEntity".writeToLog(logLevel = L.INFO)

            //雕刻配置信息
            EngraveFlowDataHelper.getOrGenerateEngraveConfigByMaterial(
                taskId,
                materialEntity.key,
                materialEntity
            )
        } else {
            //如果有材质, 则从材质中获取对应图层的配置
        }
        return materialEntity
    }

    override fun getEngraveConfig(flowLayoutHelper: BaseFlowLayoutHelper): List<EngraveConfigEntity> {
        val taskId = flowLayoutHelper.flowTaskId
        val list = EngraveFlowDataHelper.getTaskEngraveConfigList(taskId)
        if (list.isNotEmpty()) {
            //已经有配置, 则使用
        } else {
            val result = mutableListOf<EngraveConfigEntity>()
            for (layer in LayerHelper.engraveLayerList) {
                result.add(getEngraveConfig(flowLayoutHelper, layer.layerId))
            }
            return result
        }
        return list
    }

    override fun getEngraveConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        layerId: String
    ): EngraveConfigEntity {
        val taskId = flowLayoutHelper.flowTaskId
        return EngraveFlowDataHelper.generateEngraveConfig(taskId, layerId)
    }

    override fun onSaveEngraveConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        configEntity: EngraveConfigEntity
    ) {
        flowLayoutHelper.projectBean?.apply {
            updateOptionsFromEngraveConfig(configEntity)
        }
    }

    override fun onStartEngrave(flowLayoutHelper: BaseFlowLayoutHelper) {
        if (HawkEngraveKeys.enableItemEngraveParams) {
            LPEngraveHelper.generateEngraveConfig(
                flowLayoutHelper.engraveCanvasFragment?.renderDelegate
            )
        }
    }

    override fun getEngraveElementConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        element: LPElementBean
    ): EngraveConfigEntity {
        element.apply {
            printPower = printPower ?: HawkEngraveKeys.lastPower
            printDepth = printDepth ?: HawkEngraveKeys.lastDepth
            printPrecision = printPrecision ?: HawkEngraveKeys.lastPrecision
            printType = printType ?: DeviceHelper.getProductLaserType().toInt()
            printCount = printCount ?: 1

            val itemLayerId = _layerId
            materialKey = materialKey ?: MaterialHelper.createCustomMaterial()
                .find { it.layerId == itemLayerId }?.key
        }
        //雕刻配置
        return LPEngraveHelper.generateEngraveConfig("${element.index}", element)
    }

}