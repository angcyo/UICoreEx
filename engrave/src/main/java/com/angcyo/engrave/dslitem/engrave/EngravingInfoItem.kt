package com.angcyo.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.EngraveFlowDataHelper
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.toEngraveTime
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime

/**
 * 雕刻信息展示的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */
open class EngravingInfoItem : DslTagGroupItem() {

    /**雕刻任务id*/
    var itemTaskId: String? = null

    init {
        itemLayoutId = R.layout.item_engrave_info_layout
    }

    override fun initLabelDesList() {
        val engraveTaskEntity = EngraveFlowDataHelper.getEngraveTask(itemTaskId)
        val engraveConfigEntity = EngraveFlowDataHelper.getCurrentEngraveConfig(itemTaskId)
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(itemTaskId)
        val engraveDataEntity = EngraveFlowDataHelper.getEngraveDataEntity(itemTaskId)

        renderLabelDesList {
            engraveConfigEntity?.let {
                val materialEntity = EngraveHelper.getMaterial(engraveConfigEntity.materialCode)

                //材质:
                add(LabelDesData(_string(R.string.custom_material), materialEntity.toText()))
                //分辨率: 1k
                val findPxInfo = LaserPeckerHelper.findPxInfo(transferConfigEntity?.dpi)
                add(LabelDesData(_string(R.string.resolution_ratio), findPxInfo.des))

                //功率:
                add(LabelDesData(_string(R.string.custom_power), "${engraveConfigEntity.power}%"))

                //深度:
                add(LabelDesData(_string(R.string.custom_speed), "${engraveConfigEntity.depth}%"))

                //雕刻次数
                val times = engraveConfigEntity.time
                val printTimes = engraveDataEntity?.printTimes
                add(LabelDesData(_string(R.string.print_times), "${printTimes}/${times}"))

                //加工时间
                val startEngraveTime = engraveTaskEntity?.startTime ?: 0
                val engraveTime = (nowTime() - startEngraveTime).toEngraveTime()
                add(LabelDesData(_string(R.string.work_time), engraveTime))
            }

            /*add( LabelDesData(_string(R.string.laser_type), "") )*/
        }
    }
}