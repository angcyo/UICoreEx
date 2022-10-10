package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.data.EngraveLayerInfo
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.library.ex.clamp
import com.angcyo.library.ex.nowTime
import com.angcyo.objectbox.findAll
import com.angcyo.objectbox.findFirst
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.*
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.queryOrCreateEntity

/**
 * 雕刻流程数据相关处理类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/09
 */
object EngraveFlowDataHelper {

    //---预览相关---

    /**构建一个预览配置信息*/
    fun generatePreviewConfig(taskId: String?): PreviewConfigEntity {
        return PreviewConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            diameterPixel = HawkEngraveKeys.lastDiameterPixel
        }) {
            apply(PreviewConfigEntity_.taskId.equal("$taskId"))
        }
    }

    //---传输/数据相关---

    /**获取生成数据需要的配置信息*/
    fun generateTransferConfig(taskId: String?): TransferConfigEntity {
        return TransferConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            name = EngraveTransitionManager.generateEngraveName()
            px = LaserPeckerHelper.DEFAULT_PX
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

    /**获取当前任务, 所有传输了的数据, 通常这些数据都是需要雕刻的*/
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

    /**获取图层下的所有传输数据*/
    fun getLayerTransferData(taskId: String?, layerMode: Int): List<TransferDataEntity> {
        return TransferDataEntity::class.findAll(LPBox.PACKAGE_NAME) {
            apply(
                TransferDataEntity_.taskId.equal("$taskId")
                    .and(TransferDataEntity_.layerMode.equal(layerMode))
            )
        }
    }

    //---雕刻相关---

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

    /**获取对应雕刻图层的雕刻配置信息*/
    fun generateEngraveConfig(taskId: String?, layerMode: Int): EngraveConfigEntity {
        return EngraveConfigEntity::class.queryOrCreateEntity(LPBox.PACKAGE_NAME, {
            this.taskId = taskId
            this.layerMode = layerMode

            val customMaterial = EngraveHelper.createCustomMaterial()
            materialCode = customMaterial.code

            type = LaserPeckerHelper.LASER_TYPE_BLUE
            precision = HawkEngraveKeys.lastPrecision

            power = customMaterial.power
            depth = customMaterial.depth
            time = 1
        }) {
            apply(
                EngraveConfigEntity_.taskId.equal("$taskId")
                    .and(EngraveConfigEntity_.layerMode.equal(layerMode))
            )
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

    /**获取一个雕刻任务实体*/
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
        return EngraveTaskEntity::class.findFirst(LPBox.PACKAGE_NAME) {
            apply(EngraveTaskEntity_.taskId.equal("$taskId"))
        }
    }

    /**重新雕刻, 则需要清空数据雕刻完成的状态*/
    fun againEngrave(taskId: String?) {
        val taskEntity = getEngraveTask(taskId) ?: return
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

}
