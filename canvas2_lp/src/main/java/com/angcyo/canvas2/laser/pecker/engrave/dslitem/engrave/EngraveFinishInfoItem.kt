package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex._string

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishInfoItem : EngravingInfoItem() {

    /**雕刻图层模式*/
    var itemLayerMode: Int = -1

    init {
        itemLayoutId = R.layout.item_engrave_finish_info_layout
        itemTagLayoutId = R.layout.dsl_tag_item
    }

    override fun initLabelDesList() {
        renderLabelDesList {
            val engraveConfigEntity =
                EngraveFlowDataHelper.getEngraveConfig(itemTaskId, itemLayerMode)
            val transferDataEntityList =
                EngraveFlowDataHelper.getLayerTransferData(itemTaskId, itemLayerMode)

            engraveConfigEntity?.let {
                //功率:
                add(LabelDesData(_string(R.string.custom_power), "${engraveConfigEntity.power}%"))

                //深度:
                add(LabelDesData(_string(R.string.custom_speed), "${engraveConfigEntity.depth}%"))

                //雕刻次数
                val transferDataEntity = transferDataEntityList.firstOrNull()
                val times = engraveConfigEntity.time
                val engraveDataEntity = EngraveFlowDataHelper.getEngraveDataEntity(
                    itemTaskId,
                    transferDataEntity?.index ?: 0
                )
                val printTimes = engraveDataEntity?.printTimes ?: 0
                add(LabelDesData(_string(R.string.print_times), "${printTimes}/${times}"))
            }
        }
    }
}