package com.angcyo.engrave.transition

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.canvas.utils.engraveColorBytes
import com.angcyo.canvas.utils.getEngraveBitmap
import com.angcyo.canvas.utils.toEngraveBitmap
import com.angcyo.engrave.data.BitmapPath
import com.angcyo.library.component.byteWriter
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.entity.toTransferData
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

        /**反向转成图片*/
        fun List<BitmapPath>.toEngraveBitmap(width: Int, height: Int): Bitmap {
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.FILL
                strokeWidth = 1f
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            forEach { bp ->
                val y: Float = bp.y.toFloat()
                val left: Float
                val right: Float
                if (bp.ltr) {
                    //左到右
                    left = bp.x.toFloat()
                    right = left + bp.len
                } else {
                    //右到左
                    right = bp.x.toFloat()
                    left = right - bp.len
                }
                canvas.drawLine(left, y, right, y, paint)
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

        /**将抖动数据 00011110001010 描述字符串, 转换成可视化图片*/
        fun String.toEngraveDitheringBitmap(width: Int, height: Int): Bitmap {
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.FILL
                strokeWidth = 1f
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            var x = 0f
            var y = 0f
            lines().forEach { line ->
                x = 0f
                line.forEach { char ->
                    if (char == '1') {
                        //1绘制
                        canvas.drawCircle(x, y, 1f, paint)//绘制圆点
                    }
                    x++
                }
                y++
            }
            return result
        }
    }

    /**将可视化数据处理成机器需要的线段数据或抖动数据
     * [CanvasConstant.DATA_MODE_BLACK_WHITE] 线段数据
     * [CanvasConstant.DATA_MODE_DITHERING] 抖动数据
     * */
    override fun doTransitionTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        if (renderer is DataItemRenderer) {
            val dataItem = renderer.dataItem
            val dataBean = dataItem?.dataBean
            if (dataBean != null) {
                val bitmap = renderer.getEngraveBitmap()
                if (bitmap != null) {
                    val dataMode = getDataMode(dataBean, transferConfigEntity)
                    val pxBitmap = LaserPeckerHelper.bitmapScale(bitmap, transferConfigEntity.px)
                    val transferDataEntity =
                        TransferDataEntity(index = EngraveTransitionManager.generateEngraveIndex())

                    //1:保存一份原始可视化数据
                    saveEngraveData("${transferDataEntity.index}", pxBitmap, "png")

                    when (dataMode) {
                        //黑白数据, 发送线段数据
                        CanvasConstant.DATA_MODE_BLACK_WHITE -> {
                            transferDataEntity.engraveDataType = DataCmd.ENGRAVE_TYPE_BITMAP_PATH
                            val listBitmapPath = handleBitmapPath(pxBitmap, 128)
                            val bytes = byteWriter {
                                listBitmapPath.forEach {
                                    write(it.x, 2)
                                    write(it.y, 2)
                                    write(it.len, 2)
                                }
                            }
                            transferDataEntity.data = bytes.toTransferData()
                            transferDataEntity.lines = listBitmapPath.size

                            //2:路径数据写入日志
                            saveEngraveData(transferDataEntity.index, "$listBitmapPath", "bp")
                            //3:保存一份数据的预览图
                            val previewBitmap =
                                listBitmapPath.toEngraveBitmap(pxBitmap.width, pxBitmap.height)
                            saveEngraveData("${transferDataEntity.index}.p", previewBitmap, "png")
                        }
                        //色阶数据, 红色通道的灰度雕刻数据
                        CanvasConstant.DATA_MODE_GREY -> {
                            transferDataEntity.engraveDataType = DataCmd.ENGRAVE_TYPE_BITMAP
                            val data = pxBitmap.engraveColorBytes()
                            transferDataEntity.data = data.toTransferData()
                            val previewBitmap =
                                data.toEngraveBitmap(pxBitmap.width, pxBitmap.height)
                            //3:数据的预览图片
                            saveEngraveData("${transferDataEntity.index}.p", previewBitmap, "png")
                        }
                        //抖动数据
                        else -> {
                            transferDataEntity.engraveDataType =
                                DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING
                            //白色1 黑色0
                            val pair = handleBitmapByte(pxBitmap, 128)
                            transferDataEntity.data = pair.second.toTransferData()

                            //路径数据写入日志
                            saveEngraveData(transferDataEntity.index, pair.first, "dt")
                            //3:保存一份数据的预览图
                            val previewBitmap =
                                pair.first.toEngraveDitheringBitmap(pxBitmap.width, pxBitmap.height)
                            saveEngraveData("${transferDataEntity.index}.p", previewBitmap, "png")
                        }
                    }

                    initTransferDataEntity(renderer, transferConfigEntity, transferDataEntity)
                    //注意这里要覆盖宽/高
                    transferDataEntity.width = pxBitmap.width
                    transferDataEntity.height = pxBitmap.height

                    return transferDataEntity
                }
            }
        }
        return null
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