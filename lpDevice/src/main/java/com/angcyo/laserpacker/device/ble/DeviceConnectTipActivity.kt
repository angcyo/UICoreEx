package com.angcyo.laserpacker.device.ble

import android.os.Bundle
import android.view.Gravity
import com.airbnb.lottie.LottieAnimationView
import com.angcyo.activity.BaseDialogActivity
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.component.model.NightModel
import com.angcyo.core.vmApp
import com.angcyo.laserpacker.device.R
import com.angcyo.library.L
import com.angcyo.library._screenHeight
import com.angcyo.library._screenWidth
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex._color
import kotlin.math.min

/**
 * 设备自动连接后的通知提示界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/28
 */
class DeviceConnectTipActivity : BaseDialogActivity() {

    companion object {

        /**通过蓝牙名称获取获取设备类型
         * 也可以通过硬件版本获取设备类型[com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]*/
        fun getDeviceType(name: String? = vmApp<LaserPeckerModel>().productInfoData.value?.deviceName): String {
            val prefix = LaserPeckerHelper.PRODUCT_PREFIX
            val result = when {
                name == LaserPeckerHelper.CI ||
                        name?.startsWith("$prefix-CI") == true ||
                        name?.startsWith("$prefix-${LaserPeckerHelper.CI}") == true ||
                        name?.startsWith(LaserPeckerHelper.CI) == true ||
                        name?.startsWith(LaserPeckerHelper.CI_OLD) == true -> LaserPeckerHelper.CI

                name == LaserPeckerHelper.LV ||
                        name?.startsWith("$prefix-V") == true ||
                        name?.startsWith(LaserPeckerHelper.LV) == true -> LaserPeckerHelper.LV

                name == LaserPeckerHelper.LIV ||
                        name?.startsWith("$prefix-IV") == true ||
                        name?.startsWith(LaserPeckerHelper.LIV) == true -> LaserPeckerHelper.LIV

                name == LaserPeckerHelper.LIII ||
                        name?.startsWith("$prefix-III") == true ||
                        name?.startsWith(LaserPeckerHelper.LIII) == true -> LaserPeckerHelper.LIII

                name == LaserPeckerHelper.LII ||
                        name?.startsWith("$prefix-II") == true ||
                        name?.startsWith(LaserPeckerHelper.LII) == true -> LaserPeckerHelper.LII

                name == LaserPeckerHelper.LI ||
                        name?.startsWith("$prefix-I") == true ||
                        name?.startsWith(LaserPeckerHelper.LI) == true -> LaserPeckerHelper.LI

                else -> LaserPeckerHelper.LI
            }
            L.d("格式化蓝牙名称:$name->$result")
            return result
        }

        /**根据设备名, 获取设备对应的图片资源
         * [name] 设备名, 或者蓝牙名都支持
         * */
        fun getDeviceImageRes(name: String? = vmApp<LaserPeckerModel>().productInfoData.value?.deviceName): Int =
            when (getDeviceType(name)) {
                LaserPeckerHelper.CI -> R.mipmap.device_c1
                LaserPeckerHelper.LV -> R.mipmap.device_l5
                LaserPeckerHelper.LIV -> R.mipmap.device_l4
                LaserPeckerHelper.LIII -> R.mipmap.device_l3
                LaserPeckerHelper.LII -> R.mipmap.device_l2
                LaserPeckerHelper.LI -> R.mipmap.device_l1
                else -> R.mipmap.device_l1
            }

        /**格式化蓝牙名称*/
        fun formatDeviceName(name: String? = vmApp<LaserPeckerModel>().productInfoData.value?.deviceName): String? {
            val prefix = LaserPeckerHelper.PRODUCT_PREFIX
            val c1 = "$prefix-CI"
            val c1_2 = "$prefix-${LaserPeckerHelper.CI}"
            val lp5 = "$prefix-V"
            val lp4 = "$prefix-IV"
            val lp3 = "$prefix-III"
            val lp2 = "$prefix-II"
            val lp1 = "$prefix-I"
            return when {
                name?.startsWith(c1) == true -> name.replace(c1, "${LaserPeckerHelper.CI}-")
                name?.startsWith(c1_2) == true -> name.replace(c1_2, "${LaserPeckerHelper.CI}-")
                name?.startsWith(lp5) == true -> name.replace(lp5, "${LaserPeckerHelper.LV}-")
                name?.startsWith(lp4) == true -> name.replace(lp4, "${LaserPeckerHelper.LIV}-")
                name?.startsWith(lp3) == true -> name.replace(lp3, "${LaserPeckerHelper.LIII}-")
                name?.startsWith(lp2) == true -> name.replace(lp2, "${LaserPeckerHelper.LII}-")
                name?.startsWith(lp1) == true -> name.replace(lp1, "${LaserPeckerHelper.LI}-")
                else -> name
            }
        }

        /**指定的蓝牙设备名, 是否是wifi设备*/
        fun isWifiDevice(deviceName: String?): Boolean =
            LaserPeckerHelper.isWifiDevice(formatDeviceName(deviceName))

        fun isLp5Device(deviceName: String?): Boolean {
            val name = formatDeviceName(deviceName)
            return name?.startsWith(LaserPeckerHelper.LV) == true
        }

        fun isLx2Device(deviceName: String?): Boolean {
            val name = formatDeviceName(deviceName)
            return name?.startsWith(LaserPeckerHelper.CII) == true
        }
    }

    init {
        activityLayoutId = R.layout.activity_device_connect_tip
        dialogGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        if (isInPadMode()) {
            dialogWidth = min(_screenWidth, _screenHeight)
        }
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        vmApp<LaserPeckerModel>().productInfoData.value?.apply {
            _vh.tv(R.id.device_name_view)?.text = formatDeviceName(deviceName)
            //设备图

            //动画资源
            _vh.v<LottieAnimationView>(R.id.device_image_view)?.apply {
                when (getDeviceType(deviceName)) {
                    LaserPeckerHelper.LIV -> {
                        imageAssetsFolder = "lottie/L4/images"
                        setAnimation("lottie/L4/data.json")
                    }

                    LaserPeckerHelper.LIII -> {
                        imageAssetsFolder = "lottie/L3/images"
                        setAnimation("lottie/L3/data.json")
                    }

                    LaserPeckerHelper.LII -> {
                        imageAssetsFolder = "lottie/L2/images"
                        setAnimation("lottie/L2/data.json")
                    }

                    LaserPeckerHelper.LI -> {
                        imageAssetsFolder = "lottie/L1/images"
                        setAnimation("lottie/L1/data.json")
                    }

                    else -> setImageResource(getDeviceImageRes(name))
                }
            }
        }

        _vh.click(R.id.finish_button) {
            finish()
        }

        if (vmApp<NightModel>().isDarkMode) {
            _vh.tv(R.id.setting_button)?.setTextColor(_color(R.color.text_primary_color))
        }
        _vh.click(R.id.setting_button) {
            finish()
            dslAHelper {
                start(DeviceSettingFragment::class.java)
            }
        }
    }
}