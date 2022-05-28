package com.angcyo.bluetooth.fsc.laserpacker

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