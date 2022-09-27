package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.toZModeString
import com.angcyo.engrave.data.LabelDesData
import com.angcyo.engrave.toEngraveTime
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
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

    //所有需要提示的数据
    val labelDesList = mutableListOf<LabelDesData>()

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
        labelDesList.clear()
        //不是恢复的数据
        labelDesList.add(
            LabelDesData(
                _string(R.string.device_setting_tips_fourteen_11),
                QuerySettingParser.Z_MODEL.toZModeString()
            )
        )

        labelDesList.add(
            LabelDesData(
                _string(R.string.laser_type),
                ""
            )
        )

        //分辨率: 1k
        labelDesList.add(
            LabelDesData(
                _string(R.string.resolution_ratio),
                ""
            )
        )
        //材质:
        labelDesList.add(
            LabelDesData(
                _string(R.string.custom_material),
                ""
            )
        )

        //功率:
        labelDesList.add(
            LabelDesData(
                _string(R.string.custom_power),
                "0%"
            )
        )

        //深度:
        labelDesList.add(
            LabelDesData(
                _string(R.string.custom_speed),
                "0%"
            )
        )

        //雕刻次数
        val times = 1
        val printTimes = 1
        labelDesList.add(
            LabelDesData(_string(R.string.print_times), "${printTimes}/${times}")
        )

        //加工时间
        val startEngraveTime = 1

        if (startEngraveTime > 0) {
            var engraveTime = (nowTime() - startEngraveTime).toEngraveTime()
            labelDesList.add(LabelDesData(_string(R.string.tips_fourteen_12), engraveTime))
            labelDesList.add(LabelDesData(_string(R.string.work_time), engraveTime))
        }

        itemHolder.flow(R.id.lib_flow_layout)
            ?.resetChild(labelDesList, R.layout.dsl_solid_tag_item) { itemView, item, itemIndex ->
                itemView.dslViewHolder().tv(R.id.lib_label_view)?.text = item.label
                itemView.dslViewHolder().tv(R.id.lib_des_view)?.text = item.des
            }
    }
}