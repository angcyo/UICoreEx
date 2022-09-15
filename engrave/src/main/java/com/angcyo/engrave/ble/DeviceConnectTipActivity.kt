package com.angcyo.engrave.ble

import android.os.Bundle
import android.view.Gravity
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

        /**根据设备名, 获取设备对应的图片资源
         * [name] 设备名, 或者蓝牙名都支持
         * */
        fun getDeviceImageRes(name: String?): Int = when {
            name == LaserPeckerHelper.CI || name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-CI") == true -> R.mipmap.device_c1
            name == LaserPeckerHelper.LIII || name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-III") == true -> R.mipmap.device_l3
            name == LaserPeckerHelper.LII || name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-II") == true -> R.mipmap.device_l2
            name == LaserPeckerHelper.LI || name?.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX}-I") == true -> R.mipmap.device_l1
            else -> R.mipmap.device_l1
        }
    }

    init {
        activityLayoutId = R.layout.activity_device_connect_tip
        dialogGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        vmApp<LaserPeckerModel>().productInfoData.value?.apply {
            _vh.tv(R.id.device_name_view)?.text = deviceName
            //设备图
            _vh.img(R.id.device_image_view)?.setImageResource(getDeviceImageRes(name))
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