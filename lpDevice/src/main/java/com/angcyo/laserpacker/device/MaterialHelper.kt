package com.angcyo.laserpacker.device

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.toDpiScale
import com.angcyo.core.vmApp
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.ex.readAssets
import com.angcyo.library.ex.resetAll
import com.angcyo.library.unit.unitDecimal
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfig
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
import com.angcyo.objectbox.laser.pecker.entity.EntitySync
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity_
import kotlin.math.max

/**
 * 材质助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/30
 */
object MaterialHelper {

    //region ---材质相关---

    /**缓存*/
    val materialList = mutableListOf<MaterialEntity>()

    /**相同的材质key, 合并后的集合*/
    val unionMaterialList = mutableListOf<MaterialEntity>()

    /**设备连接上之后, 初始化材质推荐参数
     * [com.angcyo.laserpacker.device.model.FscDeviceModel.initDevice]
     * */
    fun initMaterial() {
        materialList.resetAll(getProductMaterialList(vmApp<LaserPeckerModel>().productInfoData.value))
        unionMaterialList.clear()
        materialList.filterTo(unionMaterialList) { entity ->
            unionMaterialList.find {
                if (it._isCustomMaterial) {
                    //用户自定义的材质, 只需要key一样即可, 不根据光源不一样分开存储
                    it.key == entity.key
                } else {
                    //系统推荐的材质, 不同的光源可能材质名称一样, 所以需要区分
                    it.key == entity.key && it.type == entity.type
                }
            } == null
        }
    }

