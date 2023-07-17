package com.angcyo.laserpacker.device

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.core.vmApp
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.ex.ensureInt
import com.angcyo.library.ex.readAssets
import com.angcyo.library.ex.resetAll
import com.angcyo.library.unit.unitDecimal
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
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
                if (it.isCustomMaterial) {
                    //用户自定义的材质, 只需要key一样即可, 不根据光源不一样分开存储
                    it.key == entity.key
                } else {
                    //系统推荐的材质, 不同的光源可能材质名称一样, 所以需要区分
                    it.key == entity.key && it.type == entity.type
                }
            } == null
        }
    }

    /**获取产品材质参数配置的名称*/
    fun getProductMaterialConfigName(product: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): List<String> {
        val name = product?.name ?: return emptyList()
        val result = mutableListOf<String>()
        product.laserTypeList.forEach {
            val configName = "${name}_${it.wave}_${it.power.ensureInt()}.json"
            result.add(configName)
        }
        return result
    }

    /**获取连上的设备推荐参数列表*/
    fun getProductMaterialList(product: LaserPeckerProductInfo?): List<MaterialEntity> {
        val name = product?.name ?: return emptyList()
        val result = mutableListOf<MaterialEntity>()

        //用户自定义的参数
        result.addAll(MaterialEntity::class.findAll(LPBox.PACKAGE_NAME) {
            orderDesc(MaterialEntity_.entityId)//id降序
            apply(
                MaterialEntity_.productName.equal(name).and(MaterialEntity_.isDelete.equal(false))
            )
        })

        val isCSeries = vmApp<LaserPeckerModel>().isCSeries()

        //系统的推荐参数
        product.laserTypeList.forEach {
            val configName = "${name}_${it.wave}_${it.power.ensureInt()}.json"
            val json = "argument/${configName}"
            L.w("读取材质:${json}")
            val text =
                LaserPeckerConfigHelper.readMaterialConfig(configName) ?: app().readAssets(json)
            val list = text?.fromJson<List<MaterialEntity>>(listType(MaterialEntity::class))
            list?.let {
                result.addAll(list)
                for (materialEntity in list) {
                    if (materialEntity.layerId == LayerHelper.LAYER_FILL) {
                        //填充图层, 和GCode图层/切割的参数一致
                        materialEntity.copy().apply {
                            layerId = LayerHelper.LAYER_LINE
                            result.add(this)

                            if (isCSeries) {
                                //切割图层, 也使用GCode图层的参数
                                materialEntity.copy().apply {
                                    layerId = LayerHelper.LAYER_CUT
                                    result.add(this)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (result.isEmpty()) {
            //如果为空, 则创建一个自定义的材质参数
            result.addAll(createCustomMaterial())
        }
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
        return MaterialEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(MaterialEntity_.key.equal("$materialKey"))
        }
    }

    /**创建一个自定义的材质*/
    fun createCustomMaterial(): List<MaterialEntity> {
        //自定义, 自动记住了上一次的值
        val result = mutableListOf<MaterialEntity>()
        for (layerInfo in LayerHelper.getEngraveLayerList()) {
            MaterialEntity().apply {
                resId = R.string.custom
                key = "custom"
                type = DeviceHelper.getProductLaserType().toInt()
                power = HawkEngraveKeys.lastPower
                depth = HawkEngraveKeys.lastDepth
                precision = HawkEngraveKeys.lastPrecision
                layerId = layerInfo.layerId
                createMaterialCode(key!!)

                result.add(this)
            }
        }
        return result
    }


    /**获取材质列表
     * [type] 激光类型/光源 0:蓝光 1:白光, 不指定则都要
     * [LaserPeckerHelper.LASER_TYPE_WHITE] 0x01 白光
     * [LaserPeckerHelper.LASER_TYPE_BLUE] 0x00 蓝光
     * */
    fun getMaterialList(materialKey: String?, dpiScale: Float, type: Int?): List<MaterialEntity> {
        val result = mutableListOf<MaterialEntity>()
        materialList.forEach {
            if (it.key == materialKey) {
                if (it.dpiScale <= 0 || it.dpiScale == dpiScale) {
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
        return indexOfMaterial(materialList, materialEntity.key, materialEntity.type)
    }

    /**获取材质在对应列表中的索引
     * [getMaterialList]
     * */
    fun indexOfMaterial(materialList: List<MaterialEntity>, materialKey: String?, type: Int?): Int {
        val index =
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
            append(dpiScale.unitDecimal(1))
        }
    }

}