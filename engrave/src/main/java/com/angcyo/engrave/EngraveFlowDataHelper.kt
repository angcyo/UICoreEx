package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.toDpiScale
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.vmApp
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.MaterialHelper.createMaterialCode
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.library.ex._string
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.uuid
import com.angcyo.library.getAppString
import com.angcyo.objectbox.*
import com.angcyo.objectbox.laser.pecker.*
import com.angcyo.objectbox.laser.pecker.entity.*
import kotlin.math.max

/**
 * 雕刻流程数据相关处理类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */
object EngraveFlowDataHelper {

    //region ---task---

    /**
     * 如果之前的任务id[oldTaskId],已经有数据传输了, 则重新生成一个新的任务id, 否则不变
     * [PreviewConfigEntity] 和 [TransferConfigEntity] 需要使用上一次保存的信息
     * */
    fun generateTaskId(oldTaskId: String?): String {
        val oldCount = TransferDataEntity::class.countBy(LPBox.PACKAGE_NAME) {
            apply(TransferDataEntity_.taskId.equal("$oldTaskId"))
        }

        if (oldTaskId != null && oldCount <= 0) {
            return oldTaskId
        }

        val taskId = uuid()
        PreviewConfigEntity::class.findLast(LPBox.PACKAGE_NAME)?.apply {
            entityId = 0//重新存储
            this.taskId = taskId
            lpSaveEntity()
        } ?: generatePreviewConfig(taskId)
        TransferConfigEntity::class.findLast(LPBox.PACKAGE_NAME)?.apply {
            entityId = 0//重新存储
            this.taskId = taskId
            lpSaveEntity()
        } ?: generateTransferConfig(taskId)
        return taskId
    }

    /**通过某个雕刻索引[index], 创建一个新的任务id.
     * 通常在雕刻历史文档时使用
     * [oldTaskId] 可以指定任务id, 也可以不指定. 不指定则从最后一个获取
     * @return 返回可以直接雕刻的任务id*/
    fun generateTask(index: Int, oldTaskId: String? = null): String? {
        //此任务, 只雕刻一个数据
        val transferDataEntity = getTransferData(index, oldTaskId) ?: return null
        val newTaskId = uuid()

        //复制一份雕刻数据
        transferDataEntity.apply {
            entityId = 0
            this.taskId = newTaskId
            lpSaveEntity()

            //复制一份雕刻参数
            getEngraveConfig(oldTaskId, layerMode)?.let { entity ->
                entity.entityId = 0
                entity.taskId = newTaskId
                entity.lpSaveEntity()
            }
        }

        return newTaskId
    }

    /**构建一个简单的任务, 不需要传输数据, 直接雕刻指定索引的任务*/
    fun generateSingleTask(index: Int, taskId: String?): String? {
        val engraveDataEntity = getEngraveDataEntity(taskId, index) ?: EngraveDataEntity()
        engraveDataEntity.apply {
            //需要直接雕刻的索引
            this.taskId = taskId
            this.index = index
            this.isFromDeviceHistory = true
            this.deviceAddress = LaserPeckerHelper.lastDeviceAddress()
            engraveDataEntity.clearEngraveData()
            lpSaveEntity()
        }
        return taskId
    }

