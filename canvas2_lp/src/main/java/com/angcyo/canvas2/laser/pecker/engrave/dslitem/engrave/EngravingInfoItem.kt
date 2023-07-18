package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.laserpacker.device.toEngraveTime
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻信息展示的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */
open class EngravingInfoItem : DslTagGroupItem() {

    /**雕刻任务id*/
    var itemTaskId: String? = null

    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    init {
        itemLayoutId = R.layout.item_engrave_info_layout
    }

    override fun onInitTagItemLayout(
        viewHolder: DslViewHolder,
        itemView: View,
        item: LabelDesData,
        itemIndex: Int
    ) {
        super.onInitTagItemLayout(viewHolder, itemView, item, itemIndex)
        viewHolder.tv(R.id.lib_des_view)?.setTextColor(_color(R.color.device_primary_color))
    }

    override fun initLabelDesList() {
        val engraveTaskEntity = EngraveFlowDataHelper.getEngraveTask(itemTaskId)
        val engraveConfigEntity = EngraveFlowDataHelper.getCurrentEngraveConfig(itemTaskId)
        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(itemTaskId)
        val engraveDataEntity = EngraveFlowDataHelper.getCurrentEngraveDataEntity(itemTaskId)
        val transferDataList = EngraveFlowDataHelper.getTransferDataList(itemTaskId)

        renderLabelDesList {
            engraveConfigEntity?.let {
                if (!deviceStateModel.isPenMode(engraveConfigEntity.moduleState)) {
                    //材质:
                    add(materialData(EngraveFlowDataHelper.getCurrentEngraveMaterName(itemTaskId)))

                    //分辨率: 1k
                    val dpi = transferConfigEntity?.dpi ?: transferDataList.firstOrNull()?.dpi
                    val findPxInfo = LaserPeckerHelper.findPxInfo(dpi)
                    add(resolutionData(findPxInfo.toText()))
                }

                //雕刻精度
                if (laserPeckerModel.isCSeries()) {
                    /*add(
                        LabelDesData(
                            _string(R.string.engrave_speed),
                            "${engraveConfigEntity.toEngravingSpeed()}%"
                        )
                    )*/
                    add(precisionData("${engraveConfigEntity.precision}"))
                }
                if (deviceStateModel.isPenMode(engraveConfigEntity.moduleState)) {
                    //画笔模式
                    add(
                        LabelDesData(
                            _string(R.string.engrave_speed),
                            "${EngraveCmd.depthToSpeed(engraveConfigEntity.depth)}%"
                        )
                    )
                } else {
                    //功率:
                    add(powerData(engraveConfigEntity.power))

                    //深度:
                    add(depthData(engraveConfigEntity.depth))

                    //雕刻次数
                    val times = engraveConfigEntity.time
                    val printTimes = engraveDataEntity?.printTimes ?: 0
                    add(timesData(printTimes, times))
                }

                //加工时间
                val startEngraveTime = engraveTaskEntity?.startTime ?: 0
                val engraveTime = (nowTime() - startEngraveTime).toEngraveTime()
                add(workTimeData(engraveTime))

                if (deviceStateModel.deviceStateData.value?.isEngraving() == true) {
                    //雕刻中才显示,剩余时长
                    val duration = EngraveFlowDataHelper.calcEngraveProgressDuration(itemTaskId)
                    if (duration > 3000) {
                        //大于3秒才显示
                        add(remainingTimesData(duration.toEngraveTime()))
                    }
                }
            }

            /*add( LabelDesData(_string(R.string.laser_type), "") )*/
        }
    }
}