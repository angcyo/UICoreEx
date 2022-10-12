package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.toEngraveTime
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex._string

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishTopItem : DslTagGroupItem() {

    /**雕刻任务id, 通过id可以查询到各种信息*/
    var itemTaskId: String? = null

    init {
        itemLayoutId = R.layout.item_engrave_finish_top_layout
        itemTagLayoutId = R.layout.dsl_tag_item_vertical
    }

    override fun initLabelDesList() {

        val taskEntity = EngraveFlowDataHelper.getEngraveTask(itemTaskId)
        val layerList = EngraveFlowDataHelper.getEngraveLayerList(itemTaskId)
        val engraveConfigEntity =
            EngraveFlowDataHelper.getEngraveConfig(itemTaskId, layerList.firstOrNull()?.mode ?: 0)
        val materialEntity = EngraveHelper.getMaterial(engraveConfigEntity?.materialCode)

        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(itemTaskId)

        renderLabelDesList {
            add(LabelDesData("${_string(R.string.custom_material)}:", materialEntity.toText()))

            val pxInfo = LaserPeckerHelper.findPxInfo(transferConfigEntity?.dpi)
            add(LabelDesData("${_string(R.string.resolution_ratio)}:", pxInfo.des))

            val startEngraveTime = taskEntity?.startTime ?: 0
            val endEngraveTime = taskEntity?.finishTime ?: 0
            val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
            add(LabelDesData("${_string(R.string.work_time)}:", engraveTime))
        }
    }

}