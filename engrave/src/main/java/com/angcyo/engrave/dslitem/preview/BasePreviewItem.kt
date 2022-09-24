package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.CanvasDelegate
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
    val previewModel = vmApp<PreviewModel>()

    /**用来获取预览的元素Bounds*/
    var itemCanvasDelegate: CanvasDelegate? = null

    /**查询设备状态*/
    fun queryDeviceStateCmd() {
        laserPeckerModel.queryDeviceState()
    }

    /**开始预览*/
    fun startPreviewCmd(updateState: Boolean, async: Boolean, zPause: Boolean = false) {
        previewModel.startPreview(itemCanvasDelegate, async, zPause)
        if (updateState) {
            queryDeviceStateCmd()
        }
    }

    /**停止预览*/
    fun stopPreviewCmd() {
        val cmd = EngravePreviewCmd.previewStop()
        cmd.enqueue()
        queryDeviceStateCmd()
    }

    /**z轴滚动预览*/
    fun zContinuePreviewCmd() {
        val cmd = EngravePreviewCmd.previewZContinue()
        cmd.enqueue()
        laserPeckerModel.queryDeviceState()
    }

}