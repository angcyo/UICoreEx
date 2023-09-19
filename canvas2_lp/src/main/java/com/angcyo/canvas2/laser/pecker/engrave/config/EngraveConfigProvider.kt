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
import com.angcyo.laserpacker.device.initLayerDpi
import com.angcyo.library.L
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfigList
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EntitySync
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity_
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveAllEntity
import com.angcyo.objectbox.removeAll

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

    /**开始传输数据
     * [onStartEngrave]*/
    override fun onSaveTransferConfig(
        flowLayoutHelper: BaseFlowLayoutHelper,
        configEntity: TransferConfigEntity
    ) {
        if (flowLayoutHelper._isSingleItemFlow) {
            LPEngraveHelper.generateTransferConfig(
                flowLayoutHelper.engraveCanvasFragment?.renderDelegate,
                flowLayoutHelper.flowTaskId
            )
        } else {
            flowLayoutHelper.projectBean?.apply {
                file_name = configEntity.name
                updateOptionsFromTransferLayer(configEntity.layerJson.getLayerConfigList())
            }
        }
    }

    override fun getEngraveMaterialList(flowLayoutHelper: IEngraveConfigTaskProvider): List<MaterialEntity> {
        val taskId = flowLayoutHelper.engraveConfigTaskId
        //默认选中材质, 获取任务之前已经选中的材质, 如果有
        var materialEntityList = EngraveFlowDataHelper.findTaskMaterialList(taskId)
        if (materialEntityList.isNullOrEmpty()) {
            val projectBean = flowLayoutHelper.engraveConfigProjectBean
            val projectMaterialList = projectBean?.getProjectMaterialList()
            if (projectMaterialList.isNullOrEmpty()) {
                //未初始化材质信息, 默认使用第一个
                MaterialHelper.initMaterial()//初始化材质, 用来更新上一次雕刻使用的自定义材质信息
                materialEntityList = MaterialHelper.getCustomMaterialList()
            } else {
                //工程中自带的材质信息
                MaterialEntity::class.removeAll(LPBox.PACKAGE_NAME) {
                    apply(MaterialEntity_.taskId.equal("$taskId"))
                }
                for (entity in projectMaterialList) {
                    entity.layerId?.let { entity.initLayerDpi(it, entity.dpi) }
                    entity.taskId = taskId
                    entity.materialType = MaterialEntity.MATERIAL_TYPE_CUSTOM
                    entity.userId = "${EntitySync.userId}"
                }
                projectMaterialList.lpSaveAllEntity()
                MaterialHelper.initMaterial()//重新初始化材质
                materialEntityList = projectMaterialList
            }

            "任务:${taskId} 默认材质:$materialEntityList".writeToLog(logLevel = L.INFO)

            //创建雕刻配置信息
            EngraveFlowDataHelper.getOrGenerateEngraveConfigByMaterial(taskId, materialEntityList)
        } else {
            //如果有材质, 则从材质中获取对应图层的配置
        }
        return materialEntityList
    }

    override fun getEngraveMaterial(
        flowLayoutHelper: IEngraveConfigTaskProvider,
        layerId: String
    ): MaterialEntity = getEngraveMaterialList(flowLayoutHelper).find { it.layerId == layerId }
        ?: MaterialHelper.getLayerMaterialList(layerId).last()

    override fun getEngraveConfigList(flowLayoutHelper: IEngraveConfigTaskProvider): List<EngraveConfigEntity> {
        val taskId = flowLayoutHelper.engraveConfigTaskId
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
        flowLayoutHelper: IEngraveConfigTaskProvider,
        layerId: String
    ): EngraveConfigEntity {
        val taskId = flowLayoutHelper.engraveConfigTaskId
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

    /**开始雕刻
     * [onSaveTransferConfig]*/
    override fun onStartEngrave(flowLayoutHelper: BaseFlowLayoutHelper) {
        if (flowLayoutHelper._isSingleItemFlow) {
            LPEngraveHelper.generateEngraveConfig(
                flowLayoutHelper.engraveCanvasFragment?.renderDelegate,
                flowLayoutHelper.flowTaskId
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
            val materialEntity =
                MaterialHelper.createCustomLayerMaterialList().find { it.layerId == itemLayerId }
            materialKey = materialKey ?: materialEntity?.key
            materialName = materialName ?: materialEntity?.name
            materialCode = materialCode ?: materialEntity?.code
        }
        //雕刻配置
        return LPEngraveHelper.generateEngraveConfig("${element.index}", element)
    }

}