    /**获取一个传输监视记录*/
    fun getTransferMonitor(taskId: String?): TransferMonitorEntity? {
        return TransferMonitorEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }
    }

    /**开始创建传输的数据*/
    fun onStartCreateTransferData(taskId: String?) {
        TransferMonitorEntity::class.lpUpdateOrCreateEntity({
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }) {
            this.taskId = taskId
            dataTransferSize = 0 //清空传输大小
            dataMakeStartTime = nowTime()
            dataMakeFinishTime = -1
            dataTransferStartTime = -1
            dataTransferMaxSpeed = -1f
            dataTransferProgress = 0
        }
    }

    /**完成创建传输的数据*/
    fun onFinishCreateTransferData(taskId: String?) {
        TransferMonitorEntity::class.lpUpdateOrCreateEntity({
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }) {
            dataMakeFinishTime = nowTime()
        }
    }

    /**开始传输数据, 用于记录时间和总数据大小*/
    fun startTransferData(taskId: String?, list: List<TransferDataEntity>) {
        TransferMonitorEntity::class.lpUpdateOrCreateEntity({
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }) {
            dataTransferStartTime = nowTime()
            dataTransferFinishTime = -1

            //数据大小
            dataTransferSize = list.sumOf { it.bytesSize() }
        }
    }

    /**更新数据传输进度和速率*/
    fun updateTransferDataProgress(taskId: String?, progress: Int, speed: Float) {
        TransferMonitorEntity::class.lpUpdateOrCreateEntity({
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }) {
            dataTransferProgress = progress
            dataTransferSpeed = speed
            dataTransferMaxSpeed = max(speed, dataTransferMaxSpeed)
        }
    }

    /**完成传输数据*/
    fun finishTransferData(taskId: String?) {
        TransferMonitorEntity::class.lpUpdateOrCreateEntity({
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }) {
            dataTransferFinishTime = nowTime()
        }
    }

    //endregion ---task---

    //region ---预览相关---

    /**构建或者获取一个预览配置信息*/
    fun generatePreviewConfig(taskId: String?): PreviewConfigEntity {
        return PreviewConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            pwrProgress = HawkEngraveKeys.lastPwrProgress
            diameterPixel = HawkEngraveKeys.lastDiameterPixel
        }) {
            apply(PreviewConfigEntity_.taskId.equal("$taskId"))
        }
    }

    //endregion ---预览相关---

    //region ---传输/数据相关---

    /**构建或者获取生成数据需要的配置信息
     * [taskId] 可以为空*/
    fun generateTransferConfig(
        taskId: String?,
        canvasDelegate: CanvasDelegate? = null
    ): TransferConfigEntity {
        return TransferConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME) {
            if (!taskId.isNullOrBlank()) {
                apply(TransferConfigEntity_.taskId.equal("$taskId"))
            }
        }.apply {
            //参数设置
            this.taskId = taskId
            val supportPxList = LaserPeckerHelper.findProductSupportPxList()
            val find = supportPxList.find { it.dpi == HawkEngraveKeys.lastDpi }
            //优先使用存在的最后一次使用的dpi, 否则默认使用第一个dpi
            dpi = find?.dpi ?: (supportPxList.firstOrNull()?.dpi ?: LaserPeckerHelper.DPI_254)
            mergeData = false
            dataMode = null

            //数据dpi恢复
            if (canvasDelegate != null) {
                val list =
                    EngraveTransitionManager.getRendererList(canvasDelegate, null)

                val isAllGCode = EngraveTransitionManager.isAllSameLayerMode(
                    list,
                    CanvasConstant.DATA_MODE_GCODE
                )

                if (isAllGCode) {
                    //全部是GCode则只能是1k
                    dpi = LaserPeckerHelper.DPI_254
                } else {
                    list.find {
                        it is DataItemRenderer && (it.getRendererRenderItem()?.dataBean?.dpi
                            ?: 0f) > 0f
                    }?.let { item ->
                        if (item is DataItemRenderer) {
                            dpi = item.getRendererRenderItem()?.dataBean?.dpi ?: dpi
                        }
                    }
                }
            }
            if (name.isEmpty()) {
                name = EngraveHelper.generateEngraveName()
            }
        }
    }

    /**获取一个传输配置信息实体
     * [TransferConfigEntity] 包含传输的文件名, dpi等信息
     * */
    fun getTransferConfig(taskId: String?): TransferConfigEntity? {
        return TransferConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(TransferConfigEntity_.taskId.equal("$taskId"))
        }
    }

    /**获取最后一次的传输配置*/
    fun getLastTransferConfig(): TransferConfigEntity? {
        return TransferConfigEntity::class.findLast(LPBox.PACKAGE_NAME)
    }

    /**获取当前任务, 所有传输了的数据, 通常这些数据都是需要雕刻的
     * [TransferDataEntity] 需要提前入库*/
    fun getTransferDataList(taskId: String?): List<TransferDataEntity> {
        return TransferDataEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(TransferDataEntity_.taskId.equal("$taskId"))
        }
    }

    /**清空数据已经传输完成的状态*/
    fun clearTransferDataState(taskId: String?) {
        val list = getTransferDataList(taskId)
        list.forEach {
            it.isTransfer = false
        }
        list.lpSaveAllEntity()
    }

    /**删除数据已经传输完成的状态*/
    fun removeTransferDataState(taskId: String?) {
        val list = getTransferDataList(taskId)
        list.lpRemoveAllEntity()
    }

    /**获取索引[index]对应的数据, 用来雕刻*/
    fun getTransferData(taskId: String?, index: Int): TransferDataEntity? {
        return TransferDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                TransferDataEntity_.taskId.equal("$taskId")
                    .and(TransferDataEntity_.index.equal(index))
            )
        }
    }

    /**通过指定的文件索引[index], 查询对应的传输数据, 反向获取对应的任务id,
     * 用来恢复雕刻任务
     * [taskId] 可以指定任务id, 也可以不指定. 不指定则从最后一个获取
     * [deviceAddress] 指定某一台设备, 同一个索引, 有可能发送给多个机器
     * */
    fun getTransferData(
        index: Int,
        taskId: String? = null,
        deviceAddress: String? = null
    ): TransferDataEntity? {
        return TransferDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            equal(TransferDataEntity_.index, index.toLong())
            if (taskId != null) {
                equalString(TransferDataEntity_.taskId, taskId)
            }
            if (deviceAddress != null) {
                equalString(TransferDataEntity_.deviceAddress, deviceAddress)
            }
        }
    }

    /**根据任务id[taskId], 获取一个未传输完成的数据, 如果有*/
    fun getNeedTransferData(taskId: String?): TransferDataEntity? {
        return TransferDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                TransferDataEntity_.taskId.equal("$taskId")
                    .and(TransferDataEntity_.isTransfer.equal(false))
            )
        }
    }

    /**获取图层下的所有传输数据*/
    fun getLayerTransferData(taskId: String?, layerMode: Int): List<TransferDataEntity> {
        return TransferDataEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(
                TransferDataEntity_.taskId.equal("$taskId")
                    .and(TransferDataEntity_.layerMode.equal(layerMode))
            )
        }
    }

    //endregion ---传输/数据相关---

    //region ---雕刻相关---

    /**获取当前任务有多少个图层需要雕刻*/
    fun getEngraveLayerList(taskId: String?): List<EngraveLayerInfo> {
        val dataList = getTransferDataList(taskId)
        val typeModeList = mutableListOf<Int>()
        dataList.forEach {
            if (!typeModeList.contains(it.layerMode)) {
                typeModeList.add(it.layerMode)
            }
        }
        val laserList = mutableListOf<EngraveLayerInfo>()
        EngraveHelper.engraveLayerList.forEach {
            if (typeModeList.contains(it.layerMode)) {
                laserList.add(it)
            }
        }
        return laserList
    }

    /**获取当前雕刻的图层*/
    fun getCurrentEngraveLayer(taskId: String?): EngraveLayerInfo? {
        val taskEntity = getEngraveTask(taskId) ?: return null
        val transferData = TransferDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                TransferDataEntity_.taskId.equal("$taskId")
                    .and(TransferDataEntity_.index.equal(taskEntity.currentIndex))
            )
        }
        EngraveHelper.engraveLayerList.forEach {
            if (transferData?.layerMode == it.layerMode) {
                return it
            }
        }
        return null
    }

    /**获取当前雕刻的配置信息*/
    fun getCurrentEngraveConfig(taskId: String?): EngraveConfigEntity? {
        if (HawkEngraveKeys.enableItemEngraveParams) {
            val taskEntity = getEngraveTask(taskId) ?: return null
            val engraveConfigEntity = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
                apply(EngraveConfigEntity_.taskId.equal("${taskEntity.currentIndex}"))
            }
            return engraveConfigEntity
        } else {
            val engraveLayerInfo = getCurrentEngraveLayer(taskId)
            val engraveConfigEntity = EngraveConfigEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveConfigEntity_.taskId.equal("$taskId")
                        .and(EngraveConfigEntity_.layerMode.equal(engraveLayerInfo?.layerMode ?: 0))
                )
            }
            return engraveConfigEntity
        }
    }

    /**获取任务雕刻的材质名称*/
    fun getCurrentEngraveMaterName(taskId: String?): String {
        val engraveConfigEntity = getCurrentEngraveConfig(taskId)
        return getEngraveMaterNameByKey(engraveConfigEntity?.materialKey)
    }

    fun getEngraveMaterNameByKey(materialKey: String?): String {
        return MaterialHelper.getMaterialByKey(materialKey)?.name
            ?: getAppString(materialKey ?: "custom")
            ?: _string(R.string.custom)
    }

    /**获取最后一个图层的雕刻配置信息*/
    fun getLastEngraveConfig(taskId: String?): EngraveConfigEntity? {
        val engraveConfigEntity = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(EngraveConfigEntity_.taskId.equal("$taskId"))
        }
        return engraveConfigEntity
    }

    /**雕刻任务, 所有图层的雕刻参数初始化*/
    fun generateEngraveConfig(taskId: String?): List<EngraveConfigEntity> {
        val result = mutableListOf<EngraveConfigEntity>()
        getTransferDataList(taskId).let {
            for (data in it) {
                result.add(generateEngraveConfig(taskId, data.layerMode))
            }
        }
        return result
    }

    /**查找任务配置的材质信息*/
    fun findTaskMaterial(taskId: String?): MaterialEntity? {
        val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
        //优先使用任务配置的雕刻信息
        val config = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.productName.equal("$productName"))
            )
        }
        config ?: return null
        return MaterialHelper.materialList.find { it.code == config.materialCode } //这是内存数据, 切换不同设备之后可以就不一样了
            ?: MaterialHelper.getMaterialByCode(taskId, config.materialCode) //这是数据库数据, 需要提前保存
    }

    /**则查找相同产品的最近一次的雕刻信息*/
    fun findLastMaterial(): MaterialEntity? {
        val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
        val config = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                EngraveConfigEntity_.materialCode.notNull()
                    .and(EngraveConfigEntity_.productName.equal("$productName"))
            )
        }
        return MaterialHelper.materialList.find { it.code == config?.materialCode }
    }

    /**使用材质key, 创建对应的雕刻参数配置
     * [defMaterial] 默认的参数配置
     * [com.angcyo.objectbox.laser.pecker.entity.MaterialEntity.key]*/
    fun generateEngraveConfigByMaterial(
        taskId: String?,
        materialKey: String?,
        defMaterial: MaterialEntity?
    ): List<EngraveConfigEntity> {
        val result = mutableListOf<EngraveConfigEntity>()
        val dpiScale = getTransferConfig(taskId)?.dpi?.toDpiScale() ?: 1f
        val materialList = MaterialHelper.getMaterialList(
            materialKey ?: "custom",
            dpiScale,
            defMaterial?.type
        ) //各个图层的雕刻参数
        EngraveConfigEntity::class.removeAll(LPBox.PACKAGE_NAME) {
            apply(EngraveConfigEntity_.taskId.equal("$taskId"))
        }//先移除旧的

        val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
        //每个图层都创建一个材质参数
        EngraveHelper.engraveLayerList.forEach { engraveLayerInfo ->
            //图层模式
            EngraveConfigEntity().apply {
                this.taskId = taskId
                layerMode = engraveLayerInfo.layerMode

                //材质
                val findMaterial = materialList.find {
                    it.layerModeStr?.contains("$layerMode") == true ||
                            it.layerMode == layerMode
                } ?: defMaterial
                findMaterial?.let {
                    materialCode = it.code
                    this.productName = productName
                    this.materialKey = it.key
                    type = it.type.toByte()

                    precision = it.precision
                    power = it.power
                    depth = it.depth

                    //物理尺寸
                    val previewConfigEntity = generatePreviewConfig(taskId)
                    diameterPixel = previewConfigEntity.diameterPixel
                }

                //设备地址
                deviceAddress = LaserPeckerHelper.lastDeviceAddress()

                lpSaveEntity()
                result.add(this)
            }
        }
        return result
    }

    /**保存雕刻配置信息到材质数据库*/
    fun saveEngraveConfigToMaterial(taskId: String?, materialName: String): List<MaterialEntity> {
        val configList = EngraveConfigEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(EngraveConfigEntity_.taskId.equal("$taskId"))
        }
        val result = mutableListOf<MaterialEntity>()
        val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
        val key = "${materialName}_${nowTime()}"

        configList.forEach {
            MaterialEntity().apply {
                this.productName = productName
                this.key = key
                name = materialName
                layerMode = it.layerMode
                dpiScale = 0f

                type = it.type.toInt()
                precision = it.precision
                power = it.power
                depth = it.depth

                //code
                createMaterialCode("${key}_${nowTime()}")

                //save
                lpSaveEntity()

                result.add(this)
            }
        }

        //重新初始化材质列表
        MaterialHelper.initMaterial()
        //重新构建参数配置信息
        generateEngraveConfigByMaterial(taskId, key, result.lastOrNull())

        return result
    }

    /**删除所有材质[materialKey]*/
    fun deleteMaterial(taskId: String?, materialKey: String) {
        val all = MaterialEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(MaterialEntity_.key.equal(materialKey))
        }
        all.forEach { it.isDelete = true }
        all.lpSaveAllEntity()
        //重新初始化材质列表
        MaterialHelper.initMaterial()
        val materialEntity = findTaskMaterial(taskId)
        if (materialEntity?.key == materialKey) {
            EngraveConfigEntity::class.removeAll(LPBox.PACKAGE_NAME) {
                apply(EngraveConfigEntity_.taskId.equal("$taskId"))
            }//移除被删除的材质配置信息
        }
    }

    /**构建或者获取对应雕刻图层的雕刻配置信息*/
    fun generateEngraveConfig(taskId: String?, layerMode: Int): EngraveConfigEntity {
        return EngraveConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            this.layerMode = layerMode

            deviceAddress = LaserPeckerHelper.lastDeviceAddress()

            //获取最后一次相同图层的雕刻参数
            val productName = vmApp<LaserPeckerModel>().productInfoData.value?.name
            val last = EngraveConfigEntity::class.findLast(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveConfigEntity_.productName.equal("$productName")
                        .and(EngraveConfigEntity_.layerMode.equal(layerMode))
                )
            }

            //材质
            val customMaterial = MaterialHelper.createCustomMaterial()
            materialCode = customMaterial.code

            //功率
            power = last?.power ?: customMaterial.power
            depth = last?.depth ?: customMaterial.depth
            time = 1

            //光源
            type = last?.type ?: DeviceHelper.getProductLaserType()
            precision = last?.precision ?: HawkEngraveKeys.lastPrecision

            //物理尺寸
            val previewConfigEntity = generatePreviewConfig(taskId)
            diameterPixel = previewConfigEntity.diameterPixel
        }) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.layerMode.equal(layerMode))
            )
        }
    }

    /**构建一个雕刻参数信息从[CanvasProjectItemBean]*/
    fun generateEngraveConfig(
        taskId: String?,
        itemBean: CanvasProjectItemBean
    ): EngraveConfigEntity {
        val layerMode = itemBean._dataMode ?: -1
        return generateEngraveConfig(taskId, layerMode).apply {
            power = itemBean.printPower ?: HawkEngraveKeys.lastPower
            depth = itemBean.printDepth ?: HawkEngraveKeys.lastDepth
            time = itemBean.printCount ?: 1

            type = itemBean.printType?.toByte() ?: DeviceHelper.getProductLaserType()
            precision = itemBean.printPrecision ?: HawkEngraveKeys.lastPrecision

            deviceAddress = LaserPeckerHelper.lastDeviceAddress()

            lpSaveEntity()
        }
    }

    /**创建单文件雕刻参数*/
    fun generateEngraveConfig(canvasDelegate: CanvasDelegate?) {
        canvasDelegate?.let {
            val rendererList = EngraveTransitionManager.getRendererList(canvasDelegate, null)
            rendererList.forEach { renderer ->
                if (renderer is DataItemRenderer) {
                    renderer.dataItem?.dataBean?.let { bean ->
                        //为每个元素创建对应的雕刻参数
                        generateEngraveConfig("${bean.index}", bean)
                    }
                }
            }
        }
    }

    /**根据数据索引, 获取对应点额雕刻配置信息*/
    fun getEngraveConfig(index: Int?): EngraveConfigEntity? {
        return EngraveConfigEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(EngraveConfigEntity_.taskId.equal("$index"))
        }
    }

    /**获取图层的雕刻配置*/
    fun getEngraveConfig(taskId: String?, layerMode: Int): EngraveConfigEntity? {
        return EngraveConfigEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.layerMode.equal(layerMode))
            )
        }
    }

    /**获取图层的雕刻配置*/
    fun getTaskEngraveConfigList(taskId: String?): List<EngraveConfigEntity> {
        return EngraveConfigEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(EngraveConfigEntity_.taskId.equal("$taskId"))
        }
    }

    /**构建或者获取一个雕刻任务实体*/
    fun generateEngraveTask(taskId: String?): EngraveTaskEntity {
        return EngraveTaskEntity::class.ensureEntity(LPBox.PACKAGE_NAME, {
            apply(EngraveTaskEntity_.taskId.equal("$taskId"))
        }) {
            this.taskId = taskId

            val indexList = getTransferDataList(taskId).mapTo(mutableListOf()) { "${it.index}" }
            if (indexList.isEmpty()) {
                //没有传输的数据, 则有可能是需要直接雕刻索引的任务
                getEngraveDataEntity(taskId)?.let {
                    indexList.add("${it.index}")
                }
            }
            dataIndexList = indexList

            //任务所属的设备地址
            deviceAddress = LaserPeckerHelper.lastDeviceAddress()
        }
    }

    /**获取一个雕刻任务*/
    fun getEngraveTask(taskId: String?): EngraveTaskEntity? {
        return EngraveTaskEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(EngraveTaskEntity_.taskId.equal("$taskId"))
        }
    }

    /**重新雕刻, 则需要清空数据雕刻完成的状态, 任务id不变*/
    fun againEngrave(fromTaskId: String?) {
        val taskEntity = getEngraveTask(fromTaskId) ?: return

        //
        taskEntity.lastDurationProgress = -1
        taskEntity.lastDuration = -1
        taskEntity.lpSaveEntity()

        //
        for (indexStr in taskEntity.dataIndexList ?: emptyList()) {
            val index = indexStr.toIntOrNull() ?: 0
            val engraveData = EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveDataEntity_.taskId.equal("$fromTaskId")
                        .and(EngraveDataEntity_.index.equal(index))
                )
            }
            engraveData?.apply {
                clearEngraveData()
                lpSaveEntity()
            }
        }
    }

    /**获取任务中, 下一个需要雕刻的数据索引, 如果有*/
    fun getNextEngraveIndex(taskId: String?): Int? {
        val taskEntity = getEngraveTask(taskId) ?: return null

        for (indexStr in taskEntity.dataIndexList ?: emptyList()) {
            val index = indexStr.toIntOrNull() ?: 0
            val engraveData = EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveDataEntity_.taskId.equal("$taskId")
                        .and(EngraveDataEntity_.index.equal(index))
                )
            }
            if (engraveData == null || engraveData.progress != 100) {
                //如果当前索引对应的数据没有雕刻, 或者雕刻进度未完成
                //则表示当前的索引需要雕刻
                return index
            }
        }
        return null
    }

    /**根据索引[index], 获取一个雕刻数据状态信息实体*/
    fun generateEngraveData(taskId: String?, index: Int): EngraveDataEntity {
        return EngraveDataEntity::class.ensureEntity(LPBox.PACKAGE_NAME, {
            apply(
                EngraveDataEntity_.taskId.equal("$taskId")
                    .and(EngraveDataEntity_.index.equal(index))
            )
        }) {
            this.taskId = taskId
            this.index = index
            deviceAddress = LaserPeckerHelper.lastDeviceAddress()
            clearEngraveData()
        }
    }

    /**完成指定索引的雕刻*/
    fun finishIndexEngrave(taskId: String?, index: Int) {
        EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                EngraveDataEntity_.taskId.equal("$taskId")
                    .and(EngraveDataEntity_.index.equal(index))
            )
        }?.apply {
            this.progress = 100
            this.finishTime = nowTime()
            lpSaveEntity()
        }
    }

    /**更新雕刻索引的进度,
     * 注意:雕刻索引之前的所有其他索引应该已经全部雕刻完成
     * [index] 当前雕刻的数据索引
     * [printTimes] 当前的打印次数,从1开始, null 不指定
     * [progress] 当前的进度
     * */
    fun updateEngraveProgress(taskId: String?, index: Int, printTimes: Int?, progress: Int) {
        getEngraveTask(taskId)?.let {
            it.dataIndexList?.let { list ->
                for (beforeIndex in list) {
                    if (beforeIndex == "$index") {
                        break
                    }
                    getEngraveDataEntity(taskId, index)?.apply {
                        //自动完成之前的所有索引, 比如雕刻到第8个文件再连接上蓝牙, 此时的雕刻进度~
                        if (progress != 100) {
                            this.progress = 100
                            lpSaveEntity()
                        }
                    }
                }
            }
        }
        //
        getEngraveDataEntity(taskId, index)?.apply {
            this.printTimes = printTimes ?: this.printTimes
            this.progress = progress
            lpSaveEntity()
        }
    }

    /**获取任务正在雕刻的数据实体*/
    fun getCurrentEngraveDataEntity(taskId: String?): EngraveDataEntity? {
        val taskEntity = getEngraveTask(taskId) ?: return null
        return getEngraveDataEntity(taskId, taskEntity.currentIndex)
    }

    /**获取任务雕刻的数据实体*/
    fun getEngraveDataEntity(taskId: String?): EngraveDataEntity? {
        return EngraveDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(EngraveDataEntity_.taskId.equal("$taskId"))
        }
    }

    /**获取指定索引对应的雕刻信息实体*/
    fun getEngraveDataEntity(taskId: String?, index: Int): EngraveDataEntity? {
        return EngraveDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            if (taskId == null) {
                apply(EngraveDataEntity_.index.equal(index))
            } else {
                apply(
                    EngraveDataEntity_.taskId.equal("$taskId")
                        .and(EngraveDataEntity_.index.equal(index))
                )
            }
        }
    }

    /**通过索引列表, 获取所有雕刻的数据*/
    fun getEngraveData(index: List<Int>): List<EngraveDataEntity> {
        return EngraveDataEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(EngraveDataEntity_.index.oneOf(index.toIntArray()))
            orderDesc(EngraveDataEntity_.startTime)
        }
    }

    /**通过索引列表, 获取所有雕刻的数据
     * [deviceAddress] 指定设备的地址, 也可以不指定*/
    fun getEngraveData(
        index: Int,
        fromDeviceHistory: Boolean,
        deviceAddress: String?
    ): EngraveDataEntity? {
        return EngraveDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            equal(EngraveDataEntity_.index, index.toLong())
            equal(EngraveDataEntity_.isFromDeviceHistory, fromDeviceHistory)
            if (deviceAddress != null) {
                equalString(EngraveDataEntity_.deviceAddress, deviceAddress)
            }
        }
    }

    /**计算雕刻任务的总进度[0~100]*/
    fun calcEngraveProgress(taskId: String?): Int {
        val taskEntity = getEngraveTask(taskId) ?: return 0

        //总进度
        var sum = 0
        //当前进度
        var current = 0

        for (indexStr in taskEntity.dataIndexList ?: emptyList()) {
            val index = indexStr.toIntOrNull() ?: 0

            //传输的数据, 用来计算总进度
            val transferData = TransferDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(
                    TransferDataEntity_.taskId.equal("$taskId")
                        .and(TransferDataEntity_.index.equal(index))
                )
            }
            transferData?.let {
                getEngraveConfig(taskId, it.layerMode)?.let { engraveConfigEntity ->
                    sum += engraveConfigEntity.time * 100
                }
            }

            //雕刻数据, 用来计算已经行走的进度
            val engraveData = EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveDataEntity_.taskId.equal("$taskId")
                        .and(EngraveDataEntity_.index.equal(index))
                )
            }
            engraveData?.let {
                current += (it.printTimes - 1) * 100 + it.progress
            }
        }
        val progress = clamp((current * 1f / sum * 100).toInt(), 0, 100)
        return progress
    }

    /**计算雕刻剩余时长
     * @return 时长毫秒, -1表示无法统计*/
    fun calcEngraveProgressDuration(taskId: String?): Long {
        val progress = calcEngraveProgress(taskId)
        val taskEntity = getEngraveTask(taskId) ?: return -1
        if (progress > 0) {
            val nowTime = nowTime()
            if (taskEntity.lastDuration == -1L || taskEntity.lastDurationProgress != progress) {
                //需要计算剩余时长
                val startTime = taskEntity.startTime
                if (startTime > 0) {
                    val totalTime = nowTime - startTime //总共过去的时间
                    val duration: Long =
                        (totalTime * 1f / progress * max(0, 100 - progress)).toLong()
                    taskEntity.lastDurationProgress = progress
                    taskEntity.lastDuration = duration
                    taskEntity.lastDurationTime = nowTime
                    taskEntity.lpSaveEntity()
                    return duration
                } else {
                    return -1
                }
            } else {
                //直接返回上一次的计算出来的时长
                return taskEntity.lastDuration - (nowTime - taskEntity.lastDurationTime)
            }
        } else {
            return -1
        }
    }

    //endregion ---雕刻相关---

}
