package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.data.LabelDesData
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.toEngraveTime
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.or
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.dslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.flow

/**
 * 雕刻信息展示的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */
open class EngravingInfoItem : DslAdapterItem() {

    /**雕刻的信息*/
    var itemEngraveState: EngraveModel.EngraveState? = null

    val engraveMode = vmApp<EngraveModel>()

    //所有需要提示的数据
    val _labelDesList = mutableListOf<LabelDesData>()

    //布局id
    var labelLayoutId = R.layout.dsl_solid_tag_item

    init {
        itemLayoutId = R.layout.item_engrave_info_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        //提示
        _labelDesList.clear()
        onCreateLabelList()

        itemHolder.flow(R.id.lib_flow_layout)
            ?.resetChild(_labelDesList, R.layout.dsl_solid_tag_item) { itemView, item, itemIndex ->
                itemView.dslViewHolder().tv(R.id.lib_label_view)?.text = item.label
                itemView.dslViewHolder().tv(R.id.lib_des_view)?.text = item.des
            }
    }

    open fun onCreateLabelList() {
        itemEngraveState?.let { engraveState ->
            val engraveDataParam = engraveState.engraveDataParam
            val engraveData = engraveDataParam.dataList.first()

            //材质:
            _labelDesList.add(
                LabelDesData(_string(R.string.custom_material), engraveDataParam.materialName)
            )
            //分辨率: 1k
            val findPxInfo = LaserPeckerHelper.findPxInfo(engraveData.px)
            _labelDesList.add(
                LabelDesData(_string(R.string.resolution_ratio), findPxInfo?.des.or())
            )

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
            val printTimes = engraveState.printTimes
            _labelDesList.add(
                LabelDesData(_string(R.string.print_times), "${printTimes}/${times}")
            )

            //加工时间
            val startEngraveTime = engraveMode._engraveTask?.startTime ?: 0
            val engraveTime = (nowTime() - startEngraveTime).toEngraveTime()
            _labelDesList.add(LabelDesData(_string(R.string.work_time), engraveTime))
        }

        /*_labelDesList.add(
            LabelDesData(_string(R.string.laser_type), "")
        )*/
    }
}