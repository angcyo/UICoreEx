package com.angcyo.laserpacker.bean

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.data.toDpiScale
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.http.base.toJson
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.filterLayerDpi
import com.angcyo.library.annotation.MM
import com.angcyo.library.getAppString
import com.angcyo.library.unit.toPixel
import com.angcyo.objectbox.laser.pecker.bean.TransferLayerConfigBean
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity

/**
 * LP工程结构, 里面包含很多子元素[com.angcyo.laserpacker.bean.LPElementBean]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
data class LPProjectBean(

    /**入库记录id
     * [com.angcyo.objectbox.laser.pecker.entity.ProjectSyncEntity.entityId]*/
    var entityId: Long = -1,

    var file_id: String? = null,

    /**工程元素占用的宽高*/
    @MM
    var width: Float = 0f,
    @MM
    var height: Float = 0f,

    /**预览的base64图片
     * (data:image/xxx;base64,xxx) 带协议头
     *
     * Canvas: trying to draw too large(141018708bytes) bitmap.
     * */
    var preview_img: String? = null,

    /**item list 的所有数据
     * [com.angcyo.laserpacker.bean.LPElementBean]
     * */
    var data: String? = null,

    /**工程名*/
    var file_name: String? = null,

    /**工程创建时间, 13位毫秒*/
    var create_time: Long = -1,

    /**工程创建时间, 13位毫秒*/
    var update_time: Long = -1,

    /**数据内容版本*/
    var version: Int = 1,

    /**固件版本号*/
    var swVersion: Int = 0,

    /**硬件版本号*/
    var hwVersion: Int = 0,

    /**扩展设备*/
    var exDevice: String? = null,

    /**产品名, 比如L4 C1等
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.productName]*/
    var productName: String? = null,

    /** [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.moduleState]*/
    var moduleState: Int = -1,

    /**每个图层对应的雕刻参数
     * [List<LPLaserOptionsBean>]*/
    var laserOptions: String? = null,

    //---

    /**工程保存的上一次雕刻参数*/
    var lastPower: Int = -1,
    var lastDepth: Int = -1,
    var lastType: Int = -1,
    var lastLayerDpi: String? = null,

    //---

    /**平台*/
    var platform: String? = null,

    /**预览图
     * [preview_img]*/
    @Transient var _previewImgBitmap: Bitmap? = null,

    /**本地对应的文件路径, 如果有*/
    var _filePath: String? = null,

    /**是否处于调试模式下, 用于debug下方便断点*/
    var _debug: Boolean? = null
) {

    val _laserOptions: List<LPLaserOptionsBean>?
        get() = laserOptions.fromJson<List<LPLaserOptionsBean>>(listType(LPLaserOptionsBean::class))

    /**获取传输图层信息数据
     * [com.angcyo.objectbox.laser.pecker.entity.TransferLayerConfigBean]*/
    fun getTransferLayerJson(): String? {
        val list = mutableListOf<TransferLayerConfigBean>()
        _laserOptions?.forEach {
            val layerId = it.layerId
            if (!layerId.isNullOrBlank()) {
                list.add(TransferLayerConfigBean(layerId, layerId.filterLayerDpi(it.dpi)))
            }
        }
        return if (list.isEmpty()) null else list.toJson()
    }

    /**更新bean里面的数据*/
    fun updateOptionsFromTransferLayer(list: List<TransferLayerConfigBean>?) {
        list?.forEach { config ->
            val options = _laserOptions
            options?.find { it.layerId == config.layerId }?.apply {
                dpi = config.dpi
            }
            if (!options.isNullOrEmpty()) {
                laserOptions = options.toJson()
            }
        }
    }

    /**获取工程对应的材质描述信息, 没有入库*/
    fun getProjectMaterialList(): List<MaterialEntity> {
        val options = _laserOptions
        if (options.isNullOrEmpty()) {
            return emptyList()
        }
        val result = MaterialHelper.createLayerMaterialList { entity ->
            options.find { it.layerId == entity.layerId }?.apply {
                HawkEngraveKeys.lastDiameterPixel = diameter.toPixel()
                entity.productName = productName

                entity.code = materialCode ?: entity.code
                entity.key = materialKey

                val str = getAppString(materialKey ?: "")
                if (str.isNullOrBlank()) {
                    //非系统配置的材质, 使用材质名
                    entity.name = materialName
                } else {
                    //系统材质
                    entity.resIdStr = materialKey
                }

                entity.dpi = dpi
                entity.dpiScale = dpi.toDpiScale()
                entity.type = lightSource
                entity.precision = precision
                entity.power = printPower
                entity.depth = printDepth
                entity.count = printCount
            }
        }

        if (options.isNotEmpty()) {
            laserOptions = options.toJson()
        }

        return result
    }

    /**更新bean里面的数据*/
    fun updateOptionsFromEngraveConfig(configEntity: EngraveConfigEntity) {
        val options = _laserOptions

        options?.find { it.layerId == configEntity.layerId }?.apply {
            printPower = configEntity.power
            printDepth = configEntity.depth
            printCount = configEntity.time
            precision = configEntity.precision
            lightSource = configEntity.type.toInt()
        }

        if (!options.isNullOrEmpty()) {
            laserOptions = options.toJson()
        }
    }

}
