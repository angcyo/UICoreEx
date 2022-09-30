package com.angcyo.engrave.dslitem.engrave

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.EngraveDataParam
import com.angcyo.engrave.data.LabelDesData
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishInfoItem : EngravingInfoItem() {

    /**雕刻参数信息*/
    var itemEngraveDataParam: EngraveDataParam? = null

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
        itemEngraveDataParam?.let { engraveDataParam ->
            //功率:
            _labelDesList.add(
                LabelDesData(_string(R.string.custom_power), "${engraveDataParam.power}%")
            )

            //深度:
            _labelDesList.add(
                LabelDesData(_string(R.string.custom_speed), "${engraveDataParam.depth}%")
            )

            //雕刻次数
            val times = engraveDataParam.time
            val printTimes = engraveDataParam.dataList.first().printTimes
            _labelDesList.add(
                LabelDesData(_string(R.string.print_times), "${printTimes}/${times}")
            )
        }
    }

}