package com.angcyo.canvas2.laser.pecker.history.dslitem

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.data.LabelDesData
import com.angcyo.laserpacker.device.toEngraveTime
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity

/**
 * app的雕刻历史
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/05
 */
class EngraveTaskHistoryItem : EngraveHistoryItem() {

    /**雕刻任务*/
    var itemEngraveTaskEntity: EngraveTaskEntity? = null
        set(value) {
            field = value
            val taskId = value?.taskId
            val transferList = EngraveFlowDataHelper.getTransferDataList(taskId)
            val engraveConfigList = EngraveFlowDataHelper.getTaskEngraveConfigList(taskId)
            itemTransferDataEntityList = transferList
            itemEngraveConfigEntityList = engraveConfigList
        }

    override fun onInitEngraveTime(list: MutableList<LabelDesData>) {
        val startEngraveTime = itemEngraveTaskEntity?.startTime ?: 0
        val endEngraveTime = itemEngraveTaskEntity?.finishTime ?: 0
        if (endEngraveTime > 0 && startEngraveTime > 0 && endEngraveTime > startEngraveTime) {
            val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
            list.add(formatLabelDes(_string(R.string.work_time), engraveTime))
        } else {
            list.add(formatLabelDes(_string(R.string.work_time), "--"))
        }
    }
}