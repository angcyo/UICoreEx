package com.angcyo.bluetooth.fsc.laserpacker.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_1270
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_158
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_317
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_508
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_0_8K
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_1K
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_1_3K
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_2K
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_4K
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.MM
import com.angcyo.library.ex.decimal
import com.angcyo.library.ex.floor
import com.angcyo.library.extend.IToText
import com.angcyo.library.extend.IToValue

/**
 * 打印像素的信息
 *
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_1K]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_1_3K]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_2K]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.PX_4K]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/01
 */
data class PxInfo(
    /*//当PX为以下值时对应图片分辨率：
    //PX = 0x05 时 图片分辨率为800*800
    //PX = 0x04 时 图片分辨率为1000*1000
    //PX = 0x03 时 图片分辨率为1300*1300
    //PX = 0x02 时 图片分辨率为2000*2000
    //PX = 0x01 时 图片分辨率为4000*4000
    val px: Byte = DEFAULT_PX,
    //对应分辨率的宽高
    val pxWidth: Int,
    val pxHeight: Int,*/

    /**每英寸内像素点的个数
     * 设备基准值: 254, 像素点间距0.1mm 最小能达到:0.0125 8倍
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_158]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_254]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_317]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_423]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_508]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_635]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_846]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_1270]
     * */
    val dpi: Float,
    /**最大的物理尺寸
     * [com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo.widthPhys]*/
    @MM
    val physSize: Int,
) : IToText, IToValue {

    /**dpi对应的数据需要缩放的比例*/
    val dpiScale: Float = dpi.toDpiScale()

    /**显示的界面上的描述*/
    val des: String = dpi.toPxDes(physSize)

    override fun toText(): CharSequence = des

    override fun toValue(): Int = dpi.toDpiInt()

    /**设备dpi对应的像素宽度*/
    fun devicePxWidth(productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): Float {
        val deviceWidthPx = ((productInfo?.widthPhys ?: 100) / 0.1).floor()
        return (deviceWidthPx * dpiScale).toFloat()
    }

    /**设备dpi对应的像素高度*/
    fun devicePxHeight(productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value): Float {
        val deviceHeightPx = ((productInfo?.heightPhys ?: 100) / 0.1).floor()
        return (deviceHeightPx * dpiScale).toFloat()
    }

    /**宽度值转换*/
    fun transformWidth(
        width: Int,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return width
        }
        val scale = width * 1f / productInfo.bounds.width() //实际尺寸对应的比例
        return (devicePxWidth(productInfo) * scale).toInt() //缩放转换
    }

    /**高度值转换*/
    fun transformHeight(
        height: Int,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return height
        }
        val scale = height * 1f / productInfo.bounds.height()
        return (devicePxHeight(productInfo) * scale).toInt()
    }

    /**X坐标转换*/
    fun transformX(
        x: Int,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return x
        }
        val scale = (x - productInfo.bounds.left) * 1f / productInfo.bounds.width() //x方向的实际缩放比例
        return (devicePxWidth(productInfo) * scale).toInt()
    }

    /**Y坐标转换*/
    fun transformY(
        y: Int,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return y
        }
        val scale = (y - productInfo.bounds.top) * 1f / productInfo.bounds.height() //y方向的实际缩放比例
        return (devicePxHeight(productInfo) * scale).toInt()
    }

}

/**缩放比*/
fun Float.toDpiScale() = this / 254

fun Float.toDpiInt() = floor().toInt()

/**dpi转描述字符串*/
fun Float.toPxDes(
    @MM
    physSize: Int = vmApp<LaserPeckerModel>().productInfoData.value?.widthPhys ?: 100
): String {
    val scale = toDpiScale()
    val scaleFloor = scale.floor()

    //总共有这么多像素点
    val physSizePx = physSize / 0.1

    //包含小数的写法
    val sk = physSizePx * scale / 1000
    //不包含小数的写法
    val sFK = (physSizePx * scaleFloor / 1000).floor()

    if (sk == sFK) {
        return "${sFK.toInt()}K"
    }
    return "${sk.decimal(1)}K"
}

/**将dpi, 转换成原来的px单位数据
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_158]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_254]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_317]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_508]
 * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DPI_1270]
 * */
fun Float.toOldPxByte(): Byte = when (this) {
    DPI_158 -> PX_0_8K
    DPI_317 -> PX_1_3K
    DPI_508 -> PX_2K
    DPI_1270 -> PX_4K
    else -> PX_1K
}