package com.angcyo.bluetooth.fsc.laserpacker.command

import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.Px
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.OverflowInfo
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.MM
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
    //val index: Int = 0x0,//为将打印文件名。 文件编号 4字节
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
    var px: Byte = LaserPeckerHelper.PX_1K,
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

        fun getLimitPath(productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): Path? {
            val peckerModel = vmApp<LaserPeckerModel>()
            val limitPath = if (peckerModel.isZOpen()) {
                productInfo?.zLimitPath
            } else if (peckerModel.isROpen()) {
                productInfo?.zLimitPath
            } else if (peckerModel.isSOpen() || peckerModel.isSRepMode()) {
                productInfo?.zLimitPath
            } else {
                productInfo?.limitPath
            }
            return limitPath
        }

        fun getBoundsPath(productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): Path? {
            val peckerModel = vmApp<LaserPeckerModel>()
            val boundsPath = if (peckerModel.isZOpen()) {
                productInfo?.zLimitPath
            } else if (peckerModel.isROpen()) {
                productInfo?.zLimitPath
            } else if (peckerModel.isSOpen() || peckerModel.isSRepMode()) {
                productInfo?.zLimitPath
            } else if (productInfo != null) {
                Path().apply {
                    addRect(productInfo.bounds, Path.Direction.CW)
                }
            } else {
                null
            }
            return boundsPath
        }

        /**预览flash内存中的图片
         * [index] 文件索引*/
        fun previewFlashBitmapCmd(index: Int, pwrProgress: Float): EngravePreviewCmd {
            val bytes = index.toHexString(8).toHexByteArray()
            return EngravePreviewCmd(0x01).apply {
                d1 = bytes[0]
                d2 = bytes[1]
                d3 = bytes[2]
                d4 = bytes[3]

                updatePWR(pwrProgress)
            }
        }

        /**[adjustRectRange]*/
        fun adjustRectRange(
            rect: RectF?,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): OverflowInfo {
            rect ?: return OverflowInfo()
            return adjustRectRange(
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                dpi,
                productInfo
            )
        }

        /**修正预览的范围, 返回的是[px]调整过后的坐标
         * 会包含是否越界标识
         * [x] [y] 在画布中的坐标 像素
         * [width] [height] 在画布中的宽高 像素
         * */
        fun adjustRectRange(
            @Px x: Float, @Px y: Float, @Px width: Float, @Px height: Float,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? =
                vmApp<LaserPeckerModel>().productInfoData.value
        ): OverflowInfo {
            val tempRect = acquireTempRectF()
            var overflowBounds = false
            var overflowLimit = false
            if (productInfo != null) {
                tempRect.set(x, y, x + width, y + height)

                //
                overflowBounds = getBoundsPath(productInfo)?.overflow(tempRect) == true

                //
                //溢出
                overflowLimit = getLimitPath(productInfo)?.overflow(tempRect) == true
            }

            var previewX: Int = x.toInt()
            var previewY: Int = y.toInt()

            var previewWidth: Int = width.toInt()
            var previewHeight: Int = height.toInt()

            val pxInfo = LaserPeckerHelper.findPxInfo(dpi)
            if (overflowBounds) {
                //预览超出了设备物理范围, 缩成设备物理中心点

                //超过范围, 缩成在中心的一个点
                previewX = (productInfo?.bounds?.width()?.toInt() ?: 0) / 2
                previewY = (productInfo?.bounds?.height()?.toInt() ?: 0) / 2

                //平移
                previewX = pxInfo.transformX(previewX)
                previewY = pxInfo.transformY(previewY)

                previewWidth = 1
                previewHeight = 1
            } else {
                previewX = pxInfo.transformX(previewX)
                previewY = pxInfo.transformY(previewY)

                previewWidth = pxInfo.transformWidth(previewWidth)
                previewHeight = pxInfo.transformHeight(previewHeight)
            }

            tempRect.release()
            return OverflowInfo(
                Rect(
                    previewX,
                    previewY,
                    previewX + previewWidth,
                    previewY + previewHeight
                ), null, overflowBounds, overflowLimit
            )
        }

        /**调整4点坐标, 并标识是否溢出*/
        fun adjustFourPoint(
            rectPoint: RectPointF,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): OverflowInfo {
            var overflowLimit = false
            var overflowBounds = false
            if (productInfo != null) {

                val rectPath = rectPoint.toPath()
                overflowLimit = getLimitPath(productInfo)?.overflow(rectPath) == true
                overflowBounds = getBoundsPath(productInfo)?.overflow(rectPath) == true

                /*val limitPath = getLimitPath(productInfo)
                if (limitPath != null) {
                    if (!limitPath.contains(rectPoint.toPath())) {
                        //溢出
                        overflowLimit = true
                    }
                }

                val boundsPath = getBoundsPath(productInfo)
                if (boundsPath != null) {
                    if (!boundsPath.contains(rectPoint.toPath())) {
                        //溢出
                        overflowBounds = true
                    }
                }*/
            }

            val result = RectPointF(originRotate = rectPoint.originRotate)
            result.originRectF.set(rectPoint.originRectF)

            val pxInfo = LaserPeckerHelper.findPxInfo(dpi)
            result.leftTop.x = pxInfo.transformX(rectPoint.leftTop.x.toInt()).toFloat()
            result.leftBottom.x = pxInfo.transformX(rectPoint.leftBottom.x.toInt()).toFloat()
            result.rightTop.x = pxInfo.transformX(rectPoint.rightTop.x.toInt()).toFloat()
            result.rightBottom.x = pxInfo.transformX(rectPoint.rightBottom.x.toInt()).toFloat()

            result.leftTop.y = pxInfo.transformY(rectPoint.leftTop.y.toInt()).toFloat()
            result.leftBottom.y = pxInfo.transformY(rectPoint.leftBottom.y.toInt()).toFloat()
            result.rightTop.y = pxInfo.transformY(rectPoint.rightTop.y.toInt()).toFloat()
            result.rightBottom.y = pxInfo.transformY(rectPoint.rightBottom.y.toInt()).toFloat()

            return OverflowInfo(null, result, overflowBounds, overflowLimit)
        }

        /**根据[px]和[productInfo]调整预览的范围
         * [pwrProgress] [0~1f] 预览光功率
         * [diameter] 物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
         * */
        fun adjustPreviewRangeCmd(
            @Px x: Int, @Px y: Int, @Px width: Int, @Px height: Int,
            pwrProgress: Float,
            @MM
            diameter: Int,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd? {
            val overflowInfo = adjustRectRange(
                x.toFloat(),
                y.toFloat(),
                width.toFloat(),
                height.toFloat(),
                dpi,
                productInfo
            )
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)
            if (overflowInfo.isOverflowBounds) {
                //超出物理范围, 不发送指令
                return null
            }
            return _previewRangeCmd(overflowInfo.resultRect!!, pwrProgress, diameter, dpi)
        }

        /**表示范围预览
         * [rect] 经过[px]处理过的像素*/
        fun _previewRangeCmd(
            rect: Rect,
            pwrProgress: Float,
            @MM diameter: Int,
            dpi: Float
        ): EngravePreviewCmd {
            return _previewRangeCmd(
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                pwrProgress,
                diameter,
                dpi
            )
        }

        fun _previewRangeCmd(
            @Px x: Int, @Px y: Int, @Px width: Int, @Px height: Int,
            pwrProgress: Float,
            @MM
            diameter: Int,  //物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
            dpi: Float = LaserPeckerHelper.DPI_254,
        ): EngravePreviewCmd {
            val widthBytes = width.toHexString(4).toHexByteArray()
            val heightBytes = height.toHexString(4).toHexByteArray()

            return EngravePreviewCmd(0x02).apply {
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
        fun adjustPreviewFourPointCmd(
            rectPoint: RectPointF,
            pwrProgress: Float,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd? {
            val overflowInfo = adjustFourPoint(rectPoint, dpi, productInfo)
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)

            if (overflowInfo.isOverflowBounds) {
                //溢出了, 预览一个点
                /*return _previewRangeCmd(
                    overflowInfo.resultRectPoint!!.originRectF.centerX().toInt(),
                    overflowInfo.resultRectPoint!!.originRectF.centerY().toInt(),
                    1, 1, pwrProgress, 0, dpi
                )*/
                //超出物理范围, 不发送指令
                return null
            }

            //注意4点顺序
            val resultRectPoint = overflowInfo.resultRectPoint ?: return null
            val x1 = resultRectPoint.leftTop
            val x2 = resultRectPoint.rightTop
            val x3 = resultRectPoint.rightBottom
            val x4 = resultRectPoint.leftBottom

            val x1xBytes = x1.x.toInt().toHexString(4).toHexByteArray()
            val x1yBytes = x1.y.toInt().toHexString(4).toHexByteArray()

            return EngravePreviewCmd(0x08).apply {
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

        /**电动支架升降控制指令
         * 支架升
         * [step] 步长毫米
         * 发送:  AA BB 0F 02 06 01 00 82 00 00 00 00 00 00 04 01 00 90
         * 返回:  AA BB 08 02 06 00 01 00 00 00 09
         *       AA BB 08 02 06 00 00 00 00 00 08
         * */
        fun previewBracketUpCmd(@MM step: Int = 65535): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                if (vmApp<LaserPeckerModel>().productInfoData.value?.isLI_Z() == true) {
                    d1 = 0x03 //0x01 //0x02
                    d4 = 0x01
                } else {
                    d1 = 0x01
                    val bytes = (step * 10).toHexString(4).toHexByteArray()//2022-10-22 *10保留精度
                    d2 = bytes[0]
                    d3 = bytes[1]
                }
            }
        }

        /**支架降
         * [step] 步长1mm*/
        fun previewBracketDownCmd(@MM step: Int = 65535): EngravePreviewCmd {
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
        fun previewBracketStopCmd(): EngravePreviewCmd {
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
         * [bounds] C1使用的参数
         * */
        fun previewShowCenterCmd(pwrProgress: Float, bounds: RectF): EngravePreviewCmd? {
            val overflowInfo = adjustRectRange(
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height(),
                LaserPeckerHelper.DPI_254,
                vmApp<LaserPeckerModel>().productInfoData.value
            )
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)
            if (overflowInfo.isOverflowBounds) {
                return null
            }
            val rect = overflowInfo.resultRect!!
            return EngravePreviewCmd(0x07).apply {

                val widthBytes = rect.width().toHexString(4).toHexByteArray()
                val heightBytes = rect.height().toHexString(4).toHexByteArray()

                d1 = widthBytes[0]
                d2 = widthBytes[1]
                d3 = heightBytes[0]
                d4 = heightBytes[1]

                x = rect.left
                y = rect.top

                updatePWR(pwrProgress)
            }
        }

        /**结束预览指令*/
        fun previewStopCmd(): EngravePreviewCmd {
            return EngravePreviewCmd(0x03)
        }

        /**第三轴暂停预览*/
        fun adjustPreviewZRangeCmd(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            pwrProgress: Float,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd? {
            val overflowInfo = adjustRectRange(
                x.toFloat(),
                y.toFloat(),
                width.toFloat(),
                height.toFloat(),
                dpi,
                productInfo
            )
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)
            if (overflowInfo.isOverflowBounds) {
                //超出物理范围, 不发送指令
                return null
            }
            return _previewZRangeCmd(overflowInfo.resultRect!!, pwrProgress, dpi)
        }

        /**第三轴暂停预览, 用来拖动时更新x,t
         * [rect] 最终调整过后的坐标
         * [pwrProgress] [0~1f] 预览光功率
         * */
        fun _previewZRangeCmd(
            rect: Rect,
            pwrProgress: Float,
            dpi: Float = LaserPeckerHelper.DPI_254
        ): EngravePreviewCmd {
            return EngravePreviewCmd(0x04).apply {
                x = rect.left
                y = rect.top
                updatePWR(pwrProgress)
            }
        }

        /**第三轴继续预览指令, z轴滚动预览*/
        fun previewZContinueCmd(): EngravePreviewCmd {
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
                0x02.toByte(), 0x07.toByte() /*中心点预览*/ -> {
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
            0x07.toByte() -> append("显示中心:${getPreviewRange()}")
            0x08.toByte() -> {
                append(" 4点预览")
                val rectPoint = getFourPoint()
                append(" lt:${rectPoint.leftTop} rt:${rectPoint.rightTop} rb:${rectPoint.rightBottom} lb:${rectPoint.leftBottom}")
            }
            else -> append("Unknown")
        }
        append(" x:$x y:$y px:$px pwr:$pwr diameter:${diameter}")
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
