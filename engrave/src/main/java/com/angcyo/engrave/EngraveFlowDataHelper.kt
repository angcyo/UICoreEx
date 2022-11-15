package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.engrave.data.EngraveLayerInfo
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.uuid
import com.angcyo.objectbox.*
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.*
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.laser.pecker.lpUpdateOrCreateEntity
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

    /**获取一个传输监视记录*/
    fun getTransferMonitor(taskId: String?): TransferMonitorEntity? {
        return TransferMonitorEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }
    }

    /**开始创建传输的数据*/
    fun startCreateTransferData(taskId: String?) {
        TransferMonitorEntity::class.lpUpdateOrCreateEntity({
            apply(TransferMonitorEntity_.taskId.equal("$taskId"))
        }) {
            this.taskId = taskId
            dataMakeStartTime = nowTime()
            dataMakeFinishTime = -1
        }
    }

    /**完成创建传输的数据*/
    fun finishCreateTransferData(taskId: String?) {
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

    /**构建或者获取生成数据需要的配置信息*/
    fun generateTransferConfig(taskId: String?): TransferConfigEntity {
        return TransferConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            name = EngraveTransitionManager.generateEngraveName()
            dpi = LaserPeckerHelper.DPI_254
            mergeData = true
            dataMode = null
        }) {
            apply(TransferConfigEntity_.taskId.equal("$taskId"))
        }
    }

    /**获取一个传输配置信息实体*/
    fun getTransferConfig(taskId: String?): TransferConfigEntity? {
        return TransferConfigEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(TransferConfigEntity_.taskId.equal("$taskId"))
        }
    }

    /**获取当前任务, 所有传输了的数据, 通常这些数据都是需要雕刻的
     * [TransferDataEntity] 需要提前入库*/
    fun getTransferDataList(taskId: String?): List<TransferDataEntity> {
        return TransferDataEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(TransferDataEntity_.taskId.equal("$taskId"))
        }
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
     * 用来恢复雕刻任务*/
    fun getTransferData(index: Int): TransferDataEntity? {
        return TransferDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(TransferDataEntity_.index.equal(index))
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
        EngraveTransitionManager.engraveLayerList.forEach {
            if (typeModeList.contains(it.mode)) {
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
        EngraveTransitionManager.engraveLayerList.forEach {
            if (transferData?.layerMode == it.mode) {
                return it
            }
        }
        return null
    }

    /**获取当前雕刻的配置信息*/
    fun getCurrentEngraveConfig(taskId: String?): EngraveConfigEntity? {
        val engraveLayerInfo = getCurrentEngraveLayer(taskId)
        val engraveConfigEntity = EngraveConfigEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.layerMode.equal(engraveLayerInfo?.mode ?: 0))
            )
        }
        return engraveConfigEntity
    }

    /**构建或者获取对应雕刻图层的雕刻配置信息*/
    fun generateEngraveConfig(taskId: String?, layerMode: Int): EngraveConfigEntity {
        return EngraveConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            this.layerMode = layerMode

            val customMaterial = EngraveHelper.createCustomMaterial()
            materialCode = customMaterial.code

            power = customMaterial.power
            depth = customMaterial.depth
            time = 1

            type = LaserPeckerHelper.LASER_TYPE_BLUE
            precision = HawkEngraveKeys.lastPrecision
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

            type = itemBean.printType ?: LaserPeckerHelper.LASER_TYPE_BLUE
            precision = itemBean.printPrecision ?: HawkEngraveKeys.lastPrecision

            lpSaveEntity()
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

    /**构建或者获取一个雕刻任务实体*/
    fun generateEngraveTask(taskId: String?): EngraveTaskEntity {
        return EngraveTaskEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId

            val indexList = getTransferDataList(taskId).mapTo(mutableListOf()) { "${it.index}" }
            dataIndexList = indexList
        }) {
            apply(EngraveTaskEntity_.taskId.equal("$taskId"))
        }
    }

    /**获取一个雕刻任务*/
    fun getEngraveTask(taskId: String?): EngraveTaskEntity? {
        return EngraveTaskEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(EngraveTaskEntity_.taskId.equal("$taskId"))
        }
    }

    /**重新雕刻, 则需要清空数据雕刻完成的状态*/
    fun againEngrave(taskId: String?) {
        val taskEntity = getEngraveTask(taskId) ?: return

        //
        taskEntity.lastDurationProgress = -1
        taskEntity.lastDuration = -1
        taskEntity.lpSaveEntity()

        //
        for (indexStr in taskEntity.dataIndexList ?: emptyList()) {
            val index = indexStr.toIntOrNull() ?: 0
            val engraveData = EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
                apply(
                    EngraveDataEntity_.taskId.equal("$taskId")
                        .and(EngraveDataEntity_.index.equal(index))
                )
            }
            engraveData?.progress = 0 //清空打印进度
            engraveData?.lpSaveEntity()
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
        return EngraveDataEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            this.index = index
            progress = 0
        }) {
            apply(
                EngraveDataEntity_.taskId.equal("$taskId")
                    .and(EngraveDataEntity_.index.equal(index))
            )
        }
    }

    /**完成指定索引的雕刻*/
    fun finishEngrave(index: Int) {
        EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(EngraveDataEntity_.index.equal(index))
        }?.apply {
            this.progress = 100
            this.finishTime = nowTime()
            lpSaveEntity()
        }
    }

    /**更新雕刻索引的进度
     * [index] 当前雕刻的数据索引
     * [printTimes] 当前的打印次数,从1开始
     * [progress] 当前的进度
     * */
    fun updateEngraveProgress(index: Int, printTimes: Int, progress: Int) {
        EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(EngraveDataEntity_.index.equal(index))
        }?.apply {
            this.printTimes = printTimes
            this.progress = progress
            lpSaveEntity()
        }
    }

    /**获取任务正在雕刻的数据实体*/
    fun getEngraveDataEntity(taskId: String?): EngraveDataEntity? {
        val taskEntity = getEngraveTask(taskId) ?: return null
        return getEngraveDataEntity(taskId, taskEntity.currentIndex)
    }

    /**获取指定索引对应的雕刻信息实体*/
    fun getEngraveDataEntity(taskId: String?, index: Int): EngraveDataEntity? {
        return EngraveDataEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(
                EngraveDataEntity_.taskId.equal("$taskId")
                    .and(EngraveDataEntity_.index.equal(index))
            )
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
