package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.data.CanvasProjectItemBean.Companion.MM_UNIT
import com.angcyo.canvas.graphics.IEngraveProvider
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.component.file.writeTo
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.Private
import com.angcyo.library.utils.FileTextData
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity

/**雕刻数据转换, 将界面渲染的数据, 转换成机器的雕刻数据
 *
 * 数据转换配置信息
 * [com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity]
 *
 * 输出的数据信息
 * [com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
interface IEngraveTransition {

    companion object {
        /**保存雕刻数据到文件
         * [fileName] 需要保存的文件名, 无扩展
         * [suffix] 文件后缀, 扩展名
         * [data]
         *   [String]
         *   [ByteArray]
         *   [Bitmap]
         *   [File]
         * ]*/
        fun saveEngraveData(
            fileName: Any?,
            data: FileTextData?,
            suffix: String = "engrave"
        ): String? {
            //将雕刻数据写入文件
            return data.writeTo(
                CanvasConstant.ENGRAVE_FILE_FOLDER,
                "${fileName}.${suffix}",
                false
            )
        }

        /**数据需要处理成什么格式, 丢给机器雕刻
         * [getDataMode]*/
        fun getDataMode(
            renderer: BaseItemRenderer<*>?,
            transferConfigEntity: TransferConfigEntity
        ): Int {
            if (renderer is DataItemRenderer) {
                return getDataMode(
                    renderer.getRendererRenderItem()?.dataBean,
                    transferConfigEntity
                )
            }
            return transferConfigEntity.dataMode ?: CanvasConstant.DATA_MODE_DITHERING
        }

        /**数据处理的模式
         * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
         * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
         * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING]
         * */
        fun getDataMode(
            bean: CanvasProjectItemBean?,
            transferConfigEntity: TransferConfigEntity
        ): Int {
            return transferConfigEntity.dataMode ?: (bean?._dataMode
                ?: CanvasConstant.DATA_MODE_DITHERING)
        }
    }

    //region---core---

    /**将[engraveProvider]转换成传输给机器的数据
     * 请在数据返回之后, 决定是否要保存值数据库
     * */
    @CallPoint
    fun doTransitionTransferData(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        param: TransitionParam?
    ): TransferDataEntity?

    //endregion---core---

    //region---private---

    /**一些通用配置属性初始化*/
    @Private
    fun initTransferDataEntity(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        transferDataEntity: TransferDataEntity,
        rotateBounds: RectF = engraveProvider.getEngraveRotateBounds()
    ) {
        transferDataEntity.taskId = transferConfigEntity.taskId

        val mmValueUnit = MM_UNIT
        transferDataEntity.dpi = transferConfigEntity.dpi
        transferDataEntity.name = transferConfigEntity.name

        //雕刻数据坐标
        if (transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_GCODE) {
            //mm单位
            transferDataEntity.x = (mmValueUnit.convertPixelToValue(rotateBounds.left) * 10).toInt()
            transferDataEntity.y = (mmValueUnit.convertPixelToValue(rotateBounds.top) * 10).toInt()
            transferDataEntity.width =
                (mmValueUnit.convertPixelToValue(rotateBounds.width()) * 10).toInt()
            transferDataEntity.height =
                (mmValueUnit.convertPixelToValue(rotateBounds.height()) * 10).toInt()
        } else {
            //px单位
            transferDataEntity.x = rotateBounds.left.toInt()
            transferDataEntity.y = rotateBounds.top.toInt()
            transferDataEntity.width = rotateBounds.width().toInt()
            transferDataEntity.height = rotateBounds.height().toInt()
        }

        if (transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING ||
            transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_BITMAP ||
            transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_BITMAP_PATH
        ) {
            //抖动的数据
            val rect = EngravePreviewCmd.adjustRectRange(
                rotateBounds,
                transferConfigEntity.dpi
            ).resultRect!!
            transferDataEntity.x = rect.left
            transferDataEntity.y = rect.top
            transferDataEntity.width = rect.width()
            transferDataEntity.height = rect.height()
        }

        //图层模式赋值, 和数据模式本质是一样的, 外部可以修改赋值
        //[com.angcyo.engrave.data.EngraveLayerInfo]
        engraveProvider.getEngraveDataItem()?.dataBean?._dataMode?.let {
            transferDataEntity.layerMode = it
        }
    }

    //endregion---private---
}