package com.angcyo.bluetooth.fsc.laserpacker.command

import android.graphics.Rect
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_PX
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.core.vmApp
import com.angcyo.library.component.pool.acquireTempRect
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.*
import com.angcyo.library.model.RectPointF
import com.angcyo.library.model.toPath

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
    //0x08 为4角点预览方式，上位机指定4个角点标。下机机连接4个角点做预览。
    // Data1-data4为第一个角点坐标；
    // Data5-data6为第二个角点坐标；
    // Data10-11为第三个角点坐标；
    // Data12-13为第四个角点坐标；
    val state: Byte,
    //val name: Int = 0x0,//为将打印文件名。 文件编号 4字节
    var d1: Byte = 0,
    var d2: Byte = 0,
    var d3: Byte = 0,
    var d4: Byte = 0,
    //Data5-data6为第二个角点坐标；
    var x: Int = 0x0, //当State = 0x02时 X,Y表示预览的起始坐标。 2字节
    var y: Int = 0x0, // 2字节
    var custom: Byte = 0x0,
    //当PX为以下值时对应图片分辨率：
    //PX = 0x05 时 图片分辨率为800*800
    //PX = 0x04 时 图片分辨率为1000*1000
    //PX = 0x03 时 图片分辨率为1300*1300
    //PX = 0x02 时 图片分辨率为2000*2000
    //PX = 0x01 时 图片分辨率为4000*4000
    //Data8-10为第三个角点坐标
    var px: Byte = DEFAULT_PX,
    //为预览光功率设置，data11为功率档位，范围为1 - 10。
    var pwr: Byte = 0x1,
    //开启了第三轴预览时, 这个值是物理直径
    //物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
    //Data10-11为第三个角点坐标；
    var diameter: Int = 0, // 2字节
    var d11: Int = 0, // 2字节
    //Data12-13为第四个角点坐标
    var d12: Int = 0, // 2字节
    var d13: Int = 0, // 2字节
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

        /**修正预览的范围, 返回的是[px]调整过后的坐标
         * 会包含是否越界标识
         * [x] [y] 在画布中的坐标 像素
         * [width] [height] 在画布中的宽高 像素
         * */
        fun adjustBitmapRange(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            px: Byte = DEFAULT_PX,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): Pair<Rect, Boolean> {
            val tempRect = acquireTempRectF()
            var overflow = false
            if (productInfo != null) {
                tempRect.set(
                    x.toFloat(),
                    y.toFloat(),
                    (x + width).toFloat(),
                    (y + height).toFloat()
                )
                val peckerModel = vmApp<LaserPeckerModel>()
                val limitPath = if (peckerModel.haveExDevice()) {
                    //有外接设备
                    productInfo.zLimitPath
                } else {
                    productInfo.limitPath
                }

                if (limitPath != null) {
                    if (limitPath.overflow(tempRect)) {
                        //溢出
                        overflow = true
                    }
                }
            }

            var previewX = x
            var previewY = y

            var previewWidth = width
            var previewHeight = height

            if (overflow) {
                //预览要出范围, 缩成设备物理中心点

                //超过范围, 缩成在中心的一个点
                previewX = (productInfo?.bounds?.width()?.toInt() ?: 0) / 2
                previewY = (productInfo?.bounds?.height()?.toInt() ?: 0) / 2

                //平移
                previewX = LaserPeckerHelper.transformX(previewX, px)
                previewY = LaserPeckerHelper.transformY(previewY, px)

                previewWidth = 1
                previewHeight = 1
            } else {
                previewX = LaserPeckerHelper.transformX(previewX, px)
                previewY = LaserPeckerHelper.transformY(previewY, px)

                previewWidth = LaserPeckerHelper.transformWidth(previewWidth, px)
                previewHeight = LaserPeckerHelper.transformHeight(previewHeight, px)
            }

            /*if (productInfo != null) {
                //设备原点在中心
                previewX = x - productInfo.bounds.left.toInt()
                previewY = y - productInfo.bounds.top.toInt()
            }*/

            /*
               val pxInfo = LaserPeckerHelper.findPxInfo(px)

            //是否溢出
            var overflow = false
            if (previewX < 0 || previewY < 0 || previewWidth < 0 || previewHeight < 0) {
                overflow = true
            }

            if (!overflow) {
                if (pxInfo != null) {
                    if (previewX + previewWidth > pxInfo.pxWidth) {
                        overflow = true
                    }
                    if (!vmApp<LaserPeckerModel>().isZOpen() && previewY + previewHeight > pxInfo.pxHeight) {
                        //Z轴没有打开的情况下, 才限制高度
                        overflow = true
                    }
                }
            }*/

            tempRect.release()
            return Rect(
                previewX,
                previewY,
                previewX + previewWidth,
                previewY + previewHeight
            ) to overflow
        }

        /**调整4点坐标, 并标识是否溢出*/
        fun adjustFourPoint(
            rectPoint: RectPointF,
            px: Byte = DEFAULT_PX,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): Pair<RectPointF, Boolean> {
            var overflow = false
            if (productInfo != null) {
                val peckerModel = vmApp<LaserPeckerModel>()
                val limitPath = if (peckerModel.haveExDevice()) {
                    //有外接设备
                    productInfo.zLimitPath
                } else {
                    productInfo.limitPath
                }

                if (limitPath != null) {
                    if (!limitPath.contains(rectPoint.toPath())) {
                        //溢出
                        overflow = true
                    }
                }
            }

            val result = RectPointF(originRotate = rectPoint.originRotate)
            result.originRectF.set(rectPoint.originRectF)

            result.leftTop.x =
                LaserPeckerHelper.transformX(rectPoint.leftTop.x.toInt(), px).toFloat()
            result.leftBottom.x =
                LaserPeckerHelper.transformX(rectPoint.leftBottom.x.toInt(), px).toFloat()
            result.rightTop.x =
                LaserPeckerHelper.transformX(rectPoint.rightTop.x.toInt(), px).toFloat()
            result.rightBottom.x =
                LaserPeckerHelper.transformX(rectPoint.rightBottom.x.toInt(), px).toFloat()

            result.leftTop.y =
                LaserPeckerHelper.transformY(rectPoint.leftTop.y.toInt(), px).toFloat()
            result.leftBottom.y =
                LaserPeckerHelper.transformY(rectPoint.leftBottom.y.toInt(), px).toFloat()
            result.rightTop.y =
                LaserPeckerHelper.transformY(rectPoint.rightTop.y.toInt(), px).toFloat()
            result.rightBottom.y =
                LaserPeckerHelper.transformY(rectPoint.rightBottom.y.toInt(), px).toFloat()

            return result to overflow
        }

        /**根据[px]和[productInfo]调整预览的范围
         * [pwrProgress] [0~1f] 预览光功率
         * [diameter] 物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
         * */
        fun previewRange(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            pwrProgress: Float,
            diameter: Int,
            px: Byte = DEFAULT_PX,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd {
            val pair = adjustBitmapRange(x, y, width, height, px, productInfo)
            vmApp<LaserPeckerModel>().overflowRectData.postValue(pair.second)
            return previewRangeCmd(pair.first, pwrProgress, diameter, px)
        }

        /**表示范围预览
         * [rect] 经过[px]处理过的像素*/
        fun previewRangeCmd(
            rect: Rect,
            pwrProgress: Float,
            diameter: Int,
            px: Byte = DEFAULT_PX
        ): EngravePreviewCmd {
            return previewRangeCmd(
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                pwrProgress,
                diameter,
                px
            )
        }

        fun previewRangeCmd(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            pwrProgress: Float,
            diameter: Int,  //物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
            px: Byte = DEFAULT_PX
        ): EngravePreviewCmd {
            val widthBytes = width.toHexString(4).toHexByteArray()
            val heightBytes = height.toHexString(4).toHexByteArray()

            return EngravePreviewCmd(0x02, px = px).apply {
                d1 = widthBytes[0]
                d2 = widthBytes[1]
                d3 = heightBytes[0]
                d4 = heightBytes[1]

                this.x = x
                this.y = y

                this.diameter = diameter

                updatePWR(pwrProgress)
            }
        }

        /**表示范围预览, 4点预览
         * [rect] 经过[px]处理过的像素*/
        fun previewFourPoint(
            rectPoint: RectPointF,
            pwrProgress: Float,
            px: Byte = DEFAULT_PX,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd {
            val pair = adjustFourPoint(rectPoint, px, productInfo)
            vmApp<LaserPeckerModel>().overflowRectData.postValue(pair.second)

            if (pair.second) {
                //溢出了, 预览一个点
                return previewRangeCmd(
                    pair.first.originRectF.centerX().toInt(),
                    pair.first.originRectF.centerY().toInt(),
                    1, 1, pwrProgress, 0, px
                )
            }

            //注意4点顺序
            val x1 = rectPoint.leftTop
            val x2 = rectPoint.rightTop
            val x3 = rectPoint.rightBottom
            val x4 = rectPoint.leftBottom

            val x1xBytes = x1.x.toInt().toHexString(4).toHexByteArray()
            val x1yBytes = x1.y.toInt().toHexString(4).toHexByteArray()

            return EngravePreviewCmd(0x08, px = px).apply {
                //第1个点
                d1 = x1xBytes[0]
                d2 = x1xBytes[1]
                d3 = x1yBytes[0]
                d4 = x1yBytes[1]

                //第2个点
                x = x2.x.toInt()
                y = x2.y.toInt()

                //第3个点
                diameter = x3.x.toInt()
                d11 = x3.y.toInt()

                //第4个点
                d12 = x4.x.toInt()
                d13 = x4.y.toInt()

                updatePWR(pwrProgress)
            }
        }

        /**表示更新范围预览*/
        /*fun previewUpdateRange(x: Int, y: Int, width: Int, height: Int): PrintPreviewCmd {
            val widthBytes = width.toHexString(4).toHexByteArray()
            val heightBytes = height.toHexString(4).toHexByteArray()
            return PrintPreviewCmd(DEFAULT_PX).apply {
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
         * [step] 步长毫米
         * 发送:  AA BB 0F 02 06 01 00 82 00 00 00 00 00 00 04 01 00 90
         * 返回:  AA BB 08 02 06 00 01 00 00 00 09
         *       AA BB 08 02 06 00 00 00 00 00 08
         * */
        fun previewBracketUp(step: Int = 65535): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                if (vmApp<LaserPeckerModel>().productInfoData.value?.isLI_Z() == true) {
                    d1 = 0x03 //0x01 //0x02
                    d4 = 0x01
                } else {
                    d1 = 0x01
                    val bytes = step.toHexString(4).toHexByteArray()
                    d2 = bytes[0]
                    d3 = bytes[1]
                }
            }
        }

        /**支架降
         * [step] 步长1mm*/
        fun previewBracketDown(step: Int = 65535): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                if (vmApp<LaserPeckerModel>().productInfoData.value?.isLI_Z() == true) {
                    d1 = 0x02 //0x00 //0x03
                    d4 = 0x01
                } else {
                    d1 = 0x00
                    val bytes = step.toHexString(4).toHexByteArray()
                    d2 = bytes[0]
                    d3 = bytes[1]
                }
            }
        }

        /**停止支架*/
        fun previewBracketStop(): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                if (vmApp<LaserPeckerModel>().productInfoData.value?.isLI_Z() == true) {
                    d1 = 0x04
                    d4 = 0x01
                } else {
                    d1 = 0x02
                }
            }
        }

        /**显示中心, 中心点预览
         * [pwrProgress] [0~1f] 预览光功率
         * */
        fun previewShowCenter(pwrProgress: Float): EngravePreviewCmd {
            return EngravePreviewCmd(0x07).apply {
                updatePWR(pwrProgress)
            }
        }

        /**结束预览指令*/
        fun previewStop(): EngravePreviewCmd {
            return EngravePreviewCmd(0x03)
        }

        /**第三轴暂停预览*/
        fun previewZRange(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            pwrProgress: Float,
            px: Byte = DEFAULT_PX,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd {
            val pair = adjustBitmapRange(x, y, width, height, px, productInfo)
            vmApp<LaserPeckerModel>().overflowRectData.postValue(pair.second)
            return previewZRange(pair.first, pwrProgress, px)
        }

        /**第三轴暂停预览, 用来拖动时更新x,t
         * [rect] 最终调整过后的坐标
         * [pwrProgress] [0~1f] 预览光功率
         * */
        fun previewZRange(
            rect: Rect,
            pwrProgress: Float,
            px: Byte = DEFAULT_PX
        ): EngravePreviewCmd {
            return EngravePreviewCmd(0x04, px = px).apply {
                x = rect.left
                y = rect.top
                updatePWR(pwrProgress)
            }
        }

        /**第三轴继续预览指令, z轴滚动预览*/
        fun previewZContinue(): EngravePreviewCmd {
            return EngravePreviewCmd(0x05)
        }
    }

    //功能码
    override fun commandFunc(): Byte = 0x02

    override fun toHexCommandString(): String {
        val dataLength = 0x17 //数据长度
        val data = buildString {
            append(commandFunc().toHexString())
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
                }
                /*0x03.toByte() -> {
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
                }*/
                0x08.toByte() -> {
                    //4点预览, 第一个点的坐标
                    //先x
                    val xBytes = ByteArray(2)
                    xBytes[0] = d1
                    xBytes[1] = d2
                    //后y
                    val yBytes = ByteArray(2)
                    yBytes[0] = d3
                    yBytes[1] = d4
                    val x = xBytes.toHexString(false)
                    append(" ")
                    append(x)
                    val y = yBytes.toHexString(false)
                    append(" ")
                    append(y)
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
            append(diameter.toHexString(4))
            append(d11.toHexString(4))
            append(d12.toHexString(4))
            append(d13.toHexString(4))
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        append(" 打印预览:")
        when (state) {
            0x01.toByte() -> {
                append("图片预览")
                val nameBytes = ByteArray(4)
                nameBytes[0] = d1
                nameBytes[1] = d2
                nameBytes[2] = d3
                nameBytes[3] = d4
                append(" ${nameBytes.toHexInt()}")
            }
            0x02.toByte() -> {
                append("范围预览")
                val rect = getPreviewRange()
                append(" $rect 直径:$diameter")
            }
            0x03.toByte() -> append("结束预览")
            0x04.toByte() -> append("第三轴暂停预览")
            0x05.toByte() -> append("第三轴继续预览")
            0x06.toByte() -> {
                if (vmApp<LaserPeckerModel>().productInfoData.value?.isLI_Z() == true) {
                    append("LI-Z")
                    when (d4) {
                        0x01.toByte() -> append(" 手动调焦")
                        0x02.toByte() -> append(" 自动调焦")
                        0x03.toByte() -> append(" 开关机")
                    }
                    when (d1) {
                        0x00.toByte() -> append(" 单击向下调焦")
                        0x01.toByte() -> append(" 单击向上调焦")
                        0x02.toByte() -> append(" 长按向上调焦")
                        0x03.toByte() -> append(" 长按向下调焦")
                        0x04.toByte() -> append(" 停止移动")
                    }
                } else {
                    val stepBytes = ByteArray(2)
                    stepBytes[0] = d2
                    stepBytes[1] = d3
                    when (d1) {
                        0x00.toByte() -> append("降支架:${stepBytes.toHexInt()}")
                        0x01.toByte() -> append("升支架:${stepBytes.toHexInt()}")
                        0x02.toByte() -> append("停止支架")
                        else -> append("未知支架控制:$state")
                    }
                }
            }
            0x07.toByte() -> append("显示中心")
            0x08.toByte() -> {
                append(" 4点预览")
                val rectPoint = getFourPoint()
                append(" lt:${rectPoint.leftTop} rt:${rectPoint.rightTop} rb:${rectPoint.rightBottom} lb:${rectPoint.leftBottom}")
            }
            else -> append("Unknown")
        }
        append(" x:$x y:$y px:$px pwr:$pwr")
    }

    /**获取预览范围矩形*/
    fun getPreviewRange(result: Rect = acquireTempRect()): Rect {
        result.left = x
        result.top = y

        val widthBytes = ByteArray(2)
        widthBytes[0] = d1
        widthBytes[1] = d2

        val heightBytes = ByteArray(2)
        heightBytes[0] = d3
        heightBytes[1] = d4

        val w = widthBytes.toHexInt()
        val h = heightBytes.toHexInt()

        result.right = x + w
        result.bottom = y + h
        return result
    }

    /**获取4点预览的值*/
    fun getFourPoint(): RectPointF {
        val result = RectPointF()

        val bytes = ByteArray(2)

        //1
        bytes[0] = d1
        bytes[1] = d2
        result.leftTop.x = bytes.toHexInt().toFloat()

        bytes[0] = d3
        bytes[1] = d4
        result.leftTop.y = bytes.toHexInt().toFloat()

        //2
        result.rightTop.x = x.toFloat()
        result.rightTop.y = y.toFloat()

        //3
        result.rightBottom.x = diameter.toFloat()
        result.rightBottom.y = d11.toFloat()

        //4
        result.leftBottom.x = d12.toFloat()
        result.leftBottom.y = d13.toFloat()

        return result
    }

    /**[progress] [0~1f]
     * */
    fun updatePWR(progress: Float) {
        pwr = (1 + 9 * progress).toInt().toByte()
    }
}
