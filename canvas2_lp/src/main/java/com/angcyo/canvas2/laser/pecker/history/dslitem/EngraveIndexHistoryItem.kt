package com.angcyo.canvas2.laser.pecker.history.dslitem

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.data.LabelDesData
import com.angcyo.laserpacker.device.toEngraveTime
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity

/**
 * 雕刻历史item, 机器的历史
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-1-5
 */
class EngraveIndexHistoryItem : EngraveHistoryItem() {

    /**雕刻的数据实体*/
    var itemEngraveDataEntity: EngraveDataEntity? = null
        set(value) {
            field = value
            if (value != null) {
                EngraveFlowDataHelper.getTransferData(
                    value.index,
                    deviceAddress = LaserPeckerHelper.lastDeviceAddress()
                )?.let {
                    itemTransferDataEntityList = listOf(it)
                }
                value.taskId?.let {
                    itemEngraveConfigEntityList = EngraveFlowDataHelper.getTaskEngraveConfigList(it)
                }
            }
        }

    override fun onInitEngraveTime(list: MutableList<LabelDesData>) {
        val startEngraveTime = itemEngraveDataEntity?.startTime ?: 0
        val endEngraveTime = itemEngraveDataEntity?.finishTime ?: 0
        if (endEngraveTime > 0 && startEngraveTime > 0 && endEngraveTime > startEngraveTime) {
            val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
            list.add(formatLabelDes(_string(R.string.work_time), engraveTime))
        } else {
            list.add(formatLabelDes(_string(R.string.work_time), "--"))
        }
    }
}