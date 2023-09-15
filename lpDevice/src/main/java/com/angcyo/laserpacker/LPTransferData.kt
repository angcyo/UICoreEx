package com.angcyo.laserpacker

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.core.vmApp
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.library.ex.uuid
import com.angcyo.library.libCacheFile
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity_
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.queryOrCreateEntity

/**
 * [com.angcyo.engrave2.EngraveFlowDataHelper.generateEngraveConfig]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/26
 */
object LPTransferData {

    /**创建指定图层的雕刻参数
     *
     * [LaserPeckerHelper.LASER_TYPE_WHITE]
     * [LaserPeckerHelper.LASER_TYPE_BLUE]
     * */
    fun generateEngraveConfig(
        taskId: String?,
        layerId: String?,
        type: Byte? = null,
        power: Int? = null,
        depth: Int? = null,
        precision: Int? = null,
    ): EngraveConfigEntity {
        return EngraveConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            this.layerId = layerId

            deviceAddress = LaserPeckerHelper.lastDeviceAddress()

            //获取最后一次相同图层的雕刻参数
            val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
            val last = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveConfigEntity_.productName.equal("$productName")
                        .and(EngraveConfigEntity_.layerId.equal(layerId ?: ""))
                )
            }

            //材质
            val customMaterial =
                MaterialHelper.createCustomLayerMaterialList().find { it.layerId == layerId }!!
            materialCode = customMaterial.code

            //功率
            this.power = power ?: last?.power ?: customMaterial.power
            this.depth = depth ?: last?.depth ?: customMaterial.depth
            time = 1

            //光源, 此处配置有多个地方, 使用find查找
            this.type = type ?: last?.type ?: DeviceHelper.getProductLaserType()
            this.precision = precision ?: last?.precision ?: HawkEngraveKeys.lastPrecision
            this.pump = last?.pump ?: pump

            //物理尺寸
            diameterPixel = HawkEngraveKeys.lastDiameterPixel
        }) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.layerId.equal(layerId ?: ""))
            )
        }
    }

    /**使用GCode数据, 创建一个[TransferDataEntity]
     * [taskId] 关联的任务id
     * [index] 数据索引
     * [lines] GCode行数
     * [bytes] 真实的数据
     * [name] 临时文件名
     * */
    fun ofGCode(
        bytes: ByteArray,
        lines: Int = 0,
        taskId: String = uuid(),
        index: Int = EngraveHelper.generateEngraveIndex(),
        name: String? = "$index"
    ): TransferDataEntity {
        val file = libCacheFile(name ?: "$index")
        file.writeBytes(bytes)
        return TransferDataEntity().apply {
            this.taskId = taskId
            this.index = index
            this.lines = lines
            this.dataPath = file.path
            this.engraveDataType = DataCmd.ENGRAVE_TYPE_GCODE
            this.layerId = LaserPeckerHelper.LAYER_LINE

            x = 0
            y = 0
            width = 1
            height = 1
            dpi = LaserPeckerHelper.DPI_254
        }
    }

}