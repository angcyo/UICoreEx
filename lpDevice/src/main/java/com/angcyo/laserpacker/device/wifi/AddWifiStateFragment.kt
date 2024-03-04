package com.angcyo.laserpacker.device.wifi

import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.base.removeThis
import com.angcyo.bluetooth.BluetoothModel
import com.angcyo.bluetooth.fsc.WifiApiModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.bluetooth.fsc.laserpacker.writeBleLog
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.updateItemWith
import com.angcyo.getData
import com.angcyo.getDataParcelable
import com.angcyo.http.tcp.TcpDevice
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.DeviceConnectTipActivity
import com.angcyo.laserpacker.device.model.FscDeviceModel
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiStateItem
import com.angcyo.library.L
import com.angcyo.library.component.startCountDown
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.ex.anim
import com.angcyo.library.ex.ceilInt
import com.angcyo.library.ex.toText
import com.angcyo.library.toastQQ

/**
 * 连接设备 控制/状态提示界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/31
 */
class AddWifiStateFragment : BaseDslFragment() {
    /**是否是配置ap网络*/
    var isConfigApDevice: Boolean = false

    /**选择的设备*/
    var deviceConfig: WifiConfigBean? = null

    /**ble*/
    val bleModel = vmApp<BluetoothModel>()

    init {
        fragmentTitle = _string(R.string.connect_device)
        fragmentConfig.isLightStyle = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_color))
        bigTitleLayout()

        contentLayoutId = R.layout.layout_add_wifi_state
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isConfigApDevice =
            getData<Boolean>(AddWifiConfigFragment.KEY_IS_CONFIG_AP_DEVICE) ?: isConfigApDevice
        if (isConfigApDevice) {
        } else {
            deviceConfig = getDataParcelable()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _configuringAnimator?.cancel()
        bleModel.disconnectAllDevice()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        renderDslAdapter {
            AddWifiStateItem()() {
                itemDes = _string(R.string.wifi_configuring, "$_progress%")
                itemTip = _string(R.string.add_wifi_device_tip)

                itemReconfigureAction = {
                    startConfiguring()
                }
            }
        }

        _vh.click(R.id.state_button) {
            finishWifiConfig()
        }

        if (isConfigApDevice) {

        } else if (deviceConfig == null) {
            removeThis()
            toastQQ(_string(R.string.core_thread_error_tip))
        }

        startConfiguring()
    }

    /**完成wifi配置, 退出相关界面*/
    private fun finishWifiConfig() {
        dslFHelper {
            remove(AddWifiDeviceFragment::class, AddWifiConfigFragment::class)
            remove(AddHttpApConfigFragment::class)
            removeThis()
        }
    }

    private fun toConfigState(state: Int) {
        _adapter.get<AddWifiStateItem>().firstOrNull()?.apply {
            itemState = state
        }
        if (state == AddWifiStateItem.STATE_SUCCESS) {
            _vh.visible(R.id.state_wrap_layout)

            startCountDown(3) {
                _vh.tv(R.id.state_button)?.text =
                    "${_string(R.string.ui_finish)} (${(it / 1000f).ceilInt()})"
                if (it == 0L) {
                    finishWifiConfig()
                }
            }
        } else {
            _vh.gone(R.id.state_wrap_layout)
        }
    }

    /**开始配置*/
    private fun startConfiguring() {
        startConfiguringAnimator()

        //默认状态
        toConfigState(AddWifiStateItem.STATE_NORMAL)

        if (isConfigApDevice) {
            "开始Ap配网:${HawkEngraveKeys.lastWifiSSID}:${HawkEngraveKeys.lastWifiPassword}".writeBleLog()

        } else {
            "开始配置wifi:$deviceConfig".writeBleLog()
            deviceConfig?.let { configBean ->
                bleModel.connect(configBean.device) { connected ->
                    "Ble连接设备[${configBean.device.name}]:$connected".writeBleLog()
                    if (connected) {
                        sendConfig(configBean)
                    } else {
                        //超时, 连接失败
                        toConfigState(AddWifiStateItem.STATE_ERROR)
                    }
                }
            }
        }
    }

    private fun sendConfig(configBean: WifiConfigBean) {
        val bleName = DeviceConnectTipActivity.formatDeviceName(configBean.device.name)
        if (DeviceConnectTipActivity.isWifiDevice(configBean.device.name)) {
            val deviceConfig = _deviceSettingBean!!
            bleModel.writeAndListener(
                configBean.device,
                deviceConfig.lp5BleServiceUuid,
                deviceConfig.lp5BleWriteUuid,
                deviceConfig.lp5BleNotifyUuid,
                "A1=\"${configBean.name}\",\"${configBean.password}\"".toByteArray()
            ) { data, exception ->
                if (exception == null && data != null) {
                    val text = data.toText()
                    "蓝牙配网成功[${configBean.device.device}]:${configBean}:$text".writeToLog(
                        logLevel = L.INFO
                    )
                    if (text.isBlank() || text == "B1.1") {
                        toConfigState(AddWifiStateItem.STATE_ERROR)
                    } else {
                        val ip = text.substring(5, text.length)
                        connectWifiDevice(TcpDevice(ip, HawkEngraveKeys.wifiPort, bleName))
                    }
                } else {
                    toConfigState(AddWifiStateItem.STATE_ERROR)
                }
            }
        } else {
            "非wifi设备[${configBean.device.name}]".writeBleLog()
            toConfigState(AddWifiStateItem.STATE_ERROR)
        }
    }

    /**连接到wifi设备*/
    private fun connectWifiDevice(tcpDevice: TcpDevice) {
        HawkEngraveKeys.lastWifiIp = tcpDevice.address
        vmApp<WifiApiModel>().connect(tcpDevice, null)
        toConfigState(AddWifiStateItem.STATE_SUCCESS)
        FscDeviceModel.disableAutoConnect(false)
    }

    private var _progress: Int = 0
    private var _configuringAnimator: ValueAnimator? = null

    private fun startConfiguringAnimator() {
        _configuringAnimator?.cancel()
        _progress = 0
        _configuringAnimator = anim(0, 100) {
            animatorDuration = 30_000
            onAnimatorUpdateValue = { value, fraction ->
                _progress = value as Int
                _adapter.updateItemWith<AddWifiStateItem>()?.apply {
                    if (itemState == AddWifiStateItem.STATE_NORMAL) {
                        itemDes =
                            _string(R.string.wifi_configuring, "$_progress%")
                    } else {
                        _configuringAnimator?.cancel()
                    }
                }
            }
        }
    }

}