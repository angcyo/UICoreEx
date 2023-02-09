package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.graphics.IEngraveProvider
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.component.file.appFilePath
import com.angcyo.core.component.file.writeTo
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Private
import com.angcyo.library.ex.ceil
import com.angcyo.library.ex.ensureExtName
import com.angcyo.library.ex.floor
import com.angcyo.library.unit.IValueUnit.Companion.MM_UNIT
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

        /**存放元素预览图的扩展名*/
        const val EXT_PREVIEW = ".png"

        /**存放元素转成数据后, 数据再次预览图的扩展名*/
        const val EXT_DATA_PREVIEW = ".p.png"

        /**图片路径数据*/
        const val EXT_BP = ".bp"

        /**抖动数据*/
        const val EXT_DT = ".dt"

        /**gcode数据*/
        const val EXT_GCODE = ".gcode"

        /**保存雕刻数据到文件
         * [index] 需要保存的文件名(雕刻索引), 无扩展
         * [suffix] 文件后缀, 扩展名
         * [data]
         *   [String]
         *   [ByteArray]
         *   [Bitmap]
         *   [File]
         * ]*/
        fun saveEngraveData(
            index: Any?,
            data: FileTextData?,
            suffix: String = "engrave",
            recycle: Boolean = false,
        ): String? {
            //将雕刻数据写入文件
            return data.writeTo(
                CanvasConstant.ENGRAVE_FILE_FOLDER,
                "${index}${suffix.ensureExtName()}",
                false,
                recycle
            )
        }

        /**通过雕刻索引, 获取对应的元素预览图片文件路径*/
        fun getEngravePreviewBitmapPath(index: Any?): String = appFilePath(
            "${index}${EXT_PREVIEW.ensureExtName()}",
            CanvasConstant.ENGRAVE_FILE_FOLDER
        )

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

    /**创建一个传输的数据[TransferDataEntity], 并进行一些初始化*/
    fun createTransferDataEntity(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity
    ) = TransferDataEntity().apply {
        initTransferDataIndex(this, engraveProvider, transferConfigEntity)
    }

    /**初始化传输数据的索引, 在构建[TransferDataEntity]之后, 尽快调用 */
    @Private
    fun initTransferDataIndex(
        transferDataEntity: TransferDataEntity,
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
    ) {
        val dataBean = engraveProvider.getEngraveDataItem()?.dataBean
        val index = dataBean?.index ?: -1
        transferDataEntity.index = if (index > 0) {
            index
        } else {
            EngraveTransitionManager.generateEngraveIndex()
        }
        dataBean?.index = transferDataEntity.index
        dataBean?.dpi = transferConfigEntity.dpi
    }

    /**一些通用配置属性初始化, 一般在数据完成之后调用*/
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

        //产品名
        vmApp<LaserPeckerModel>().productInfoData.value?.apply {
            transferDataEntity.productName = name
            transferDataEntity.deviceAddress = deviceAddress
        }

        @MM
        val originWidth = mmValueUnit.convertPixelToValue(rotateBounds.width())
        val originHeight = mmValueUnit.convertPixelToValue(rotateBounds.height())
        transferDataEntity.originX = mmValueUnit.convertPixelToValue(rotateBounds.left)
        transferDataEntity.originY = mmValueUnit.convertPixelToValue(rotateBounds.top)
        transferDataEntity.originWidth = originWidth
        transferDataEntity.originHeight = originHeight

        //雕刻数据坐标
        if (transferDataEntity.engraveDataType == DataCmd.ENGRAVE_TYPE_GCODE) {
            //mm单位
            transferDataEntity.x =
                (mmValueUnit.convertPixelToValue(rotateBounds.left) * 10).floor().toInt()
            transferDataEntity.y =
                (mmValueUnit.convertPixelToValue(rotateBounds.top) * 10).floor().toInt()
            transferDataEntity.width = (originWidth * 10).ceil().toInt()
            transferDataEntity.height = (originHeight * 10).ceil().toInt()
        } else {
            //px单位
            transferDataEntity.x = rotateBounds.left.floor().toInt()
            transferDataEntity.y = rotateBounds.top.floor().toInt()
            transferDataEntity.width = rotateBounds.width().ceil().toInt()
            transferDataEntity.height = rotateBounds.height().ceil().toInt()
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