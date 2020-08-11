package com.angcyo.bmob.bean

import android.os.Build
import cn.bmob.v3.BmobObject
import com.angcyo.library.app
import com.angcyo.library.ex.connect
import com.angcyo.library.ex.fileSizeString
import com.angcyo.library.ex.getMobileIP
import com.angcyo.library.ex.getWifiIP
import com.angcyo.library.utils.Device

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/08
 */

data class DeviceBmob(
    var userId: String? = null,
    var psuedoID: String? = Device.deviceId,
    var androidId: String? = Device.androidId,
    var deviceModel: String? = null,
    var cpu: String? = null,
    var memorySize: String? = null,
    var sdSize: String? = null,
    var ip: String? = null,
    var buildString: String? = null,
    var screenInfo: String? = null,
    var proxy: String? = null,
    var other: String? = null,
    var packageName: String? = app().packageName
) : BmobObject() {
    companion object {

        /**快速获取一个对象*/
        fun get(userId: String? = null, other: String? = null): DeviceBmob {
            val bmob = DeviceBmob()
            bmob.apply {
                this.userId = userId
                this.other = other
                deviceModel = buildString {
                    //OnePlus/ONEPLUS A6000/jenkins/qcom/ONEPLUS A6000_22_191215
                    append(Build.MANUFACTURER)//LGE
                    append("/")
                    append(Build.MODEL)//Nexus 5X
                    append("/")
                    append(Build.USER)//android-build
                    append("/")
                    append(Build.HARDWARE)//bullhead
                    append("/")
                    append(Build.DISPLAY)//N2G48C
                }
                cpu = Build.SUPPORTED_ABIS.connect("/").toString()

                ip = buildString {
                    append(getWifiIP()).append("|").append(getMobileIP())
                }

                buildString = buildString {
                    Device.buildString(this)
                }

                screenInfo = buildString {
                    Device.screenInfo(app(), this)
                }

                //内存信息
                memorySize = buildString {
                    append(Device.getAvailableMemory().fileSizeString())
                    append(" /")
                    append(Device.getTotalMemory().fileSizeString())
                }

                //SD空间信息
                sdSize = buildString {
                    append(Device.getSdAvailableBytes().fileSizeString())
                    append("/")
                    append(Device.getSdTotalBytes().fileSizeString())
                }

                //代理
                proxy = buildString {
                    append(Device.proxyInfo())
                    append("/")
                    append(Device.vpnInfo())
                }
            }
            return bmob
        }
    }
}