package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import android.view.View
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.DslTagGroupItem
import com.angcyo.item.data.LabelDesData
import com.angcyo.laserpacker.device.filterLayerDpi
import com.angcyo.laserpacker.device.toEngraveTime
import com.angcyo.library.ex._color
import com.angcyo.library.ex.nowTime
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.widget.DslViewHolder

/**
 * 雕刻信息展示的item, 雕刻中的信息展示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/20
 */
open class EngravingInfoItem : DslTagGroupItem() {

    /**雕刻任务id*/
    var itemTaskId: String? = null

    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    /**当前的雕刻任务*/
    val _engraveTaskEntity: EngraveTaskEntity?
        get() = EngraveFlowDataHelper.getEngraveTask(itemTaskId)

    /**当前任务正在雕刻的雕刻配置*/
    val _currentEngraveConfigEntity: EngraveConfigEntity?
        get() = EngraveFlowDataHelper.getCurrentEngraveConfig(itemTaskId)

    /**当前任务的传输配置*/
    val _transferConfigEntity: TransferConfigEntity?
        get() = EngraveFlowDataHelper.getTransferConfig(itemTaskId)

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
        val engraveTaskEntity = _engraveTaskEntity
        val engraveConfigEntity = _currentEngraveConfigEntity
        val transferConfigEntity = _transferConfigEntity
        val engraveDataEntity = EngraveFlowDataHelper.getCurrentEngraveDataEntity(itemTaskId)
        val transferDataList = EngraveFlowDataHelper.getTransferDataList(itemTaskId)

        renderLabelDesList {
            engraveConfigEntity?.let {
                //雕刻模式
                add(moduleData(engraveConfigEntity.type, engraveConfigEntity.moduleState))

                if (!deviceStateModel.isPenMode(engraveConfigEntity.moduleState)) {
                    //材质:
                    add(materialData(EngraveFlowDataHelper.getCurrentEngraveMaterName(itemTaskId)))

                    //分辨率: 1k
                    var dpi = engraveConfigEntity.dpi
                        ?: transferConfigEntity?.getLayerConfigDpi(
                            it.layerId,
                            HawkEngraveKeys.getLastLayerDpi(it.layerId)
                        )
                        ?: transferDataList.firstOrNull()?.dpi ?: LaserPeckerHelper.DPI_254
                    it.layerId?.let {
                        dpi = it.filterLayerDpi(dpi)
                    }
                    val findPxInfo = LaserPeckerHelper.findPxInfo(
                        it.layerId ?: LaserPeckerHelper.LAYER_LINE,
                        dpi
                    )
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
                    //加速级别
                    add(precisionData("${engraveConfigEntity.precision}"))
                }
                if (deviceStateModel.isPenMode(engraveConfigEntity.moduleState)) {
                    //画笔模式, 雕刻速度
                    add(velocityData("${EngraveCmd.depthToSpeed(engraveConfigEntity.depth)}%"))
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