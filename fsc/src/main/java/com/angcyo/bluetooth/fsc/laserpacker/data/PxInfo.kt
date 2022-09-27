package com.angcyo.bluetooth.fsc.laserpacker.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_PX
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
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
    //当PX为以下值时对应图片分辨率：
    //PX = 0x05 时 图片分辨率为800*800
    //PX = 0x04 时 图片分辨率为1000*1000
    //PX = 0x03 时 图片分辨率为1300*1300
    //PX = 0x02 时 图片分辨率为2000*2000
    //PX = 0x01 时 图片分辨率为4000*4000
    val px: Byte = DEFAULT_PX,
    //对应分辨率的宽高
    val pxWidth: Int,
    val pxHeight: Int,
    //显示的界面上的描述
    val des: String,
) : IToText, IToValue {

    /**宽度值转换*/
    fun transformWidth(
        width: Int,
        productInfo: LaserPeckerProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return width
        }
        val scale = width * 1f / productInfo.bounds.width() //实际尺寸对应的比例
        return (pxWidth * scale).toInt() //缩放转换
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
        return (pxHeight * scale).toInt()
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
        return (pxWidth * scale).toInt()
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
        return (pxHeight * scale).toInt()
    }

    override fun toText(): CharSequence = des

    override fun toValue(): Any = px
}
