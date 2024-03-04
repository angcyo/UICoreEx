package com.angcyo.laserpacker.device.wifi

import android.app.Application
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.base.dslFHelper
import com.angcyo.bluetooth.BluetoothModel
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.WifiApiModel
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.fragment.bigTitleLayout
import com.angcyo.core.vmApp
import com.angcyo.dialog.normalDialog
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.allSelectedItem
import com.angcyo.dsladapter.findAllItem
import com.angcyo.dsladapter.findItem
import com.angcyo.dsladapter.hideItemBy
import com.angcyo.dsladapter.paddingVertical
import com.angcyo.dsladapter.removeItem
import com.angcyo.dsladapter.renderEmptyItem
import com.angcyo.item.DslLabelTextItem
import com.angcyo.item.style.itemText
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.laserpacker.device.ble.DeviceConnectTipActivity
import com.angcyo.laserpacker.device.ble.dslitem.BluetoothDeviceItem
import com.angcyo.laserpacker.device.model.FscDeviceModel
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiEmptyItem
import com.angcyo.laserpacker.device.wifi.dslitem.AddWifiRadarScanItem
import com.angcyo.library.app
import com.angcyo.library.ex._color
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._string
import com.angcyo.library.ex.cancelAnimator
import com.angcyo.library.ex.copyDrawable
import com.angcyo.library.ex.gone
import com.angcyo.library.ex.havePermission
import com.angcyo.library.ex.infinite
import com.angcyo.library.ex.rotateAnimation
import com.angcyo.library.ex.toApplicationDetailsSettings
import com.angcyo.putDataParcelable
import com.angcyo.viewmodel.observe
import com.clj.fastble.data.BleDevice

/**
 * wifi配网 选择设备界面
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/07/30
 */
class AddWifiDeviceFragment : BaseDslFragment() {

    /**ble*/
    val bleModel = vmApp<BluetoothModel>()

    /**是否显示rssi信号强度*/
    var showRssi: Boolean = BluetoothSearchHelper.SHOW_RSSI

