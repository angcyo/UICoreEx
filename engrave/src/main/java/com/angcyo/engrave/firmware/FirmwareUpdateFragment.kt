package com.angcyo.engrave.firmware

import android.os.Bundle
import com.angcyo.base.back
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.vmApp
import com.angcyo.engrave.R
import com.angcyo.getData
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.toast
import com.angcyo.widget.span.span

/**
 * 固件升级界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/08
 */
class FirmwareUpdateFragment : BaseDslFragment() {

    companion object {
        /**固件的扩展名*/
        const val FIRMWARE_EXT = ".lpbin"
    }

    init {
        fragmentTitle = _string(R.string.firmware_upgrade)
        enableAdapterRefresh = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vmApp<FscBleApiModel>().connectDeviceListData.observe {
            if (it.isNullOrEmpty()) {
                fragmentTitle = _string(R.string.firmware_upgrade)
            } else {
                it.first().let { deviceState ->
                    fragmentTitle = span {
                        appendLine(deviceState.device.name)
                        append(deviceState.device.address) {
                            fontSize = 12 * dpi
                        }
                    }
                }
            }
            //更新版本文本
            _adapter.updateAllItem()
        }
    }

    override fun onLoadData() {
        super.onLoadData()

        val info: String? = getData<String>() ?: activity?.intent?.getData<String>() //文件路径
        val support = info != null

        if (info != null) {
            renderDslAdapter {
                FirmwareUpdateItem()() {
                    itemFirmwareInfo = info.toFirmwareInfo()
                }
            }
        }

        if (!support) {
            toast("not support!")
            back()
        }
    }
}