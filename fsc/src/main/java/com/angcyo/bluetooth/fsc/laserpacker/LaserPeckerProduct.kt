package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.Path
import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.data.ProductInfo
import com.angcyo.canvas.core.MmValueUnit

/**
 * 产品型号/参数表
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/28
 */
object LaserPeckerProduct {

    const val LI = "LI"
    const val LI_Z = "LI-Z"           //spp 100*100mm
    const val LI_PRO = "LI-PRO"       //spp 100*100mm
    const val LI_Z_PRO = "LI-Z-PRO"   //spp 100*100mm
    const val LII = "LII"             //spp 100*100mm
    const val LI_Z_ = "LI-Z模块"         //spp 100*100mm
    const val LII_M_ = "LII-M模块"       //spp 100*100mm
    const val LIII_YT = "LIII-YT"       //spp 50*50mm
    const val LIII = "LIII"           //spp 90*70mm 椭圆
    const val LIII_MAX = "LIII-MAX"   //spp 160*120mm 椭圆
    const val CI = "CI"               //spp 300*400mm
    const val CII = "CII"
    const val UNKNOWN = "Unknown"

    /**解析产品信息*/
    fun parseProduct(version: Int): ProductInfo {
        val name = parseProductName(version)
        val unit = MmValueUnit()
        val bounds = RectF()
        var isOriginCenter = true
        val limitPath: Path = when (name) {
            LI_Z, LI_PRO, LI_Z_PRO, LII, LI_Z_, LII_M_ -> {
                Path().apply {
                    val left = unit.convertValueToPixel(-50f)
                    val right = unit.convertValueToPixel(50f)
                    bounds.set(left, left, right, right)
                    addRect(bounds, Path.Direction.CW)
                }
            }
            LIII -> {
                Path().apply {
                    val left = unit.convertValueToPixel(-50f)
                    val right = unit.convertValueToPixel(50f)
                    bounds.set(left, left, right, right)

                    val l = unit.convertValueToPixel(-50f)
                    val t = unit.convertValueToPixel(-35f)
                    //addOval(l, t, -l, -t, Path.Direction.CW)
                    maxOvalPath(l, t, -l, -t, this)
                }
            }
            LIII_MAX -> {
                Path().apply {
                    val left = unit.convertValueToPixel(-100f)
                    val right = unit.convertValueToPixel(100f)
                    bounds.set(left, left, right, right)

                    val l = unit.convertValueToPixel(-80f)
                    val t = unit.convertValueToPixel(-60f)
                    //addOval(l, t, -l, -t, Path.Direction.CW)
                    maxOvalPath(l, t, -l, -t, this)
                }
            }
            CI -> {
                isOriginCenter = false
                Path().apply {
                    val width = unit.convertValueToPixel(300f)
                    val height = unit.convertValueToPixel(400f)
                    bounds.set(0f, 0f, width, height)
                    addRect(bounds, Path.Direction.CW)
                }
            }
            else -> Path()
        }
        val info = ProductInfo(version, name, bounds, limitPath, isOriginCenter)
        return info
    }

    /**中间一个正方形, 左右各一个半圆*/
    fun maxOvalPath(left: Float, top: Float, right: Float, bottom: Float, path: Path) {
        val width = right - left
        val height = bottom - top
        if (width > height) {
            val x1 = (width - height) / 2
            path.moveTo(x1, bottom)
            path.addArc(left, top, left + x1 + x1, bottom, 90f, 180f)
            path.lineTo(left + x1 + height, top)

            path.addArc(right - x1 - x1, top, right, bottom, -90f, 180f)
            path.lineTo(left + x1, bottom)
        } else {
            val y1 = (height - width) / 2
            path.moveTo(left, y1)
            path.addArc(left, top, right, y1 + y1, 180f, 180f)
            path.lineTo(right, bottom - y1)

            path.addArc(left, bottom - y1 - y1, right, bottom, 0f, 180f)
            path.lineTo(left, top + y1)
        }
    }

    /**根据软件版本号, 解析成产品名称
     * Ⅰ、Ⅱ、Ⅲ、Ⅳ、Ⅴ、Ⅵ、Ⅶ、Ⅷ、Ⅸ、Ⅹ、Ⅺ、Ⅻ...
     * https://docs.qq.com/sheet/DT0htVG9tamZQTFBz*/
    fun parseProductName(version: Int): String {
        val str = "$version"
        return if (str.startsWith("1")) { //1
            if (str.startsWith("15")) LI_Z else LI
        } else if (str.startsWith("2")) { //2
            if (str.startsWith("25")) LI_Z_PRO else LI_PRO
        } else if (str.startsWith("3")) { //3
            LII
        } else if (str.startsWith("4")) { //4
            if (str.startsWith("41")) LI_Z_ else if (str.startsWith("42")) LII_M_ else LIII_YT
        } else if (str.startsWith("5")) { //5
            LIII
        } else if (str.startsWith("6")) { //6
            LIII_MAX
        } else if (str.startsWith("7")) { //7
            if (str.startsWith("75")) CII else CI
        } else {
            UNKNOWN
        }
    }
}