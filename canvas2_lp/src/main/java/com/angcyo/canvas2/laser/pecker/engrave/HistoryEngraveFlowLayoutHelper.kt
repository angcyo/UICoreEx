package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.bluetooth.fsc.laserpacker.writeEngraveLog
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity

/**
 * 历史雕刻流程处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/28
 */
class HistoryEngraveFlowLayoutHelper : EngraveFlowLayoutHelper() {

    /**设备历史雕刻的数据*/
    var deviceHistoryEngraveDataEntity: EngraveDataEntity? = null
        set(value) {
            field = value
            clearFlowId("设置设备历史雕刻数据")
        }

    /**app历史雕刻的数据*/
    var appHistoryEngraveTaskEntity: EngraveTaskEntity? = null
        set(value) {
            field = value
            clearFlowId("设置App历史雕刻数据")
        }

    /**传输的数据, 如果没有传输的数据
     * [deviceHistoryEngraveDataEntity] or [appHistoryEngraveTaskEntity] 赋值一个
     * 这个属性一定要赋值, 用来生成预览bounds
     * */
    var transferDataEntityList: List<TransferDataEntity>? = null

    override fun onEngraveFlowChanged(from: Int, to: Int) {
        deviceHistoryEngraveDataEntity?.let { entity ->
            //使用数据索引, 创建一个雕刻任务id
            if (to > 0 && flowTaskId == null) {
                flowTaskId = if (transferDataEntityList.isNullOrEmpty()) {
                    //不在本机传输的数据
                    EngraveFlowDataHelper.generateSingleTask(entity.index, entity.taskId)
                } else {
                    EngraveFlowDataHelper.generateTask(entity.index, entity.taskId)
                }
                "生成历史流程id[$flowTaskId]".writeEngraveLog()
            }
        }
        appHistoryEngraveTaskEntity?.let {
            "设置历史流程id[$flowTaskId]->[${it.taskId}]".writeEngraveLog()
            flowTaskId = it.taskId
        }
        if (to != ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            super.onEngraveFlowChanged(from, to)
        }
        if (to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG) {
            //历史文档直接进入雕刻配置
            if (from == ENGRAVE_FLOW_TRANSMITTING &&
                transferModel._transferState?.state == TransferState.TRANSFER_STATE_CANCEL
            ) {
                //传输数据被取消, 则直接关闭页面
                engraveBackFlow = 0
                engraveFlow = -1
                cancelable = true
                back()
            } else {
                engraveFlow = if (appHistoryEngraveTaskEntity == null) {
                    //机器数据, 数据一定存在
                    ENGRAVE_FLOW_BEFORE_CONFIG
                } else {
                    //app的历史记录, 有可能没有传输数据
                    ENGRAVE_FLOW_AUTO_TRANSFER
                }
            }
        } else if (to == ENGRAVE_FLOW_PREVIEW) {
            //预览界面, 创建预览信息, 并开始预览
            if (transferDataEntityList.isNullOrEmpty()) {
                //不在本机传输的数据, 则直接进入雕刻配置界面
                engraveFlow = ENGRAVE_FLOW_BEFORE_CONFIG
            } else {
                previewModel.startPreview(PreviewModel.createPreviewInfo(transferDataEntityList))
            }
        }
    }

    override fun onStartEngrave(taskId: String?) {
        super.onStartEngrave(taskId)
        if (appHistoryEngraveTaskEntity == null) {
            EngraveFlowDataHelper.getEngraveDataEntity(taskId)?.let {
                it.clearEngraveData()
                it.lpSaveEntity()
            }
        } else {
            //清除缓存状态数据
            EngraveFlowDataHelper.againEngrave(appHistoryEngraveTaskEntity?.taskId)
        }
    }

    override fun renderEngraveFinish() {
        super.renderEngraveFinish()
        showCloseView(true, _string(R.string.ui_quit))
    }

}