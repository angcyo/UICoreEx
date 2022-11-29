package com.angcyo.engrave

import com.angcyo.engrave.model.PreviewModel
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity

/**
 * 历史雕刻流程处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/28
 */
class HistoryEngraveFlowLayoutHelper : EngraveFlowLayoutHelper() {

    /**历史雕刻的数据*/
    var historyEngraveDataEntity: EngraveDataEntity? = null
        set(value) {
            field = value
            flowTaskId = null
            if (value != null) {
                itemTransferDataEntity =
                    EngraveFlowDataHelper.getTransferData(value.index, value.taskId)
            }
        }

    /**传输的数据*/
    private var itemTransferDataEntity: TransferDataEntity? = null

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        historyEngraveDataEntity?.let {
            //使用数据索引, 创建一个雕刻任务id
            if (flowTaskId == null) {
                flowTaskId = EngraveFlowDataHelper.generateTask(it.index, it.taskId)
            }
        }
        if (to != ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            super.onEngraveFlowChanged(from, to)
        }
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            //历史文档直接进入雕刻配置
            engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
        } else if (to == ENGRAVE_FLOW_PREVIEW) {
            //预览界面, 创建预览信息, 并开始预览
            previewModel.startPreview(PreviewModel.createPreviewInfo(itemTransferDataEntity))
        }
    }

}