package com.angcyo.engrave.transition

import android.graphics.Bitmap
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.data.ItemDataBean
import com.angcyo.canvas.data.ItemDataBean.Companion.mmUnit
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasDataHandleOperate
import com.angcyo.core.component.file.writeTo
import com.angcyo.engrave.transition.EngraveTransitionManager.Companion.generateEngraveIndex
import com.angcyo.library.utils.FileTextData
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity

/**雕刻数据转换
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
interface IEngraveTransition {

/*
    */
    /**开始转换需要准备的数据
     * 返回[null], 表示未处理
     * *//*
    fun doTransitionReadyData(renderer: BaseItemRenderer<*>): EngraveReadyInfo?

    */
    /**开始转换需要雕刻的数据
     * 返回[false], 表示未处理
     * *//*
    fun doTransitionEngraveData(
        renderer: BaseItemRenderer<*>,
        engraveReadyInfo: EngraveReadyInfo
    ): Boolean*/

    /**数据处理的模式
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE]
     * [com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING]
     * */
    fun getDataMode(bean: ItemDataBean?, transferConfigEntity: TransferConfigEntity): Int {
        return transferConfigEntity.dataMode ?: (bean?._dataMode ?: 0)
    }

    /**将[renderer]转换成传输给机器的数据*/
    fun doTransitionTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity?

    /**一些通用配置属性初始化*/
    fun initTransferDataEntity(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity,
        transferDataEntity: TransferDataEntity
    ) {
        transferDataEntity.taskId = transferConfigEntity.taskId

        val mmValueUnit = mmUnit
        val rotateBounds = renderer.getRotateBounds()
        transferDataEntity.px = transferConfigEntity.px
        transferDataEntity.name = transferConfigEntity.name

        if (transferDataEntity.index <= 0) {
            //文件索引
            transferDataEntity.index = generateEngraveIndex()
        }

        //雕刻数据坐标
        if (transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_GCODE) {
            transferDataEntity.x = (mmValueUnit.convertPixelToValue(rotateBounds.left) * 10).toInt()
            transferDataEntity.y = (mmValueUnit.convertPixelToValue(rotateBounds.top) * 10).toInt()
            transferDataEntity.width =
                (mmValueUnit.convertPixelToValue(rotateBounds.width()) * 10).toInt()
            transferDataEntity.height =
                (mmValueUnit.convertPixelToValue(rotateBounds.height()) * 10).toInt()
        } else {
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
            val rect = EngravePreviewCmd.adjustBitmapRange(
                transferDataEntity.x,
                transferDataEntity.y,
                transferDataEntity.width,
                transferDataEntity.height,
                transferConfigEntity.px
            ).first
            transferDataEntity.x = rect.left
            transferDataEntity.y = rect.top
            transferDataEntity.width = rect.width()
            transferDataEntity.height = rect.height()
        }
    }

/*
    */
    /**初始化一个雕刻数据*//*
    fun initReadyEngraveData(renderer: BaseItemRenderer<*>, engraveReadyInfo: EngraveReadyInfo) {
        //索引
        val item = renderer.getRendererRenderItem()
        var index = item?.engraveIndex
        if (index == null) {
            index = EngraveTransitionManager.generateEngraveIndex()
            item?.engraveIndex = index
        }
        //init
        if (engraveReadyInfo.engraveData == null) {
            engraveReadyInfo.engraveData = EngraveDataInfo()
        }
        //雕刻数据
        engraveReadyInfo.engraveData?.apply {
            this.dataType = engraveReadyInfo.dataType
            this.index = item?.engraveIndex
            this.name = item?.itemLayerName?.toString()
        }
    }*/

    /**保存雕刻数据到文件
     * [fileName] 需要保存的文件名, 无扩展
     * [suffix] 文件后缀, 扩展名
     * [data]
     *   [String]
     *   [ByteArray]
     *   [Bitmap]
     * ]*/
    fun saveEngraveData(fileName: Any?, data: FileTextData?, suffix: String = "engrave"): String? {
        //将雕刻数据写入文件
        return data.writeTo(
            CanvasDataHandleOperate.ENGRAVE_CACHE_FILE_FOLDER,
            "${fileName}.${suffix}",
            false
        )
    }
}