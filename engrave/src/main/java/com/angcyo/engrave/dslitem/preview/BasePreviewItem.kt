package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
abstract class BasePreviewItem : DslAdapterItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    /**查询设备状态*/
    fun queryDeviceStateCmd() {
        laserPeckerModel.queryDeviceState()
    }

    /**z轴滚动预览*/
    fun zContinuePreviewCmd() {
        val cmd = EngravePreviewCmd.previewZContinue()
        cmd.enqueue()
        queryDeviceStateCmd()
    }
}