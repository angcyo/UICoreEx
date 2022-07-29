package com.angcyo.engrave.model

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveItemInfo
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.engrave.data.EngravePreviewInfo
import com.angcyo.engrave.data.EngraveReadyDataInfo
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.isMain
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity_
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.saveEntity
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import kotlin.math.roundToLong

/**
 * 雕刻数据存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class EngraveModel : ViewModel(), IViewModel {

    /**当前选中的雕刻参数*/
    val engraveOptionInfoData = vmData(
        EngraveOptionInfo(
            _string(R.string.material_custom),
            EngraveHelper.lastPower.toByte(),
            EngraveHelper.lastDepth.toByte(),
            1,
            diameterPixel = EngraveHelper.lastDiameterPixel
        )
    )

    /**当前正在雕刻的数据*/
    val engraveReadyInfoData = vmDataNull<EngraveReadyDataInfo>()

    /**用来通知item的雕刻进度*/
    val engraveItemData = vmDataOnce<EngraveItemInfo>()

    /**用来通知正在预览的item*/
    val engravePreviewItemData = vmDataOnce<EngraveItemInfo>()

    /**正在预览的信息
     * [com.angcyo.engrave.EngravePreviewLayoutHelper.bindDeviceState] 清空数据
     * */
    val engravePreviewInfoData = vmDataNull<EngravePreviewInfo>()

    /**设置需要雕刻的数据*/
    @AnyThread
    fun setEngraveReadyDataInfo(info: EngraveReadyDataInfo) {
        if (isMain()) {
            engraveReadyInfoData.value = info
        } else {
            engraveReadyInfoData.postValue(info)
        }
    }

    /**更新雕刻数据信息*/
    @AnyThread
    fun updateEngraveReadyDataInfo(block: EngraveReadyDataInfo.() -> Unit) {
        engraveReadyInfoData.value?.let {
            it.block()
            setEngraveReadyDataInfo(it)
        }
    }

    /**开始雕刻*/
    fun startEngrave() {
        engraveReadyInfoData.value?.apply {
            printTimes = 0
            startEngraveTime = nowTime()

            val laserPeckerModel = vmApp<LaserPeckerModel>()

            var history: EngraveHistoryEntity? = historyEntity
            if (history == null) {
                lpBoxOf(EngraveHistoryEntity::class) {
                    history = findLast(
                        1,
                        EngraveHistoryEntity_.index.equal(engraveData?.index ?: -1)
                    ).lastOrNull() ?: EngraveHistoryEntity()
                }
            }
            history?.let { entity ->
                //入库
                engraveData?.updateToEntity(entity)
                engraveOptionInfoData.value?.updateToEntity(entity)

                entity.optionMode = optionMode
                entity.dataPath = dataPath
                entity.previewDataPath = previewDataPath
                entity.startEngraveTime = startEngraveTime
                entity.printTimes = printTimes

                entity.productVersion =
                    laserPeckerModel.productInfoData.value?.version ?: entity.productVersion

                if (laserPeckerModel.isZOpen()) {
                    //z轴模式
                    entity.zMode = QuerySettingParser.Z_MODEL
                }

                entity.saveEntity(LPBox.PACKAGE_NAME)
            }

            //hold
            historyEntity = history

            //progress
            updateEngraveProgress(0)
        }
    }

    /**停止雕刻了*/
    fun stopEngrave() {
        engraveReadyInfoData.value?.apply {
            stopEngraveTime = nowTime()
            val time = stopEngraveTime - startEngraveTime
            var dTime = -1L
            if (time > 0 && printTimes > 0) {
                dTime = time / printTimes
            }
            historyEntity?.let { entity ->
                //更新雕刻历史数据, 更新雕刻耗时
                entity.stopEngraveTime = stopEngraveTime
                entity.printTimes = printTimes
                entity.duration = dTime
                entity.saveEntity(LPBox.PACKAGE_NAME)
            }

            updateEngraveProgress(-1)
        }
    }

    /**更新打印次数*/
    fun updatePrintTimes(times: Int) {
        engraveReadyInfoData.value?.apply {
            printTimes = times
            setEngraveReadyDataInfo(this)

            historyEntity?.let { entity ->
                entity.printTimes = times
                entity.saveEntity(LPBox.PACKAGE_NAME)
            }
        }
    }

    /**计算雕刻剩余时间, 毫秒
     * [rate] 打印进度百分比[0-100]*/
    fun calcEngraveRemainingTime(rate: Int): Long {
        if (rate <= 0) {
            return -1
        } else if (rate >= 100) {
            return 0
        }

        val startEngraveTime = engraveReadyInfoData.value?.startEngraveTime ?: -1
        if (startEngraveTime <= 0) {
            return -1
        }

        val time = nowTime() - startEngraveTime
        val speed = rate * 1f / time

        val sum = 100 - rate
        if (sum <= 0) {
            return -1
        }
        return (sum / speed).roundToLong()
    }

    /**更新雕刻进度
     * [progress] 0~100*/
    @AnyThread
    fun updateEngraveProgress(progress: Int) {
        engraveReadyInfoData.value?.let {
            val uuid = if (progress < 0) null else it.rendererItemUuid
            engraveItemData.postValue(EngraveItemInfo(uuid, progress))
        }
    }

    /**更新预览的item*/
    @AnyThread
    fun updateEngravePreviewUuid(uuid: String?) {
        engravePreviewItemData.postValue(
            EngraveItemInfo(uuid, engravePreviewItemData.value?.progress ?: -1)
        )
    }

    /**恢复的状态*/
    fun isRestore() = engraveReadyInfoData.value == null

    /**更新正在预览的信息*/
    @MainThread
    fun updateEngravePreviewInfo(action: EngravePreviewInfo.() -> Unit) {
        if (engravePreviewInfoData.value == null) {
            engravePreviewInfoData.setValue(EngravePreviewInfo().apply(action))
        } else {
            engravePreviewInfoData.setValue(engravePreviewInfoData.value!!.apply(action))
        }
    }

}