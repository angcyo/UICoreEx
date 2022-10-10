package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.data.LabelDesData
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

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
        labelLayoutId = R.layout.dsl_tag_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
    }

    override fun onCreateLabelList() {
        val engraveConfigEntity = EngraveFlowDataHelper.getCurrentEngraveConfig(itemTaskId)
        val transferDataEntityList =
            EngraveFlowDataHelper.getLayerTransferData(itemTaskId, itemLayerMode)

        engraveConfigEntity?.let {
            //功率:
            _labelDesList.add(
                LabelDesData(_string(R.string.custom_power), "${engraveConfigEntity.power}%")
            )

            //深度:
            _labelDesList.add(
                LabelDesData(_string(R.string.custom_speed), "${engraveConfigEntity.depth}%")
            )

            //雕刻次数
            val transferDataEntity = transferDataEntityList.firstOrNull()
            val times = engraveConfigEntity.time
            val engraveDataEntity = EngraveFlowDataHelper.getEngraveDataEntity(
                itemTaskId,
                transferDataEntity?.index ?: 0
            )
            val printTimes = engraveDataEntity?.printTimes
            _labelDesList.add(
                LabelDesData(_string(R.string.print_times), "${printTimes}/${times}")
            )
        }
    }

}