    /**获取列表中的自定义材质*/
    fun getCustomMaterialList(): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        for (layerInfo in LayerHelper.engraveLayerList) {
            result.add(materialList.find {
                it.layerId == layerInfo.layerId && it.key == "custom"
            } ?: getLayerMaterialList(layerInfo.layerId).first())
        }
        return result
    }

    /**获取指定图层下的有效材质列表*/
    fun getLayerMaterialList(
        layerId: String,
        dpi: Float = HawkEngraveKeys.getLastLayerDpi(layerId)
    ): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        result.addAll(materialList.filter {
            it.layerId == layerId && (it.dpi == dpi || it.dpi == 0f)
        })
        if (result.isEmpty()) {
            //如果为空, 则获取一个自定义的材质参数
            result.addAll(materialList.filter {
                it.layerId == layerId && it.key == "custom"
            })
        }
        if (result.isEmpty()) {
            //如果为空, 则创建一个自定义的材质参数
            result.add(createLayerMaterial(layerId, dpi))
        }
        return result
    }

    /**获取产品材质参数配置的名称*/
    fun getProductMaterialConfigName(product: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): List<String> {
        val name = product?.name ?: return emptyList()
        val result = mutableListOf<String>()
        product.laserTypeList.forEach {
            val configName = it.getConfigFileName(name)
            result.add(configName)
        }
        return result
    }

    /**获取连上的设备推荐参数列表
     * [com.angcyo.engrave2.EngraveFlowDataHelper.saveEngraveConfigToMaterial]
     * [com.angcyo.canvas2.laser.pecker.engrave.config.EngraveConfigProvider.getEngraveMaterialList]
     * */
    fun getProductMaterialList(product: LaserPeckerProductInfo?): List<MaterialEntity> {
        //必有一个自定义的参数
        val result = mutableListOf<MaterialEntity>()
        result.addAll(createLayerMaterialList())
        val name = product?.name ?: return result

        //用户自定义的参数
        result.addAll(MaterialEntity::class.findAll(LPBox.PACKAGE_NAME) {
            orderDesc(MaterialEntity_.entityId)//id降序
            apply(
                MaterialEntity_.productName.equal(name)
                    .and(MaterialEntity_.isDelete.equal(false))
                    .and(MaterialEntity_.userId.equal("${EntitySync.userId}"))
            )
        })

        val isLPSeries = vmApp<LaserPeckerModel>().isLPSeries()
        //val isCSeries = vmApp<LaserPeckerModel>().isCSeries()

        //系统的推荐参数
        product.laserTypeList.forEach {
            val configName = it.getConfigFileName(name)
            val json = "argument/${configName}"
            L.w("读取材质:${json}")
            val text =
                LaserPeckerConfigHelper.readMaterialConfig(configName) ?: app().readAssets(json)
            val list = text?.fromJson<List<MaterialEntity>>(listType(MaterialEntity::class))
            list?.let {
                result.addAll(list)
                for (materialEntity in list) {
                    materialEntity.materialType = MaterialEntity.MATERIAL_TYPE_SYSTEM
                    if (materialEntity.dpi <= 0f) {
                        val dpi = materialEntity.dpiScale * LaserPeckerHelper.DPI_254
                        materialEntity.dpi = dpi
                        materialEntity.layerId?.let {
                            materialEntity.initLayerDpi(
                                it,
                                materialEntity.dpiScale * LaserPeckerHelper.DPI_254
                            )
                        }
                    }
                    if (isLPSeries && materialEntity.layerId == LayerHelper.LAYER_FILL) {
                        //填充图层, 和GCode图层/切割的参数一致
                        materialEntity.copy().apply {
                            layerId = LayerHelper.LAYER_LINE
                            result.add(this)
                        }
                    }
                }
            }
        }

        /*if (result.isEmpty()) {
            //如果为空, 则创建一个自定义的材质参数
            result.addAll(createCustomMaterial())
        }*/
        return result
    }

    /**获取一个材质*/
    fun getMaterialByCode(taskId: String?, code: String?): MaterialEntity? {
        return MaterialEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                MaterialEntity_.taskId.equal("$taskId").and(MaterialEntity_.code.equal("$code"))
            )
        }
    }

    /**获取一个材质*/
    fun getMaterialByKey(materialKey: String?): MaterialEntity? {
        if (materialKey.isNullOrBlank()) {
            return null
        }
        return MaterialEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(MaterialEntity_.key.equal(materialKey))
        } ?: materialList.find { it.key == materialKey }
    }

    /**创建一个自定义的材质*/
    fun createCustomLayerMaterialList(): List<MaterialEntity> = createLayerMaterialList {
        //default
        it.resId = R.string.custom
        it.resIdStr = "custom"
        it.key = "custom"
        it.createMaterialCode(it.key!!)
    }

    /**创建一个指定的图层材质参数, 使用上一次的图层参数*/
    fun createLayerMaterial(
        layerId: String,
        dpi: Float,
        config: (entity: MaterialEntity) -> Unit = {}
    ): MaterialEntity {
        return MaterialEntity().apply {
            //1:
            initLayerDpi(layerId, dpi)

            //2: 优先使用上一次的参数
            val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
            val lastEngraveConfig = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
                if (productName.isNullOrBlank()) {
                    //使用最后一次图层的参数
                    apply(EngraveConfigEntity_.layerId.equal(layerId))
                } else {
                    //使用最后一次产品图层的参数
                    apply(
                        EngraveConfigEntity_.layerId.equal(layerId)
                            .and(EngraveConfigEntity_.productName.equal(productName))
                    )
                }
            }
            power = lastEngraveConfig?.power ?: HawkEngraveKeys.lastPower
            depth = lastEngraveConfig?.depth ?: HawkEngraveKeys.lastDepth
            precision = lastEngraveConfig?.precision ?: HawkEngraveKeys.lastPrecision
            type = (lastEngraveConfig?.type ?: DeviceHelper.getProductLaserType()).toInt()
            count = max(1, lastEngraveConfig?.time ?: count)

            //3: 先用自定义占位
            val materialEntity = getMaterialByKey(lastEngraveConfig?.materialKey)
            resId = materialEntity?.resId ?: R.string.custom
            resIdStr = materialEntity?.resIdStr ?: "custom"
            key = materialEntity?.key ?: "custom"
            name = materialEntity?.name ?: name

            //productName //用来区分是否是自定义的材质
            //isCustomMaterial
            materialType = MaterialEntity.MATERIAL_TYPE_TEMP

            //配置
            config(this)
            //生成唯一码
            if (code.isBlank()) {
                createMaterialCode(key!!)
            }
        }
    }

    /**创建一个材质, 未入库*/
    fun createLayerMaterialList(config: (entity: MaterialEntity) -> Unit = {}): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        //一个材质, 需要包含所有图层的参数
        for (layerInfo in LayerHelper.engraveLayerList) {
            val layerId = layerInfo.layerId
            val dpi = layerId.filterLayerDpi(
                HawkEngraveKeys.lastDpiLayerJson?.getLayerConfig(layerId)?.dpi
                    ?: LaserPeckerHelper.DPI_254
            )
            val entity = createLayerMaterial(layerId, dpi, config)
            result.add(entity)
        }
        return result
    }

    /**获取材质列表, 过滤指定的dpi和光源
     * [type] 激光类型/光源 0:蓝光 1:白光, 不指定则都要
     * [LaserPeckerHelper.LASER_TYPE_WHITE] 0x01 白光
     * [LaserPeckerHelper.LASER_TYPE_BLUE] 0x00 蓝光
     * */
    fun filterMaterialList(
        materialKey: String?,
        dpi: Float,
        dpiScale: Float,
        type: Int?
    ): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        materialList.forEach {
            if (it.key == materialKey) {
                if (it.dpi > 0) {
                    //材质指定了dpi, 则必须一致
                    if (it.dpi == dpi) {
                        if (type == null || type == it.type) {
                            result.add(it)
                        }
                    }
                } else if (it.dpiScale <= 0 || it.dpiScale == dpiScale) {
                    //材质未指定dpi时, 可能是用户自定义的材质, 则返回
                    if (type == null || type == it.type) {
                        result.add(it)
                    }
                }
            }
        }
        return result
    }

    /**获取材质在对应列表中的索引*/
    fun indexOfMaterial(materialList: List<MaterialEntity>, materialEntity: MaterialEntity): Int {
        return indexOfMaterial(
            materialList,
            materialEntity.code,
            materialEntity.key,
            materialEntity.type
        )
    }

    /**获取材质在对应列表中的索引
     * [filterMaterialList]
     * */
    fun indexOfMaterial(
        materialList: List<MaterialEntity>,
        materialCode: String?,
        materialKey: String?,
        type: Int?
    ): Int {
        var index =
            materialList.indexOfFirst { it.code == materialCode && (type == null || type == it.type) }
        if (index != -1) {
            return index
        }
        index =
            materialList.indexOfFirst { it.code == materialCode }
        if (index != -1) {
            return index
        }
        index =
            materialList.indexOfFirst { it.key == materialKey && (type == null || type == it.type) }
        return max(0, index)
    }

    //endregion ---材质相关---

    /**创建一个自定义的材质Code, 唯一标识符*/
    fun MaterialEntity.createMaterialCode(materialName: String) {
        code = buildString {
            append("${materialName}_")
            append(if (type == LaserPeckerHelper.LASER_TYPE_BLUE.toInt()) "blue_" else "white_")
            append(layerId)
            //append("_${dpi}_")
            append("_")
            append(dpiScale.unitDecimal(1))
        }
    }
}

/**过滤一下图层对应的dpi*/
fun String.filterLayerDpi(dpi: Float): Float =
    if (this == LayerHelper.LAYER_LINE || this == LayerHelper.LAYER_CUT) {
        LaserPeckerHelper.DPI_254
    } else {
        dpi
    }

/**初始化对应的额dpi*/
fun MaterialEntity.initLayerDpi(layerId: String, dpi: Float) {
    this.layerId = layerId
    this.dpi = layerId.filterLayerDpi(dpi)
    this.dpiScale = this.dpi.toDpiScale()
}