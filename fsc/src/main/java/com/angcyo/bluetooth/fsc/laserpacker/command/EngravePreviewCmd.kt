package com.angcyo.bluetooth.fsc.laserpacker.command

import android.graphics.Rect
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.ProductInfo
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexInt
import com.angcyo.library.ex.toHexString

/**
 * 雕刻/打印预览指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
data class EngravePreviewCmd(
    //0x01 预览flash内存中的图片，name内容为将预览文件名称。
    //0x02 表示范围预览，name内容为长宽。
    //0x03 结束预览打印。
    //0x04 第三轴暂停预览。
    //0x05 第三轴继续预览。
    //0x06 电动支架升降控制指令，data1为升降方向 0为下，1为上，2为停止，data2高8位，data3低8位为升降步数。
    //  L1-Z新增功能：
    //  Data4为功能码：0x01为手动调焦，0x02为自动调焦，0x03为开关机指令。
    //  data3，data2为升降步数0x00，此处没有用到。
    //  当Data4功能码为0x01手动调焦时，Data1为按键码：
    //  0x00为手动单击向下调焦；0x01为手动单击向上调焦；
    //  0x02为手动长按向上调焦；0x03为手动长按向下调焦；0x04为停止移动。
    //0x07 显示中心点
    val state: Byte,
    //val name: Int = 0x0,//为将打印文件名。 文件编号 4字节
    var d1: Byte = 0,
    var d2: Byte = 0,
    var d3: Byte = 0,
    var d4: Byte = 0,
    var x: Int = 0x0, //当State = 0x02时 X,Y表示预览的起始坐标。 2字节
    var y: Int = 0x0,
    var custom: Byte = 0x0,
    //当PX为以下值时对应图片分辨率：
    //PX = 0x05 时 图片分辨率为800*800
    //PX = 0x04 时 图片分辨率为1000*1000
    //PX = 0x03 时 图片分辨率为1300*1300
    //PX = 0x02 时 图片分辨率为2000*2000
    //PX = 0x01 时 图片分辨率为4000*4000
    var px: Byte = 0x04,
    //为预览光功率设置，data11为功率档位，范围为1 - 10。
    var pwr: Byte = 0x1,
) : ICommand {

    companion object {

        /**预览flash内存中的图片
         * [name] 文件编号*/
        fun previewFlashBitmap(name: Int): EngravePreviewCmd {
            val bytes = name.toHexString(8).toHexByteArray()
            return EngravePreviewCmd(0x01).apply {
                d1 = bytes[0]
                d2 = bytes[1]
                d3 = bytes[2]
                d4 = bytes[3]
            }
        }

        /**表示范围预览*/
        fun previewRange(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            px: Byte = 0x04,
            productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd {
            var previewX = x
            var previewY = y

            var previewWidth = width
            var previewHeight = height

            if (productInfo != null) {
                //设备原点在中心
                previewX = x - productInfo.bounds.left.toInt()
                previewY = y - productInfo.bounds.top.toInt()
            }

            if (previewX < 0 || previewY < 0 || width < 0 || height < 0) {
                //不支持负数预览
                //L.w("参数需要大于0")
                //return null

                //超过范围, 缩成在中心的一个点
                previewX = (productInfo?.bounds?.width()?.toInt() ?: 0) / 2
                previewY = (productInfo?.bounds?.height()?.toInt() ?: 0) / 2

                previewWidth = 1
                previewHeight = 1
            }

            previewX = LaserPeckerHelper.transformHorizontalPixel(previewX, px, productInfo)
            previewY = LaserPeckerHelper.transformVerticalPixel(previewY, px, productInfo)

            previewWidth = LaserPeckerHelper.transformHorizontalPixel(previewWidth, px, productInfo)
            previewHeight = LaserPeckerHelper.transformVerticalPixel(previewHeight, px, productInfo)

            val widthBytes = previewWidth.toHexString(4).toHexByteArray()
            val heightBytes = previewHeight.toHexString(4).toHexByteArray()
            return EngravePreviewCmd(0x02, px = px).apply {
                d1 = widthBytes[0]
                d2 = widthBytes[1]
                d3 = heightBytes[0]
                d4 = heightBytes[1]

                this.x = previewX
                this.y = previewY
            }
        }

        /**表示更新范围预览*/
        /*fun previewUpdateRange(x: Int, y: Int, width: Int, height: Int): PrintPreviewCmd {
            val widthBytes = width.toHexString(4).toHexByteArray()
            val heightBytes = height.toHexString(4).toHexByteArray()
            return PrintPreviewCmd(0x04).apply {
                d1 = widthBytes[0]
                d2 = widthBytes[1]
                d3 = heightBytes[0]
                d4 = heightBytes[1]

                this.x = x
                this.y = y
            }
        }*/

        /**电动支架升降控制指令
         * 支架升
         * [step] 步数*/
        fun previewBracketUp(step: Int = 1): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                d1 = 0x1
                val bytes = step.toHexString(4).toHexByteArray()
                d2 = bytes[0]
                d3 = bytes[1]
            }
        }

        /**支架降*/
        fun previewBracketDown(step: Int = 1): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                d1 = 0x0
                val bytes = step.toHexString(4).toHexByteArray()
                d2 = bytes[0]
                d3 = bytes[1]
            }
        }

        /**结束预览指令*/
        fun previewStop(): EngravePreviewCmd {
            return EngravePreviewCmd(0x03)
        }
    }

    override fun toHexCommandString(): String {
        val dataLength = 0x0F //数据长度
        val func = "02" //功能码
        val data = buildString {
            append(func)
            append(state.toHexString())
            when (state) {
                0x01.toByte() -> {
                    //预览flash内存中的图片，name内容为将预览文件名称。
                    val nameBytes = ByteArray(4)
                    nameBytes[0] = d1
                    nameBytes[1] = d2
                    nameBytes[2] = d3
                    nameBytes[3] = d4
                    val name = nameBytes.toHexString(false)
                    append(name)
                }
                0x02.toByte() -> {
                    //表示范围预览，name内容为长宽。
                    //先宽
                    val widthBytes = ByteArray(2)
                    widthBytes[0] = d1
                    widthBytes[1] = d2
                    //后高
                    val heightBytes = ByteArray(2)
                    heightBytes[0] = d3
                    heightBytes[1] = d4
                    val width = widthBytes.toHexString(false)
                    append(" ")
                    append(width)
                    val height = heightBytes.toHexString(false)
                    append(" ")
                    append(height)
                    L.w("预览:x:$x y:$y w:${widthBytes.toHexInt()} h:${heightBytes.toHexInt()}")
                }
                0x03.toByte() -> {
                    //结束预览
                }
                0x04.toByte() -> {
                    //第三轴暂停预览, 发送此状态, 更新x,y值. (z轴)
                }
                0x05.toByte() -> {
                    //第三轴继续预览
                }
                0x06.toByte() -> {
                    //电动支架升降控制指令
                }
                else -> {
                    append(d1.toHexString())
                    append(d2.toHexString())
                    append(d3.toHexString())
                    append(d4.toHexString())
                }
            }
            append(" ")
            append(x.toHexString(4))
            append(" ")
            append(y.toHexString(4))
            append(" ")
            append(custom.toHexString())
            append(px.toHexString())
            append(pwr.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    /**获取预览范围矩形*/
    fun getPreviewRange(): Rect {
        val rect = Rect()
        rect.left = x
        rect.top = y

        val widthBytes = ByteArray(2)
        widthBytes[0] = d1
        widthBytes[1] = d2

        val heightBytes = ByteArray(2)
        heightBytes[0] = d3
        heightBytes[1] = d4

        val w = widthBytes.toHexInt()
        val h = heightBytes.toHexInt()

        rect.right = x + w
        rect.bottom = y + h
        return rect
    }
}
