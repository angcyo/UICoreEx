package com.angcyo.bluetooth.fsc.laserpacker.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.DEFAULT_PX
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp

/**
 * 打印像素的信息
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
) {

    /**宽度值转换*/
    fun transformWidth(
        width: Int,
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
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
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
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
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return x
        }
        val scale = x * 1f / (productInfo.bounds.width() / 2) //x方向的实际缩放比例
        val centerX = pxWidth / 2
        return (centerX + centerX * scale).toInt()
    }

    /**Y坐标转换*/
    fun transformY(
        y: Int,
        productInfo: ProductInfo? = vmApp<LaserPeckerModel>().productInfoData.value
    ): Int {
        if (productInfo == null) {
            return y
        }
        val scale = y * 1f / (productInfo.bounds.height() / 2) //y方向的实际缩放比例
        val centerY = pxHeight / 2
        return (centerY + centerY * scale).toInt()
    }

}
