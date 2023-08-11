package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.laserpacker.device.filterLayerDpi

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishInfoItem : EngravingInfoItem() {

    /**雕刻图层id*/
    var itemLayerId: String? = LaserPeckerHelper.LAYER_FILL

    init {
        itemLayoutId = R.layout.item_engrave_finish_info_layout
        itemTagLayoutId = R.layout.dsl_tag_item
    }

    override fun initLabelDesList() {
        renderLabelDesList {
            val transferConfigEntity = _transferConfigEntity
            val engraveConfigEntity =
                EngraveFlowDataHelper.getEngraveConfig(itemTaskId, itemLayerId)
            val transferDataEntityList =
                EngraveFlowDataHelper.getLayerTransferData(itemTaskId, itemLayerId)

            engraveConfigEntity?.let {
                //雕刻模块
                add(moduleData(engraveConfigEntity.type, engraveConfigEntity.moduleState))

                //材质 分辨率
                add(materialData(EngraveFlowDataHelper.getEngraveMaterNameByKey(engraveConfigEntity.materialKey)))
                var dpi = engraveConfigEntity.dpi
                    ?: transferConfigEntity?.getLayerConfigDpi(itemLayerId)
                    ?: transferDataEntityList.firstOrNull()?.dpi ?: LaserPeckerHelper.DPI_254
                itemLayerId?.let {
                    dpi = it.filterLayerDpi(dpi)
                }
                val findPxInfo =
                    LaserPeckerHelper.findPxInfo(itemLayerId ?: LaserPeckerHelper.LAYER_LINE, dpi)
                add(resolutionData(findPxInfo.toText()))

                if (deviceStateModel.isPenMode(it.moduleState)) {
                    //握笔模块下,只有 加速级别 雕刻速度
                    add(precisionData("${engraveConfigEntity.precision}"))

                    //雕刻速度
                    add(velocityData("${EngraveCmd.depthToSpeed(engraveConfigEntity.depth)}%"))
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