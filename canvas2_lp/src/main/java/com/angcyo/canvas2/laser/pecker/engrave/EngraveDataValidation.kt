package com.angcyo.canvas2.laser.pecker.engrave

import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.toDataMode
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex._string
import com.angcyo.library.unit.toMm
import com.angcyo.library.unit.unitDecimal

/**
 * 雕刻数据验证, 验证数据是否能够被雕刻
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/12
 */
object EngraveDataValidation {

    /**验证数据是否合法, 并自动弹窗提示*/
    fun validation(context: Context?, canvasDelegate: CanvasRenderDelegate?): Boolean {
        canvasDelegate ?: return true
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        val deviceStateModel = vmApp<DeviceStateModel>()

        val layerList = LayerHelper.getEngraveLayerList()

        if (laserPeckerModel.isZOpen()) {
            //所有设备的第三轴模式下, 不允许雕刻GCode数据
            val gCodeLayer =
                layerList.find { it.layerId.toDataMode() == LPDataConstant.DATA_MODE_GCODE }
            if (gCodeLayer != null) {
                val rendererList =
                    LPEngraveHelper.getLayerRendererList(canvasDelegate, gCodeLayer, false)
                if (rendererList.isNotEmpty()) {
                    //不允许雕刻GCode
                    context?.messageDialog {
                        dialogTitle = _string(R.string.engrave_warn)
                        dialogMessage = _string(R.string.data_not_allowed, gCodeLayer.label)
                    }
                    return false
                }
            }
        }

        val isCSeries = laserPeckerModel.isCSeries()
        if (isCSeries) {
            if (deviceStateModel.isPenMode()) {
                //C1的握笔模式下, 只允许雕刻GCode数据
                val gCodeLayer =
                    layerList.find { it.layerId.toDataMode() == LPDataConstant.DATA_MODE_GCODE }
                if (gCodeLayer != null) {
                    val notGCodeRendererList =
                        LPEngraveHelper.getLayerRendererListNot(canvasDelegate, gCodeLayer, false)
                    if (notGCodeRendererList.isNotEmpty()) {
                        //不允许雕刻非GCode数据
                        context?.messageDialog {
                            dialogTitle = _string(R.string.engrave_warn)
                            dialogMessage =
                                _string(R.string.data_not_allowed_reverse, gCodeLayer.label)
                        }
                        return false
                    }
                }
            }
        }

        val isL4 = laserPeckerModel.isL4()
        if (isL4) {
            if (laserPeckerModel.isSRepMode()) {
                //滑台多文件雕刻模式下, 单个文件的高度不能超过120mm
                val previewBounds = laserPeckerModel.productInfoData.value?.previewBounds

                @MM
                val maxHeight = previewBounds?.height()?.toMm() ?: 120f
                val rendererList = LPEngraveHelper.getLayerRendererList(canvasDelegate)
                for (render in rendererList) {
                    val rotateBounds = render.renderProperty?.getRenderBounds()

                    @MM
                    val height = rotateBounds?.height()?.toMm() ?: 0f
                    if (height > maxHeight) {
                        context?.messageDialog {
                            dialogTitle = _string(R.string.engrave_warn)
                            dialogMessage =
                                _string(R.string.data_not_allowed_height, maxHeight.unitDecimal(0))
                        }
                        return false
                    }
                }
            }
        }

        if (laserPeckerModel.isZOpen()) {
            //2023-4-13 Z轴模式下, 只能发送一个文件并雕刻
            val rendererList = LPEngraveHelper.getLayerRendererList(canvasDelegate)
            if (rendererList.size > 1) {
                context?.messageDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = _string(R.string.data_not_allowed_multi)
                }
                return false
            }
        }

        return true
    }

}