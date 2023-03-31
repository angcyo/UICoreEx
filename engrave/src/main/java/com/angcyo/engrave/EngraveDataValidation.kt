package com.angcyo.engrave

import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.laserpacker.device.EngraveHelper
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
    fun validation(context: Context?, canvasDelegate: CanvasDelegate?): Boolean {
        canvasDelegate ?: return true
        val laserPeckerModel = vmApp<LaserPeckerModel>()

        if (laserPeckerModel.isZOpen()) {
            //所有设备的第三轴模式下, 不允许雕刻GCode数据
            val gCodeLayer =
                EngraveHelper.engraveLayerList.find { it.layerMode == CanvasConstant.DATA_MODE_GCODE }
            if (gCodeLayer != null) {
                val rendererList =
                    EngraveTransitionManager.getRendererList(canvasDelegate, gCodeLayer, false)
                if (rendererList.isNotEmpty()) {
                    //不允许雕刻GCode
                    context?.messageDialog {
                        dialogTitle = _string(R.string.engrave_warn)
                        dialogMessage =
                            "当前模式下, 不允许雕刻\"${gCodeLayer.label}\"数据;\nData that is not allowed!"
                    }
                    return false
                }
            }
        }

        val isC1 = laserPeckerModel.isC1()
        if (isC1) {
            if (laserPeckerModel.isPenMode()) {
                //C1的握笔模式下, 只允许雕刻GCode数据
                val gCodeLayer =
                    EngraveHelper.engraveLayerList.find { it.layerMode == CanvasConstant.DATA_MODE_GCODE }
                if (gCodeLayer != null) {
                    val notGCodeRendererList = EngraveTransitionManager.getRendererListNot(
                        canvasDelegate,
                        gCodeLayer,
                        false
                    )
                    if (notGCodeRendererList.isNotEmpty()) {
                        //不允许雕刻非GCode数据
                        context?.messageDialog {
                            dialogTitle = _string(R.string.engrave_warn)
                            dialogMessage =
                                "当前模式下, 不允许雕刻非\"${gCodeLayer.label}\"数据;\nData that is not allowed!"
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
                val rendererList = EngraveTransitionManager.getRendererList(canvasDelegate)
                for (render in rendererList) {
                    val rotateBounds = render.getRotateBounds()

                    @MM
                    val height = rotateBounds.height().toMm()
                    if (height > maxHeight) {
                        context?.messageDialog {
                            dialogTitle = _string(R.string.engrave_warn)
                            dialogMessage =
                                "当前模式下, 不允许雕刻高度大于${maxHeight.unitDecimal(0)}mm的数据;\nData that is not allowed!"
                        }
                        return false
                    }
                }
            }
        }

        return true
    }

}