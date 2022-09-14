package com.angcyo.engrave.ble

import android.os.Bundle
import android.view.Gravity
import com.angcyo.activity.BaseDialogActivity
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.R
import com.angcyo.library.ex._string
import com.angcyo.transition.dslTransition
import com.angcyo.widget.TextPathAnimateView

/**
 * 设备自动连接后的通知提示界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/28
 */
class DeviceConnectTipActivity : BaseDialogActivity() {

    init {
        activityLayoutId = R.layout.activity_device_connect_tip
        dialogGravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        _vh.postDelay(360L) {
            _vh.v<TextPathAnimateView>(R.id.text_animate_view)?.start()
        }

        _vh.postDelay(2_000L) {
            _vh.tv(R.id.lib_des_view)?.text = buildString {
                append(_string(R.string.device_connected))
                vmApp<LaserPeckerModel>().productInfoData.value?.deviceName?.let {
                    append(" ")
                    append(it)
                }
                //append(" LaserPecker ")
                //vmApp<LaserPeckerModel>().productInfoData.value?.name?.let { append(it) }
            }
            dslTransition(_vh.group(R.id.lib_dialog_root_layout)) {
                onCaptureEndValues = {
                    _vh.visible(R.id.lib_des_view)
                    _vh.visible(R.id.control_layout)
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