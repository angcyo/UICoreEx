package com.angcyo.engrave.model

import androidx.lifecycle.ViewModel
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
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

    /**开始雕刻的时间, 毫秒*/
    var startEngraveTime: Long = 0L

    /**开始雕刻*/
    fun startEngrave() {
        startEngraveTime = nowTime()
    }

    /**停止雕刻了*/
    fun stopEngrave() {
        startEngraveTime = 0L
    }

    /**计算雕刻剩余时间, 毫秒
     * [rate] 打印进度百分比[0-100]*/
    fun calcEngraveRemainingTime(rate: Int): Long {
        if (rate <= 0) {
            return -1
        } else if (rate >= 100) {
            return 0
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