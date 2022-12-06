package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.component.file.appFilePath
import com.angcyo.engrave.data.EngraveLayerInfo
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.engrave.transition.IEngraveTransition
import com.angcyo.library.ex.*
import com.angcyo.library.libCacheFile
import com.angcyo.library.utils.logPath
import com.angcyo.objectbox.*
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.*
import com.angcyo.objectbox.laser.pecker.lpSaveAllEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.laser.pecker.lpUpdateOrCreateEntity
import kotlin.math.max

/**
 * 雕刻流程数据相关处理类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */
object EngraveFlowDataHelper {

    /**分享最近的雕刻日志*/
    fun shareEngraveLog() {
        val logList = mutableListOf(logPath())
        logList.addAll(getTaskEngraveLogFilePath())
        logList.zip(libCacheFile("engrave-log-${nowTimeString("yyyy-MM-dd")}.zip").absolutePath)
            ?.shareFile()
    }

    /**最后一次雕刻任务的数据日志文件路径*/
    fun getTaskEngraveLogFilePath(): List<String> {
        val result = mutableListOf<String>()
        val task = EngraveTaskEntity::class.findLast(LPBox.PACKAGE_NAME) {
            //no op
        }
        task?.dataIndexList?.forEach {
            //.png
            var path = appFilePath(
                "${it}${IEngraveTransition.EXT_PREVIEW}",
                CanvasConstant.ENGRAVE_FILE_FOLDER
            )
            if (path.isFileExist()) {
                result.add(path)
            }
            //.p.png
            path = appFilePath(
                "${it}${IEngraveTransition.EXT_DATA_PREVIEW}",
                CanvasConstant.ENGRAVE_FILE_FOLDER
            )
            if (path.isFileExist()) {
                result.add(path)
            }
            //.bp
            path = appFilePath(
                "${it}${IEngraveTransition.EXT_BP}",
                CanvasConstant.ENGRAVE_FILE_FOLDER
            )
            if (path.isFileExist()) {
                result.add(path)
            }
            //.dt
            path = appFilePath(
                "${it}${IEngraveTransition.EXT_DT}",
                CanvasConstant.ENGRAVE_FILE_FOLDER
            )
            if (path.isFileExist()) {
                result.add(path)
            }
            //.gcode
            path = appFilePath(
                "${it}${IEngraveTransition.EXT_GCODE}",
                CanvasConstant.ENGRAVE_FILE_FOLDER
            )
            if (path.isFileExist()) {
                result.add(path)
            }
        }
        return result
    }

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
        val engraveDataEntity = getEngraveDataEntity(taskId, index)
        if (engraveDataEntity == null) {
            EngraveDataEntity().apply {
                //需要直接雕刻的索引
                this.taskId = taskId
                this.index = index
                this.isFromDeviceHistory = true
                lpSaveEntity()
            }
        } else {
            engraveDataEntity.progress = 0
            engraveDataEntity.printTimes = 1
            engraveDataEntity.startTime = -1
            engraveDataEntity.finishTime = -1
            engraveDataEntity.lpSaveEntity()
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
    fun generateTransferConfig(
        taskId: String?,
        canvasDelegate: CanvasDelegate? = null
    ): TransferConfigEntity {
        return TransferConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            name = EngraveTransitionManager.generateEngraveName()
            dpi = LaserPeckerHelper.DPI_254
            mergeData = false
            dataMode = null

            //数据dpi恢复
            if (canvasDelegate != null) {
                val list =
                    EngraveTransitionManager.getRendererList(canvasDelegate, null)

                list.find {
                    it is DataItemRenderer && (it.getRendererRenderItem()?.dataBean?.dpi ?: 0f) > 0f
                }?.let { item ->
                    if (item is DataItemRenderer) {
                        dpi = item.getRendererRenderItem()?.dataBean?.dpi ?: dpi
                    }
                }
            }
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

    /**清空数据已经传输完成的状态*/
    fun clearTransferDataState(taskId: String?) {
        val list = getTransferDataList(taskId)
        list.forEach {
            it.isTransfer = false
        }
        list.lpSaveAllEntity()
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
     * */
    fun getTransferData(index: Int, taskId: String? = null): TransferDataEntity? {
        return TransferDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            if (taskId == null) {
                apply(TransferDataEntity_.index.equal(index))
            } else {
                apply(
                    TransferDataEntity_.taskId.equal("$taskId")
                        .and(TransferDataEntity_.index.equal(index))
                )
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
        EngraveTransitionManager.engraveLayerList.forEach {
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
        EngraveTransitionManager.engraveLayerList.forEach {
            if (transferData?.layerMode == it.layerMode) {
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
                    .and(EngraveConfigEntity_.layerMode.equal(engraveLayerInfo?.layerMode ?: 0))
            )
        }
        return engraveConfigEntity
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
            if (indexList.isEmpty()) {
                //没有传输的数据, 则有可能是需要直接雕刻索引的任务
                getEngraveDataEntity(taskId)?.let {
                    indexList.add("${it.index}")
                }
            }
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
            engraveData?.apply {
                printTimes = 1 //重置打印次数
                progress = 0 //清空打印进度
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

            progress = 0
            printTimes = 1
            startTime = -1
            finishTime = -1
        }
    }

    /**完成指定索引的雕刻*/
    fun finishEngrave(taskId: String?, index: Int) {
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
     * [printTimes] 当前的打印次数,从1开始
     * [progress] 当前的进度
     * */
    fun updateEngraveProgress(taskId: String?, index: Int, printTimes: Int, progress: Int) {
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
            this.printTimes = printTimes
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

    /**通过索引列表, 获取所有雕刻的数据*/
    fun getEngraveData(index: Int, fromDeviceHistory: Boolean): EngraveDataEntity? {
        return EngraveDataEntity::class.findLast(LPBox.PACKAGE_NAME) {
            apply(
                EngraveDataEntity_.index.equal(index)
                    .and(
                        EngraveDataEntity_.isFromDeviceHistory.isNull.or(
                            EngraveDataEntity_.isFromDeviceHistory.equal(
                                fromDeviceHistory
                            )
                        )
                    )
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
