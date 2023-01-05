package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.vmApp
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.ex.connect
import com.angcyo.library.ex.ensureInt
import com.angcyo.library.ex.readAssets
import com.angcyo.library.ex.resetAll
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
import com.angcyo.library.unit.unitDecimal
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity_
import kotlin.math.max

/**
 * 雕刻助手
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
object EngraveHelper {

    fun findOptionIndex(list: List<Any>?, value: Int?): Int {
        return list?.indexOfFirst { it.toString().toInt() == value } ?: -1
    }

    /**获取物理尺寸的值*/
    fun getDiameter(): Int {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        //物理尺寸
        val diameter = if (!laserPeckerModel.haveExDevice()) {
            0
        } else {
            val mm = MM_UNIT.convertPixelToValue(HawkEngraveKeys.lastDiameterPixel)
            (mm * 100).toInt()
        }
        return diameter
    }

    /**生成百分比数值列表*/
    fun percentList(max: Int = 100): List<Int> {
        return (1..max).toList()
    }

    /**获取推荐的激光类型*/
    fun getProductLaserType(): Byte {
        return LaserPeckerHelper.findProductSupportLaserTypeList()
            .find { it.type.toInt() == HawkEngraveKeys.lastType }?.type
            ?: LaserPeckerHelper.LASER_TYPE_BLUE
    }

    //<editor-fold desc="material">

    /**缓存*/
    val materialList = mutableListOf<MaterialEntity>()

    /**相同的材质key, 合并后的集合*/
    val unionMaterialList = mutableListOf<MaterialEntity>()

    /**设备连接上之后, 初始化材质推荐参数
     * [com.angcyo.engrave.model.FscDeviceModel.initDevice]
     * */
    fun initMaterial() {
        materialList.resetAll(getProductMaterialList(vmApp<LaserPeckerModel>().productInfoData.value))
        unionMaterialList.clear()
        materialList.filterTo(unionMaterialList) { entity ->
            unionMaterialList.find { it.key == entity.key && it.type == entity.type /*名称一样, 并且光源一样*/ } == null
        }
    }

    /**创建一个自定义的材质*/
    fun createCustomMaterial(): MaterialEntity {
        //自定义, 自动记住了上一次的值
        return MaterialEntity().apply {
            resId = R.string.custom
            key = "custom"
            type = getProductLaserType().toInt()
            power = HawkEngraveKeys.lastPower
            depth = HawkEngraveKeys.lastDepth
            precision = HawkEngraveKeys.lastPrecision
            layerModeStr = EngraveTransitionManager.engraveLayerList.connect { "${it.layerMode}" }
            createMaterialCode(key!!)
        }
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

        //系统的推荐参数
        product.laserTypeList.forEach {
            val json = "argument/${name}_${it.wave}_${it.power.ensureInt()}.json"
            L.w("读取材质:${json}")
            val list = app().readAssets(json)
                ?.fromJson<List<MaterialEntity>>(listType(MaterialEntity::class))
            list?.let { result.addAll(list) }
        }

        if (result.isEmpty()) {
            //如果为空, 则创建一个自定义的材质参数
            result.add(createCustomMaterial())
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

    //</editor-fold desc="material">
}

/**创建一个自定义的材质Code*/
fun MaterialEntity.createMaterialCode(materialName: String) {
    code = buildString {
        append("${materialName}_")
        append(if (type == LaserPeckerHelper.LASER_TYPE_BLUE.toInt()) "blue_" else "white_")
        append(if (layerMode == CanvasConstant.DATA_MODE_DITHERING) "bw_" else "gray_")
        append(dpiScale.unitDecimal(1))
    }

}