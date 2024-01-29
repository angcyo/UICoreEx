package com.angcyo.bluetooth.fsc.laserpacker.command

import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.Px
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.OverflowInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.isOverflowBounds
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.pool.acquireTempRect
import com.angcyo.library.component.pool.acquireTempRectF
import com.angcyo.library.component.pool.release
import com.angcyo.library.ex.*
import com.angcyo.library.model.RectPointF
import com.angcyo.library.model.toPath
import com.angcyo.library.unit.toMm

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
    //0x0B 文件名预览
    var fileName: String? = null,
    //0x0C 多矩形范围预览
    @Pixel
    var rectList: List<RectF>? = null,
) : BaseCommand() {

    companion object {

        /**指令*/
        const val ENGRAVE_PREVIEW_FUNC: Byte = 0x02

        /**支架的最大移动步长*/
        @MM
        val BRACKET_MAX_STEP: Int = 65535//130, 65535

        /**获取最佳限制框的path, 蓝色框提示
         * [com.angcyo.engrave.EngraveProductLayoutHelper.PREVIEW_COLOR] 最佳的限制框, 蓝光提示
         *
         * [getBoundsPath]
         * */
        fun getLimitPath(
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value,
            includeLimitPath: Boolean = true, /*是否要获取的对应的显示框*/
            includeTipPath: Boolean = false, /*获取对应的提示框*/
        ): Path? {
            val peckerModel = vmApp<LaserPeckerModel>()
            val deviceStateModel = vmApp<DeviceStateModel>()

            val limitPath = if (peckerModel.isZOpen()) {
                //Z轴打开
                if (includeTipPath) productInfo?.zTipPath else productInfo?.zLimitPath
            } else if (peckerModel.isROpen()) {
                //R轴打开
                if (includeTipPath) productInfo?.rTipPath else productInfo?.rLimitPath
            } else if (peckerModel.isSRepMode()) {
                //滑台多文件
                if (includeTipPath) productInfo?.sRepTipPath else productInfo?.sRepLimitPath
                    ?: productInfo?.sLimitPath
            } else if (peckerModel.isSOpen()) {
                //S轴打开
                if (includeTipPath) productInfo?.sTipPath else productInfo?.sLimitPath
            } else if (peckerModel.isCarConnect()) {
                //C1平台移动模式
                if (includeTipPath) productInfo?.carTipPath else productInfo?.carLimitPath
            } else if (deviceStateModel.isPenMode()) {
                //画笔模块
                if (includeTipPath) productInfo?.penTipPath else productInfo?.penBounds?.let { rect ->
                    Path().apply { addRect(rect, Path.Direction.CW) }
                }
            } else if (includeLimitPath) {
                if (includeTipPath) productInfo?.tipPath else productInfo?.limitPath
            } else {
                null
            }
            return limitPath
        }

        /**获取最大物理的path, 红色框提示
         * [com.angcyo.engrave.EngraveProductLayoutHelper.ENGRAVE_COLOR] 物理范围, 红光提示
         * [getLimitPath]
         * */
        @Pixel
        fun getBoundsPath(productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): Path? {
            val peckerModel = vmApp<LaserPeckerModel>()
            val deviceStateModel = vmApp<DeviceStateModel>()
            val boundsPath = if (peckerModel.isZOpen()) {
                productInfo?.zLimitPath
            } else if (peckerModel.isROpen()) {
                productInfo?.rLimitPath
            } else if (peckerModel.isSOpen() || peckerModel.isSRepMode()) {
                productInfo?.sLimitPath
            } else if (peckerModel.isCarConnect()) {
                productInfo?.carLimitPath
            } else if (deviceStateModel.isPenMode()) {
                productInfo?.penBounds?.let { rect ->
                    Path().apply { addRect(rect, Path.Direction.CW) }
                }
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
            layerId: String = LaserPeckerHelper.LAYER_LINE,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): OverflowInfo {
            rect ?: return OverflowInfo()
            return adjustRectRange(
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                layerId,
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
            layerId: String = LaserPeckerHelper.LAYER_LINE,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? =
                vmApp<LaserPeckerModel>().productInfoData.value
        ): OverflowInfo {
            val tempRect = acquireTempRectF()
            var overflowType = 0
            if (productInfo != null) {
                tempRect.set(
                    x.toInt().toFloat(),
                    y.toInt().toFloat(),
                    (x + width).toInt().toFloat(),
                    (y + height).toInt().toFloat()
                )//2023-4-4 修复预览时, 会出现小数点的情况
                val laserPeckerModel = vmApp<LaserPeckerModel>()

                //超过限制
                if (getBoundsPath(productInfo)?.overflow(tempRect) == true) {
                    overflowType = overflowType.add(OverflowInfo.OVERFLOW_TYPE_BOUNDS)
                }

                /*if (laserPeckerModel.isOverflowHeight(tempRect.height().toMm().ceilInt())) {
                    overflowType = overflowType.add(OverflowInfo.OVERFLOW_TYPE_HEIGHT)
                }*/

                //溢出
                if (getLimitPath(productInfo)?.overflow(tempRect) == true) {
                    overflowType = overflowType.add(OverflowInfo.OVERFLOW_TYPE_LIMIT)
                }
                /*if (laserPeckerModel.isOverflowHeightLimit(tempRect.height().toMm().ceilInt())) {
                    overflowType = overflowType.add(OverflowInfo.OVERFLOW_TYPE_HEIGHT_LIMIT)
                }*/
            }

            var previewX = x
            var previewY = y

            var previewWidth = width
            var previewHeight = height

            val pxInfo = LaserPeckerHelper.findPxInfo(layerId, dpi)
            if (overflowType.isOverflowBounds()) {
                //预览超出了设备物理范围, 缩成设备物理中心点

                //超过范围, 缩成在中心的一个点
                previewX = (productInfo?.bounds?.width() ?: 0f) / 2
                previewY = (productInfo?.bounds?.height() ?: 0f) / 2

                //平移
                previewX = pxInfo.transformX(previewX)
                previewY = pxInfo.transformY(previewY)

                previewWidth = 1f
                previewHeight = 1f
            } else {
                previewX = pxInfo.transformX(previewX)
                previewY = pxInfo.transformY(previewY)

                previewWidth = pxInfo.transformWidth(previewWidth)
                previewHeight = pxInfo.transformHeight(previewHeight)
            }

            tempRect.release()
            val rect = Rect().apply {
                left = previewX.floor().toInt()
                top = previewY.floor().toInt()
                right = left + previewWidth.ceil().toInt()
                bottom = top + previewHeight.ceil().toInt()
            }
            return OverflowInfo(rect, null, overflowType)
        }

        /**调整4点坐标, 并标识是否溢出*/
        fun adjustFourPoint(
            rectPoint: RectPointF,
            layerId: String,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): OverflowInfo {
            var overflowType = 0
            if (productInfo != null) {
                val rectPath = rectPoint.toPath()

                //超过限制
                if (getLimitPath(productInfo)?.overflow(rectPath) == true) {
                    overflowType = overflowType.add(OverflowInfo.OVERFLOW_TYPE_BOUNDS)
                }

                //溢出
                if (getBoundsPath(productInfo)?.overflow(rectPath) == true) {
                    overflowType = overflowType.add(OverflowInfo.OVERFLOW_TYPE_LIMIT)
                }

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

            val pxInfo = LaserPeckerHelper.findPxInfo(layerId, dpi)
            result.leftTop.x = pxInfo.transformX(rectPoint.leftTop.x)
            result.leftBottom.x = pxInfo.transformX(rectPoint.leftBottom.x)
            result.rightTop.x = pxInfo.transformX(rectPoint.rightTop.x)
            result.rightBottom.x = pxInfo.transformX(rectPoint.rightBottom.x)

            result.leftTop.y = pxInfo.transformY(rectPoint.leftTop.y)
            result.leftBottom.y = pxInfo.transformY(rectPoint.leftBottom.y)
            result.rightTop.y = pxInfo.transformY(rectPoint.rightTop.y)
            result.rightBottom.y = pxInfo.transformY(rectPoint.rightBottom.y)

            return OverflowInfo(null, result, overflowType)
        }

        /**[adjustPreviewRangeCmd]*/
        fun adjustPreviewRangeCmd(
            rect: RectF,
            @Pixel boundsList: List<RectF>?,
            @Pixel elementBoundsList: List<RectF>?,
            pwrProgress: Float,
            @MM diameter: Int
        ): EngravePreviewCmd? {
            return adjustPreviewRangeCmd(
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                boundsList,
                elementBoundsList,
                pwrProgress,
                diameter
            )
        }

        /**根据[px]和[productInfo]调整预览的范围
         * [pwrProgress] [0~1f] 预览光功率
         * [diameter] 物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
         * */
        fun adjustPreviewRangeCmd(
            @Px x: Float, @Px y: Float, @Px width: Float, @Px height: Float,
            @Pixel boundsList: List<RectF>?,
            @Pixel elementBoundsList: List<RectF>?,
            pwrProgress: Float,
            @MM
            diameter: Int,
            layerId: String = LaserPeckerHelper.LAYER_LINE,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd? {
            val overflowInfo = adjustRectRange(x, y, width, height, layerId, dpi, productInfo)

            val laserPeckerModel = vmApp<LaserPeckerModel>()
            elementBoundsList?.forEach {
                //超过限制
                if (laserPeckerModel.isOverflowHeight(it.height().toMm().ceilInt())) {
                    overflowInfo.overflowType =
                        overflowInfo.overflowType.add(OverflowInfo.OVERFLOW_TYPE_HEIGHT)
                }

                //溢出
                if (laserPeckerModel.isOverflowHeightLimit(it.height().toMm().ceilInt())) {
                    overflowInfo.overflowType =
                        overflowInfo.overflowType.add(OverflowInfo.OVERFLOW_TYPE_HEIGHT_LIMIT)
                }
            }

            laserPeckerModel.overflowInfoData.postValue(overflowInfo)
            if (overflowInfo.overflowType.have(OverflowInfo.OVERFLOW_TYPE_BOUNDS) ||
                (HawkEngraveKeys.enableDataBoundsStrict && overflowInfo.overflowType.have(
                    OverflowInfo.OVERFLOW_TYPE_LIMIT
                ))
            ) {
                //超出物理范围, 不发送指令
                return null
            }
            return _previewRangeCmd(
                overflowInfo.resultRect!!,
                boundsList,
                pwrProgress,
                diameter,
                dpi
            )
        }

        /**表示范围预览
         * [rect] 经过[px]处理过的像素*/
        fun _previewRangeCmd(
            @Pixel rect: Rect,
            @Pixel boundsList: List<RectF>?,

            pwrProgress: Float,
            @MM diameter: Int,
            dpi: Float
        ): EngravePreviewCmd {
            return _previewRangeCmd(
                rect.left,
                rect.top,
                rect.width(),
                rect.height(),
                boundsList,
                pwrProgress,
                diameter,
                dpi
            )
        }

        fun _previewRangeCmd(
            @Px x: Int, @Px y: Int, @Px width: Int, @Px height: Int,
            @Pixel boundsList: List<RectF>?,
            pwrProgress: Float,
            @MM
            diameter: Int,  //物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
            dpi: Float = LaserPeckerHelper.DPI_254,
        ): EngravePreviewCmd {
            val widthBytes = width.toHexString(4).toHexByteArray()
            val heightBytes = height.toHexString(4).toHexByteArray()

            if (boundsList.size() > 0) {
                //多矩形预览
                return EngravePreviewCmd(0x0C).apply {
                    rectList = boundsList
                    updatePWR(pwrProgress)
                }
            }

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
            layerId: String = LaserPeckerHelper.LAYER_LINE,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd? {
            val overflowInfo = adjustFourPoint(rectPoint, layerId, dpi, productInfo)
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)

            if (overflowInfo.overflowType.isOverflowBounds()) {
                //溢出了, 预览一个点
                /*return _previewRangeCmd(
                    overflowInfo.resultRectPoint!!.originRectF.centerX(),
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
        fun previewBracketUpCmd(@MM step: Int = BRACKET_MAX_STEP): EngravePreviewCmd {
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
        fun previewBracketDownCmd(@MM step: Int = BRACKET_MAX_STEP): EngravePreviewCmd {
            return EngravePreviewCmd(0x06).apply {
                if (vmApp<LaserPeckerModel>().productInfoData.value?.isLI_Z() == true) {
                    d1 = 0x02 //0x00 //0x03
                    d4 = 0x01
                } else {
                    d1 = 0x00
                    val bytes = (step * 10).toHexString(4).toHexByteArray()
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
        fun previewShowCenterCmd(
            pwrProgress: Float,
            bounds: RectF,
            boundsList: List<RectF>?
        ): EngravePreviewCmd? {
            val overflowInfo = adjustRectRange(
                bounds.left,
                bounds.top,
                bounds.width(),
                bounds.height(),
                LaserPeckerHelper.LAYER_LINE,
                LaserPeckerHelper.DPI_254,
                vmApp<LaserPeckerModel>().productInfoData.value
            )
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)
            if (overflowInfo.overflowType.isOverflowBounds()) {
                return null
            }
            val rect = overflowInfo.resultRect!!

            if (boundsList.size() > 0) {
                //多矩形预览
                return EngravePreviewCmd(0x0C).apply {
                    rectList = boundsList?.map {
                        RectF(it.centerX(), it.centerY(), it.centerX() + 1f, it.centerY() + 1f)
                    }
                    updatePWR(pwrProgress)
                }
            }

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
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            pwrProgress: Float,
            @MM
            diameter: Int,
            layerId: String = LaserPeckerHelper.LAYER_LINE,
            dpi: Float = LaserPeckerHelper.DPI_254,
            productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
        ): EngravePreviewCmd? {
            val overflowInfo = adjustRectRange(x, y, width, height, layerId, dpi, productInfo)
            vmApp<LaserPeckerModel>().overflowInfoData.postValue(overflowInfo)
            if (overflowInfo.overflowType.isOverflowBounds()) {
                //超出物理范围, 不发送指令
                return null
            }
            return _previewZRangeCmd(overflowInfo.resultRect!!, pwrProgress, diameter, dpi)
        }

        /**第三轴暂停预览, 用来拖动时更新x,t
         * [rect] 最终调整过后的坐标
         * [pwrProgress] [0~1f] 预览光功率
         * */
        fun _previewZRangeCmd(
            rect: Rect,
            pwrProgress: Float,
            @MM
            diameter: Int,
            dpi: Float = LaserPeckerHelper.DPI_254
        ): EngravePreviewCmd {

            val widthBytes = rect.width().toHexString(4).toHexByteArray()
            val heightBytes = rect.height().toHexString(4).toHexByteArray()

            return EngravePreviewCmd(0x04).apply {

                d1 = widthBytes[0]
                d2 = widthBytes[1]
                d3 = heightBytes[0]
                d4 = heightBytes[1]

                x = rect.left
                y = rect.top

                this.diameter = diameter

                updatePWR(pwrProgress)
            }
        }

        /**第三轴继续预览指令, z轴滚动预览*/
        fun previewZContinueCmd(pwrProgress: Float): EngravePreviewCmd {
            return EngravePreviewCmd(0x05).apply {
                updatePWR(pwrProgress)
            }
        }

        /**C1专属, 第三轴继续预览指令, z轴滚动预览*/
        fun previewZScrollCmd(pwrProgress: Float): EngravePreviewCmd {
            return EngravePreviewCmd(0x0A).apply {
                updatePWR(pwrProgress)
            }
        }

        //--

        /**0x09 对笔控制：data1:0x01为对笔，0x02为对笔完成。（C1产品）
         * 开始校准指令*/
        fun startCalibrationCmd(): EngravePreviewCmd {
            return EngravePreviewCmd(0x09).apply {
                d1 = 0x01
            }
        }

        /**0x09 对笔控制：data1:0x01为对笔，0x02为对笔完成。（C1产品）
         * 完成校准指令*/
        fun finishCalibrationCmd(): EngravePreviewCmd {
            return EngravePreviewCmd(0x09).apply {
                d1 = 0x02
            }
        }

        /**文件名预览指令
         * [pwrProgress] [0~1f] 预览光功率
         * */
        fun fileNamePreviewCmd(
            fileName: String,
            mount: Byte,
            @MM
            diameter: Int,
            pwrProgress: Float = HawkEngraveKeys.lastPwrProgress
        ): EngravePreviewCmd = EngravePreviewCmd(0x0B).apply {
            d1 = mount
            this.fileName = fileName
            this.diameter = diameter
            updatePWR(pwrProgress)
        }
    }

    //功能码
    override fun commandFunc(): Byte = ENGRAVE_PREVIEW_FUNC

    override fun toByteArray(): ByteArray {
        if (state == 0x0B.toByte()) {
            return commandByteWriter {
                write(commandFunc())
                write(state)
                write(d1)
                fillLength(7)//填充7个字节
                write(custom)
                write(px)
                write(pwr)
                write(diameter, 2)
                fileName?.let {
                    write(it)
                    write(0)//结束字符
                }
            }
        } else if (state == 0x0C.toByte()) {
            return commandByteWriter {
                write(commandFunc())
                write(state)
                write(pwr)
                rectList?.let {
                    write(it.size())
                    it.forEach { rect ->
                        write((rect.left.toMm() * 10).floorInt(), 2)
                        write((rect.top.toMm() * 10).floorInt(), 2)
                        write((rect.width().toMm() * 10).ceilInt(), 2)
                        write((rect.height().toMm() * 10).ceilInt(), 2)
                    }
                }
            }
        }
        return super.toByteArray()
    }

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
        if (state == 0x0B.toByte() || state == 0x0C.toByte()) {
            append(toByteArray().toHexString())
        } else {
            append(toHexCommandString().removeAll())
        }
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
                append("范围预览:")
                val rect = getPreviewRange()
                append("$rect w:${rect.width()} h:${rect.height()} 直径:$diameter")
            }

            0x03.toByte() -> append("结束预览")
            0x04.toByte() -> {
                append("第三轴暂停预览:")
                val rect = getPreviewRange()
                append("$rect w:${rect.width()} h:${rect.height()} 直径:$diameter")
            }

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

            0x07.toByte() -> {
                val rect = getPreviewRange()
                append("显示中心:${rect} cx:${rect.centerX()} cy:${rect.centerY()}")
            }

            0x08.toByte() -> {
                append(" 4点预览")
                val rectPoint = getFourPoint()
                append(" lt:${rectPoint.leftTop} rt:${rectPoint.rightTop} rb:${rectPoint.rightBottom} lb:${rectPoint.leftBottom}")
            }

            0x09.toByte() -> when (d1) {
                0x01.toByte() -> append("开始对笔")
                0x02.toByte() -> append("完成对笔")
                else -> append("对笔控制")
            }

            0x0B.toByte() -> append("文件名预览:[${d1.toSdOrUsbStr()}] $fileName")
            0x0C.toByte() -> append("多范围预览:${rectList}")

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
        pwr = progress.toLaserPeckerPWR()
    }
}

/**[0~1]范围为[1 - 10]。*/
fun Float.toLaserPeckerPWR(): Byte {
    return (1 + 9 * this).toInt().toByte()
}

/**[0~1]范围为[0~255]。*/
fun Float.toLaserPeckerPower(): Byte {
    return (255 * this).toInt().toByte()
}
