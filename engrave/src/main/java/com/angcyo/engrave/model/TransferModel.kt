package com.angcyo.engrave.model

import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.engrave.data.TransferDataConfigInfo
import com.angcyo.engrave.data.TransferDataInfo
import com.angcyo.engrave.transition.EngraveTransitionManager

/**
 * 数据传输模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
class TransferModel : ViewModel() {

    companion object {

        /**根据雕刻数据, 返回数据指令*/
        fun getTransferDataCmd(transferDataInfo: TransferDataInfo): DataCmd? {
            val bytes = transferDataInfo.data
            if (bytes == null || bytes.isEmpty()) {
                return null
            }

            //数据类型封装
            val dataCmd: DataCmd = when (transferDataInfo.engraveDataType) {
                //0x10 图片数据
                DataCmd.ENGRAVE_TYPE_BITMAP -> DataCmd.bitmapData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.px,
                    transferDataInfo.name,
                    bytes,
                )
                //0x20 GCode数据
                DataCmd.ENGRAVE_TYPE_GCODE -> DataCmd.gcodeData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.name,
                    transferDataInfo.lines,
                    bytes
                )
                //0x40 黑白画, 线段数据
                DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> DataCmd.bitmapPathData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.px,
                    transferDataInfo.name,
                    transferDataInfo.lines,
                    bytes,
                )
                //0x60 抖动数据, 二进制位
                //DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING ->
                else -> DataCmd.bitmapDitheringData(
                    transferDataInfo.index,
                    transferDataInfo.x,
                    transferDataInfo.y,
                    transferDataInfo.width,
                    transferDataInfo.height,
                    transferDataInfo.px,
                    transferDataInfo.name,
                    bytes
                )
            }
            return dataCmd
        }
    }

    //

    /**开始创建机器需要的数据*/
    fun startCreateData(
        transferDataConfigInfo: TransferDataConfigInfo,
        canvasDelegate: CanvasDelegate
    ) {
        EngraveTransitionManager.getRendererList(canvasDelegate, null)
    }

    //

}