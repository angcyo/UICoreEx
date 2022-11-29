package com.angcyo.engrave.ble

import android.os.Bundle
import android.view.Gravity
import com.airbnb.lottie.LottieAnimationView
import com.angcyo.activity.BaseDialogActivity
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.R

/**
 * 设备自动连接后的通知提示界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/28
 */
class DeviceConnectTipActivity : BaseDialogActivity() {

    companion object {

        /**通过蓝牙名称获取获取设备类型
         * 也可以通过硬件版本获取设备类型[com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.parseProductName]*/
        fun getDeviceType(name: String? = vmApp<LaserPeckerModel>().productInfoData.value?.deviceName): String =
            when {
                name == LaserPeckerHelper.CI ||
                        name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-CI") == true ||
                        name?.startsWith("C1") == true -> LaserPeckerHelper.CI
                name == LaserPeckerHelper.LIV ||
                        name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-IV") == true ||
                        name?.startsWith("L4") == true -> LaserPeckerHelper.LIV
                name == LaserPeckerHelper.LIII ||
                        name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-III") == true ||
                        name?.startsWith("L3") == true -> LaserPeckerHelper.LIII
                name == LaserPeckerHelper.LII ||
                        name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-II") == true ||
                        name?.startsWith("L2") == true -> LaserPeckerHelper.LII
                name == LaserPeckerHelper.LI ||
                        name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-I") == true ||
                        name?.startsWith("L1") == true -> LaserPeckerHelper.LI
                else -> LaserPeckerHelper.LI
            }

        /**根据设备名, 获取设备对应的图片资源
         * [name] 设备名, 或者蓝牙名都支持
         * */
        fun getDeviceImageRes(name: String? = vmApp<LaserPeckerModel>().productInfoData.value?.deviceName): Int =
            when (getDeviceType(name)) {
                LaserPeckerHelper.CI -> R.mipmap.device_c1
                LaserPeckerHelper.LIV -> R.mipmap.device_l4
                LaserPeckerHelper.LIII -> R.mipmap.device_l3
                LaserPeckerHelper.LII -> R.mipmap.device_l2
                LaserPeckerHelper.LI -> R.mipmap.device_l1
                else -> R.mipmap.device_l1
            }

        /**格式化蓝牙名称*/
        fun formatDeviceName(name: String? = vmApp<LaserPeckerModel>().productInfoData.value?.deviceName): String? =
            when {
                name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-CI") == true -> name.replace(
                    "${LaserPeckerHelper.PRODUCT_PREFIX}-CI",
                    "C1-"
                )
                name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-IV") == true -> name.replace(
                    "${LaserPeckerHelper.PRODUCT_PREFIX}-IV",
                    "L4-"
                )
                name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-III") == true -> name.replace(
                    "${LaserPeckerHelper.PRODUCT_PREFIX}-III",
                    "L3-"
                )
                name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-II") == true -> name.replace(
                    "${LaserPeckerHelper.PRODUCT_PREFIX}-II",
                    "L2-"
                )
                name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-I") == true -> name.replace(
                    "${LaserPeckerHelper.PRODUCT_PREFIX}-I",
                    "L1-"
                )
                else -> name
            }
    }

    init {
        activityLayoutId = R.layout.activity_device_connect_tip
        dialogGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        vmApp<LaserPeckerModel>().productInfoData.value?.apply {
            _vh.tv(R.id.device_name_view)?.text = formatDeviceName(deviceName)
            //设备图

            //动画资源
            _vh.v<LottieAnimationView>(R.id.device_image_view)?.apply {
                when (getDeviceType(deviceName)) {
                    LaserPeckerHelper.CI -> setImageResource(getDeviceImageRes(name))
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
                    else -> {
                        imageAssetsFolder = "lottie/L1/images"
                        setAnimation("lottie/L1/data.json")
                    }
                }
            }
        }

        _vh.click(R.id.finish_button) {
            finish()
        }

        _vh.click(R.id.setting_button) {
            finish()
            dslAHelper {
                start(DeviceSettingFragment::class.java)
            }
        }
    }
}