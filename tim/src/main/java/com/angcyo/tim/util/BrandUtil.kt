package com.angcyo.tim.util

import android.os.Build

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/12/30
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object BrandUtil {

    /**
     * 判断是否为小米设备
     */
    fun isBrandXiaoMi(): Boolean {
        return ("xiaomi".equals(Build.BRAND, ignoreCase = true)
            || "xiaomi".equals(Build.MANUFACTURER, ignoreCase = true))
    }

    /**
     * 判断是否为华为设备
     */
    fun isBrandHuawei(): Boolean {
        return ("huawei".equals(Build.BRAND, ignoreCase = true)
            || "huawei".equals(Build.MANUFACTURER, ignoreCase = true))
    }

    /**
     * 判断是否为魅族设备
     */
    fun isBrandMeizu(): Boolean {
        return ("meizu".equals(Build.BRAND, ignoreCase = true)
            || "meizu".equals(Build.MANUFACTURER, ignoreCase = true)
            || "22c4185e".equals(Build.BRAND, ignoreCase = true))
    }

    /**
     * 判断是否是oppo设备
     *
     * @return
     */
    fun isBrandOppo(): Boolean {
        return ("oppo".equals(Build.BRAND, ignoreCase = true)
            || "oppo".equals(Build.MANUFACTURER, ignoreCase = true))
    }

    /**
     * 判断是否是vivo设备
     *
     * @return
     */
    fun isBrandVivo(): Boolean {
        return ("vivo".equals(Build.BRAND, ignoreCase = true)
            || "vivo".equals(Build.MANUFACTURER, ignoreCase = true))
    }

    /**
     * 判断是否支持谷歌服务
     *
     * @return
     */
    fun isGoogleServiceSupport(): Boolean {
        /*val googleApiAvailability: GoogleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode: Int =
            googleApiAvailability.isGooglePlayServicesAvailable(DemoApplication.instance())
        return resultCode == ConnectionResult.SUCCESS*/
        return false
    }
}