package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.data.HawkKeys
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.toast

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
abstract class BasePreviewItem : DslAdapterItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    /**用来获取预览的元素Bounds*/
    var itemCanvasDelegate: CanvasDelegate? = null

    /**查询设备状态*/
    fun queryDeviceStateCmd() {
        laserPeckerModel.queryDeviceState()
    }

    /**开始预览*/
    fun startPreviewCmd(updateState: Boolean, async: Boolean, zPause: Boolean = false) {
        //val selectedRenderer = canvasDelegate?.getSelectedRenderer()

        /*if (previewBoundsInfo == null) {
            //没有强制指定的预览信息

            if (selectedRenderer != null) {
                EngraveHelper.sendPreviewRange(selectedRenderer, updateState, async, zPause)
            } else {
                if (engraveModel.isRestore().not()) {
                    toast("No preview elements!")
                }
                if (updateState) {
                    queryDeviceStateCmd()
                }
            }
        } else {
            previewBoundsInfo?.apply {
                laserPeckerModel.sendUpdatePreviewRange(
                    originRectF,
                    originRectF,
                    originRotate,
                    HawkKeys.lastPwrProgress,
                    updateState,
                    async,
                    zPause,
                    EngraveHelper.getDiameter()
                )
            }
        }*/
    }

    /**停止预览*/
    fun stopPreviewCmd() {
        val cmd = EngravePreviewCmd.previewStop()
        cmd.enqueue()
        queryDeviceStateCmd()
    }

    /**显示中心*/
    fun showPreviewCenterCmd(updateState: Boolean) {
        val cmd = EngravePreviewCmd.previewShowCenter(HawkKeys.lastPwrProgress)
        cmd.enqueue(CommandQueueHelper.FLAG_ASYNC)
        if (updateState) {
            queryDeviceStateCmd()
        }
    }

}