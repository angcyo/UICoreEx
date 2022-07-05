package com.angcyo.engrave.model

import androidx.annotation.AnyThread
import androidx.lifecycle.ViewModel
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.objectbox.findLast
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity_
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.saveEntity
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull
import kotlin.math.roundToLong

/**
 * 雕刻数据存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class EngraveModel : ViewModel(), IViewModel {

    /**当前选中的雕刻参数*/
    var engraveOptionInfoData =
        vmData(EngraveOptionInfo(_string(R.string.material_custom), 100, 10, 1))

    /**当前正在雕刻的数据*/
    var engraveInfoData = vmDataNull<EngraveDataInfo>()

    /**设置需要雕刻的数据*/
    @AnyThread
    fun setEngraveDataInfo(info: EngraveDataInfo) {
        engraveInfoData.postValue(info)
    }

    /**更新雕刻数据信息*/
    @AnyThread
    fun updateEngraveDataInfo(block: EngraveDataInfo.() -> Unit) {
        engraveInfoData.value?.let {
            it.block()
            setEngraveDataInfo(it)
        }
    }

    //雕刻历史数据表
    var engraveHistoryEntity: EngraveHistoryEntity? = null

    /**开始雕刻*/
    fun startEngrave() {
        engraveInfoData.value?.apply {
            printTimes = 0
            startEngraveTime = nowTime()
            if (!isFromHistory) {
                //不是历史雕刻的数据
                lpBoxOf(EngraveHistoryEntity::class) {
                    engraveHistoryEntity =
                        findLast(EngraveHistoryEntity_.index.equal(index ?: -1))
                            ?: EngraveHistoryEntity()

                    engraveHistoryEntity?.apply {
                        //入库
                        updateToEntity(this)
                        engraveOptionInfoData.value?.updateToEntity(this)
                        saveEntity(LPBox.PACKAGE_NAME)
                    }
                }
            }
        }
    }

    /**停止雕刻了*/
    fun stopEngrave() {
        engraveInfoData.value?.apply {
            stopEngraveTime = nowTime()
            val time = stopEngraveTime - startEngraveTime
            var dTime = -1L
            if (time > 0 && printTimes > 0) {
                dTime = time / printTimes
            }
            engraveHistoryEntity?.apply {
                //更新雕刻历史数据, 更新雕刻耗时
                duration = dTime
                saveEntity(LPBox.PACKAGE_NAME)
            }
            engraveHistoryEntity = null
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

        val startEngraveTime = engraveInfoData.value?.startEngraveTime ?: -1
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

}