package com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.parse.toDeviceStr
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.item.DslTagGroupItem
import com.angcyo.laserpacker.device.DeviceHelper
import com.angcyo.laserpacker.device.toEngraveTime
import com.angcyo.library.component.watchCount
import com.angcyo.library.ex.isDebug

/**
 * 雕刻完成信息item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
class EngraveFinishTopItem : DslTagGroupItem() {

    /**雕刻任务id, 通过id可以查询到各种信息*/
    var itemTaskId: String? = null

    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    init {
        itemLayoutId = R.layout.item_engrave_finish_top_layout
        itemTagLayoutId = R.layout.dsl_tag_item_vertical

        //分享雕刻日志
        itemClick = if (isDebug()) {
            {
                DeviceHelper.shareEngraveLog()
            }
        } else {
            {
                it.watchCount(5) {
                    DeviceHelper.shareEngraveLog()
                }
            }
        }
    }

    override fun initLabelDesList() {

        val taskEntity = EngraveFlowDataHelper.getEngraveTask(itemTaskId)
        val layerList = EngraveFlowDataHelper.getEngraveLayerList(itemTaskId)
        val engraveConfigEntity = EngraveFlowDataHelper.getEngraveConfig(
            itemTaskId,
            layerList.firstOrNull()?.layerId
        )

        val transferConfigEntity = EngraveFlowDataHelper.getTransferConfig(itemTaskId)

        renderLabelDesList {
            engraveConfigEntity?.let {
                it.productName?.let { productName ->
                    val exDevice = it.exDevice?.toDeviceStr()
                    if (exDevice.isNullOrBlank()) {
                        add(productNameData(productName))
                    } else {
                        add(productNameData("${productName}-${exDevice}"))
                    }
                }
            }

            transferConfigEntity?.let {
                if (it.originWidth != null && it.originHeight != null) {
                    add(widthHeightData(it.originWidth ?: 0f, it.originHeight ?: 0f))
                }
            }

            val startEngraveTime = taskEntity?.startTime ?: 0
            val endEngraveTime = taskEntity?.finishTime ?: 0
            val engraveTime = (endEngraveTime - startEngraveTime).toEngraveTime()
            add(workTimeData(engraveTime))
        }
    }

}