package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.Color
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.engraveColorBytes
import com.angcyo.canvas.utils.getEngraveBitmap
import com.angcyo.canvas.utils.toEngraveBitmap
import com.angcyo.engrave.data.BitmapPath
import com.angcyo.engrave.data.EngraveReadyInfo
import com.angcyo.library.component.byteWriter
import kotlin.experimental.or

/**
 * Bitmap数据转换, 什么item要处理成Bitmap数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/24
 */
class BitmapTransition : IEngraveTransition {

    companion object {

        /**[bitmap] 图片转路径数据
         * [threshold] 颜色阈值, 此值以下的色值视为黑色0
         * */
        fun handleBitmapPath(bitmap: Bitmap, threshold: Int): List<BitmapPath> {
            val result = mutableListOf<BitmapPath>()

            var lastBitmapPath: BitmapPath? = null
            var isLTR = true

            //追加一段路径
            fun appendPath(ltr: Boolean) {
                lastBitmapPath?.apply {
                    len++
                    result.add(this)
                    lastBitmapPath = null
                    isLTR = ltr
                }
            }

            val width = bitmap.width
            val height = bitmap.height

            for (y in 0 until height) {
                val ltr = isLTR
                for (x in 0 until width) {
                    //一行
                    val wIndex = if (ltr) x else width - x - 1
                    val color = bitmap.getPixel(wIndex, y)
                    val channelColor = Color.red(color)

                    if (channelColor <= threshold && color != Color.TRANSPARENT) {
                        //00, 黑色纸上雕刻, 金属不雕刻
                        if (lastBitmapPath == null) {
                            lastBitmapPath = BitmapPath(wIndex, y, 0, ltr)
                        }
                        lastBitmapPath?.apply {
                            len++
                        }
                    } else {
                        appendPath(!ltr)
                    }
                }
                //收尾
                appendPath(!ltr)
            }
            return result
        }

        /**[bitmap] 图片转抖动数据, 在黑色金属上雕刻效果正确, 在纸上雕刻时反的
         * [threshold] 颜色阈值, 此值以下的色值视为黑色0
         * 白色传1 黑色传0
         * */
        fun handleBitmapByte(bitmap: Bitmap, threshold: Int): Pair<String, ByteArray> {
            val width = bitmap.width
            val height = bitmap.height

            var byte: Byte = 0 //1个字节8位
            var bit = 0
            val logBuilder = StringBuilder()
            val bytes = byteWriter {
                for (y in 0 until height) {
                    bit = 7
                    byte = 0
                    for (x in 0 until width) {
                        //一行
                        val color = bitmap.getPixel(x, y)
                        val channelColor = Color.red(color)

                        if (channelColor <= threshold) {
                            //黑色传0, 黑色纸上雕刻, 金属不雕刻
                            //byte = byte or (0b1 shl bit)
                            logBuilder.append("0")
                        } else {
                            //白色传1
                            byte = byte or (0b1 shl bit).toByte()
                            logBuilder.append("1")
                        }
                        bit--
                        if (bit < 0) {
                            //8位
                            write(byte) //写入1个字节
                            bit = 7
                            byte = 0
                        }
                    }
                    //
                    if (bit != 7) {
                        write(byte)//写入1个字节
                    }
                    if (y != height - 1) {
                        logBuilder.appendLine()
                    }
                }
            }
            return logBuilder.toString() to bytes
        }
    }

    override fun doTransitionReadyData(renderer: BaseItemRenderer<*>): EngraveReadyInfo? {
        val item = renderer.getRendererRenderItem() ?: return null
        //走到这里的数据, 都处理成Bitmap
        val result = EngraveReadyInfo()

        //
        initReadyEngraveData(renderer, result)

        result.itemUuid = item.uuid
        result.dataType = item.dataType
        result.dataMode = item.dataMode
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
        //init
        fun initEngraveData() {
            engraveReadyInfo.engraveData?.apply {
                engraveDataType = DataCmd.ENGRAVE_TYPE_BITMAP
            }
        }

        //其他方式, 使用图片雕刻
        val bounds = renderer.getRotateBounds()
        val bitmap = renderer.getEngraveBitmap() ?: return false

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

        //bitmap转bytes
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

        //2:保存一份可视化的数据/原始数据
        //mode
        when (engraveReadyInfo.dataMode) {
            CanvasConstant.DATA_MODE_BLACK_WHITE,
            CanvasConstant.DATA_MODE_PRINT,
            CanvasConstant.DATA_MODE_SEAL -> {
                //黑白算法/版画/印章 都用路径数据传输
                engraveData.engraveDataType = DataCmd.ENGRAVE_TYPE_BITMAP_PATH

                //图片转路径数据
                val listBitmapPath = handleBitmapPath(bitmap, 128)
                engraveData.lines = listBitmapPath.size
                engraveData.data = byteWriter {
                    listBitmapPath.forEach {
                        write(it.x, 2)
                        write(it.y, 2)
                        write(it.len, 2)
                    }
                }
                //路径数据写入日志
                saveEngraveData(engraveData.index, "$listBitmapPath", "bp")
            }
            CanvasConstant.DATA_MODE_GREY -> {
                //灰度数据
                engraveData.engraveDataType = DataCmd.ENGRAVE_TYPE_BITMAP

                //雕刻的数据, 红色通道的灰度雕刻数据
                val data = bitmap.engraveColorBytes()
                engraveData.data = data
                val channelBitmap = data.toEngraveBitmap(bitmap.width, bitmap.height)
                saveEngraveData(engraveData.index, channelBitmap, "png")
            }
            //CanvasConstant.DATA_MODE_DITHERING
            else -> {
                //抖动数据使用位压缩数据传输
                //默认情况下都按照抖动数据处理, 更快的数据传输速率
                engraveData.engraveDataType = DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING

                //白色1 黑色0
                val pair = handleBitmapByte(bitmap, 128)
                engraveData.data = pair.second
                saveEngraveData(engraveData.index, pair.first, "dt")
            }
        }

        //1:保存一份byte数据
        engraveReadyInfo.dataPath = saveEngraveData(engraveData.index, engraveData.data)//数据路径

        //3:保存一份用来历史文档预览的数据
        engraveReadyInfo.previewDataPath = saveEngraveData("${engraveData.index}.p", bitmap, "png")
    }
}

/**处理模式转换成雕刻数据类型*/
fun Int.convertDataModeToEngraveType() = when (this) {
    CanvasConstant.DATA_MODE_BLACK_WHITE,
    CanvasConstant.DATA_MODE_PRINT,
    CanvasConstant.DATA_MODE_SEAL -> DataCmd.ENGRAVE_TYPE_BITMAP_PATH
    CanvasConstant.DATA_MODE_GREY -> DataCmd.ENGRAVE_TYPE_BITMAP
    CanvasConstant.DATA_MODE_GCODE -> DataCmd.ENGRAVE_TYPE_GCODE
    else -> DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING
}

/**处理模式转换成雕刻数据类型*/
fun Int.convertEngraveTypeToDataMode() = when (this) {
    DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> CanvasConstant.DATA_MODE_BLACK_WHITE
    DataCmd.ENGRAVE_TYPE_BITMAP -> CanvasConstant.DATA_MODE_GREY
    DataCmd.ENGRAVE_TYPE_GCODE -> CanvasConstant.DATA_MODE_GCODE
    else -> CanvasConstant.DATA_MODE_DITHERING
}