package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.data.LabelDesData
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.library.ex._string

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishInfoItem : EngravingInfoItem() {

    /**雕刻图层id*/
    var itemLayerId: String? = LayerHelper.LAYER_FILL

    init {
        itemLayoutId = R.layout.item_engrave_finish_info_layout
        itemTagLayoutId = R.layout.dsl_tag_item
    }

    override fun initLabelDesList() {
        renderLabelDesList {
            val engraveConfigEntity =
                EngraveFlowDataHelper.getEngraveConfig(itemTaskId, itemLayerId)
            val transferDataEntityList =
                EngraveFlowDataHelper.getLayerTransferData(itemTaskId, itemLayerId)

            engraveConfigEntity?.let {
                if (deviceStateModel.isPenMode(it.moduleState)) {
                    //握笔模块下,只有 加速级别 雕刻速度
                    add(
                        LabelDesData(
                            _string(R.string.engrave_precision),
                            "${engraveConfigEntity.precision}"
                        )
                    )

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
                    val transferDataEntity = transferDataEntityList.firstOrNull()
                    val times = engraveConfigEntity.time
                    val engraveDataEntity = EngraveFlowDataHelper.getEngraveDataEntity(
                        itemTaskId,
                        transferDataEntity?.index ?: 0
                    )
                    val printTimes = engraveDataEntity?.printTimes ?: 0
                    add(timesData(printTimes, times))
                }
            }
        }
    }
}