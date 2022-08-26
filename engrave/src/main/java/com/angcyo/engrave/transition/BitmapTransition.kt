package com.angcyo.engrave.transition

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.engraveColorBytes
import com.angcyo.canvas.utils.toEngraveBitmap
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveReadyInfo
import com.angcyo.library.ex.toBitmap

/**
 * Bitmap数据转换, 什么item要处理成Bitmap数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/24
 */
class BitmapTransition : IEngraveTransition {

    override fun doTransitionReadyData(renderer: BaseItemRenderer<*>): EngraveReadyInfo? {
        val item = renderer.getRendererItem() ?: return null
        //走到这里的数据, 都处理成Bitmap
        val result = EngraveReadyInfo()

        result.itemUuid = item.uuid
        result.dataType = item.dataType
        if (result.dataMode > 0) {
            result.dataMode = result.dataMode
        } else {
            result.dataMode = CanvasConstant.DATA_MODE_GREY
        }

        //px list
        result.dataSupportPxList = LaserPeckerHelper.findProductSupportPxList()

        //
        result.dataSupportModeList

        return result
    }

    override fun doTransitionEngraveData(
        renderer: BaseItemRenderer<*>,
        engraveReadyInfo: EngraveReadyInfo
    ): Boolean {
        val item = renderer.getRendererItem()

        //init
        fun initEngraveData() {
            initReadyEngraveData(renderer, engraveReadyInfo)
            engraveReadyInfo.engraveData?.apply {
                engraveDataType = EngraveDataInfo.ENGRAVE_TYPE_BITMAP
            }
        }

        //其他方式, 使用图片雕刻
        val bounds = renderer.getRotateBounds()
        val bitmap = renderer.preview()?.toBitmap() ?: return false

        //init
        initEngraveData()

        val x = bounds.left.toInt()
        val y = bounds.top.toInt()

        engraveReadyInfo.dataBitmap = bitmap
        engraveReadyInfo.dataX = x
        engraveReadyInfo.dataY = y

        //engraveReadyInfo.engraveData?.engraveDataType = EngraveDataInfo.ENGRAVE_TYPE_BITMAP
        _handleBitmapPx(
            engraveReadyInfo,
            engraveReadyInfo.engraveData?.px ?: LaserPeckerHelper.DEFAULT_PX
        )

        _handleBitmapData(engraveReadyInfo)

        return true
    }

    /**处理图片数据的坐标*/
    fun _handleBitmapPx(engraveReadyInfo: EngraveReadyInfo, px: Byte) {
        val engraveData = engraveReadyInfo.engraveData ?: return
        var bitmap = engraveReadyInfo.dataBitmap ?: return

        //先保存原始图片的宽高
        val width = bitmap.width
        val height = bitmap.height

        //根据px缩放图片
        bitmap = LaserPeckerHelper.bitmapScale(bitmap, px)
        //scale
        engraveReadyInfo.dataBitmap = bitmap

        //根据px, 修正坐标
        val x = engraveReadyInfo.dataX
        val y = engraveReadyInfo.dataY
        val rect = EngravePreviewCmd.adjustBitmapRange(x, y, width, height, px).first

        //雕刻的宽高使用图片本身的宽高, 否则如果宽高和数据不一致,会导致图片打印出来是倾斜的效果
        val engraveWidth = bitmap.width
        val engraveHeight = bitmap.height

        //雕刻数据坐标
        engraveData.x = rect.left
        engraveData.y = rect.top
        engraveData.width = engraveWidth
        engraveData.height = engraveHeight
        engraveData.px = px
    }

    /**处理图片数据*/
    fun _handleBitmapData(engraveReadyInfo: EngraveReadyInfo) {
        val engraveData = engraveReadyInfo.engraveData ?: return
        val bitmap = engraveReadyInfo.dataBitmap ?: return

        //mode
        when (engraveReadyInfo.dataMode) {
            CanvasConstant.DATA_MODE_BLACK_WHITE, CanvasConstant.DATA_MODE_DITHERING -> {

            }
            CanvasConstant.DATA_MODE_GREY -> {

            }
        }

        //雕刻的数据
        val data = bitmap.engraveColorBytes()
        engraveData.data = data

        //1:保存一份byte数据
        engraveReadyInfo.dataPath = saveEngraveData(engraveData.index, data)//数据路径

        //2:保存一份可视化的数据/原始数据
        val channelBitmap = data.toEngraveBitmap(bitmap.width, bitmap.height)
        saveEngraveData(engraveData.index, channelBitmap, "png")

        //3:保存一份用来历史文档预览的数据
        engraveReadyInfo.previewDataPath = saveEngraveData("${engraveData.index}.p", bitmap, "png")
    }
}