    init {
        fragmentTitle = _string(R.string.add_wifi_device_title)
        fragmentConfig.isLightStyle = true
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_color))
        bigTitleLayout()

        contentLayoutId = R.layout.layout_add_wifi_device

        //2023-8-1 lp5 ble
        vmApp<FscBleApiModel>().disconnectAll()
        vmApp<WifiApiModel>().disconnectAll()
        BluetoothModel.init(app() as Application)

        //2023-8-21 禁用自动连接
        FscDeviceModel.disableAutoConnect()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        appendRightItem(ico = R.drawable.lib_refresh, action = {
            gone()
        }) {
            toggleScan()
        }

        _vh.click(R.id.bind_wifi_button) {
            dslFHelper {
                show(AddWifiConfigFragment::class) {
                    (_adapter.allSelectedItem().firstOrNull() as? BluetoothDeviceItem)
                        ?.itemBleDevice?.let {
                            putDataParcelable(it)
                        }
                }
            }
        }
    }

    /**[com.angcyo.laserpacker.device.ble.BluetoothSearchHelper.Companion.checkAndSearchDevice]*/
    override fun onFragmentFirstShow(bundle: Bundle?) {
        super.onFragmentFirstShow(bundle)
        dslPermissions(BluetoothModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
            if (allGranted) {
                observeState()
                renderScanLayout()
            } else {
                //权限被禁用, 显示权限跳转提示框
                //toast(_string(R.string.permission_disabled))
                context.normalDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = _string(R.string.ble_permission_disabled)

                    positiveButton(_string(R.string.ui_enable_permission)) { dialog, dialogViewHolder ->
                        dialog.dismiss()
                        context.toApplicationDetailsSettings()
                    }
                }
            }
        }
    }

    override fun onFragmentNotFirstShow(bundle: Bundle?) {
        super.onFragmentNotFirstShow(bundle)
        if (fContext().havePermission(BluetoothModel.bluetoothPermissionList())) {
            if (_adapter.isEmpty()) {
                observeState()
                renderScanLayout()
            }
        }
    }

    /* fun DslAdapter.renderBluetoothDeviceItem(device: FscDevice) {
         BluetoothDeviceItem()() {
             itemAnimateRes = R.anim.item_translate_to_left_animation
             itemData = device
             itemFscDevice = device
             itemShowRssi = showRssi

             observeItemChange {
                 updateBindWrapLayout()
             }
         }
     } */

    fun DslAdapter.renderBluetoothDeviceItem(device: BleDevice) {
        BluetoothDeviceItem()() {
            itemAnimateRes = R.anim.item_translate_to_left_animation
            itemData = device
            itemBleDevice = device
            itemShowRssi = showRssi

            observeItemChange {
                updateBindWrapLayout()
            }
        }
    }

    /**切换扫描状态*/
    fun toggleScan() {
        if (bleModel.bluetoothStateData.value == BluetoothModel.BLUETOOTH_STATE_SCANNING) {
            bleModel.stopScan()
        } else {
            requestScan()
        }
    }

    /**请求扫描*/
    fun requestScan() {
        if (_adapter.get<BluetoothDeviceItem>().isEmpty()) {
            //没有设备, 显示扫描中的界面
            renderScanLayout()
        } else {
            bleModel.startScan()
        }
    }

    fun updateBindWrapLayout() {
        _vh.visible(R.id.bind_wifi_wrap_layout, _adapter.get<BluetoothDeviceItem>().isNotEmpty())
        _vh.enable(R.id.bind_wifi_button, _adapter.allSelectedItem().isNotEmpty())
    }

    //region ---界面控制---

    fun showRefreshView(show: Boolean = true) {
        rightControl()?.goneIndex(0, !show)
    }

    /**状态监听*/
    fun observeState() {
        //蓝牙状态监听
        bleModel.bluetoothStateData.observe(this, allowBackward = false) { state ->
            //loading
            state?.let {
                rightControl()?.get(0)?.apply {
                    if (state == BluetoothModel.BLUETOOTH_STATE_SCANNING) {
                        //animate().rotationBy(360f).setDuration(240).start()
                        rotateAnimation(duration = 1000, config = {
                            infinite()
                        })
                    } else {
                        cancelAnimator()
                    }
                }
                _adapter.render { hideItemBy(state != BluetoothModel.BLUETOOTH_STATE_SCANNING) { it is DslLabelTextItem } }

                //停止扫描后
                if (state == BluetoothModel.BLUETOOTH_STATE_FINISH) {
                    val list = _adapter.findAllItem<BluetoothDeviceItem>()
                    if (list.isNullOrEmpty()) {
                        _adapter.render {
                            removeItem { it is AddWifiRadarScanItem }
                            removeItem { it is AddWifiEmptyItem }
                            AddWifiEmptyItem()() {
                                itemRefreshActon = {
                                    requestScan()
                                }
                            }
                        }
                    }
                }
            }
        }

        bleModel.bluetoothDeviceData.observe { device ->
            device?.let {
                _adapter.render {
                    //移除旧的item
                    val find =
                        findItem(false) { it is BluetoothDeviceItem && it.itemBleDevice?.device?.address == device.device.address }

                    val isWifiDevice = DeviceConnectTipActivity.isWifiDevice(device.name)
                    if (isWifiDevice) {
                        removeItem { it is AddWifiRadarScanItem }
                        removeItem { it is AddWifiEmptyItem }
                    }

                    //过滤
                    if (find == null) {
                        //LP5 LX2 才有WIFI模块
                        if (isWifiDevice) {
                            //添加新的item
                            renderBluetoothDeviceItem(device)
                        }
                    } else {
                        (find as BluetoothDeviceItem).apply {
                            itemData = device
                            itemBleDevice = device
                            itemUpdateFlag = true
                        }
                    }
                }
            }
        }
    }

    /**渲染扫描中的界面, 权限已给*/
    fun renderScanLayout() {
        fragmentTitle = _string(R.string.add_wifi_device_title)
        showRefreshView()
        renderDslAdapter(true) {
            DslLabelTextItem()() {
                itemBackgroundDrawable = fragmentConfig.fragmentBackgroundDrawable?.copyDrawable()
                itemText = _string(R.string.add_wifi_device_scan_tip)
                paddingVertical(_dimen(R.dimen.lib_hdpi))
            }
            renderEmptyItem(_dimen(R.dimen.lib_xhdpi))
            AddWifiRadarScanItem()()
        }
        bleModel.startScan()
    }

    //endregion ---界面控制---

}