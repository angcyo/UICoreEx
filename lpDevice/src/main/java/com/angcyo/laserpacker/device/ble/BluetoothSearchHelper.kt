package com.angcyo.laserpacker.device.ble

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.component.model.LanguageModel
import com.angcyo.core.vmApp
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dialog.normalDialog
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterStatusItem
import com.angcyo.dsladapter._dslAdapter
import com.angcyo.dsladapter.filter.SortAfterFilterInterceptor
import com.angcyo.dsladapter.findItem
import com.angcyo.dsladapter.itemIndexPosition
import com.angcyo.dsladapter.renderAdapterEmptyStatus
import com.angcyo.dsladapter.updateAdapterState
import com.angcyo.dsladapter.updateItem
import com.angcyo.item.component.SearchAdapterFilter
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.dslitem.BluetoothConnectItem
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._string
import com.angcyo.library.ex.cancelAnimator
import com.angcyo.library.ex.find
import com.angcyo.library.ex.infinite
import com.angcyo.library.ex.isDebug
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.rotateAnimation
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toApplicationDetailsSettings
import com.angcyo.library.utils.Device
import com.angcyo.viewmodel.observe
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.loading.RadarScanLoadingView
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.tab
import com.feasycom.common.bean.FscDevice
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 蓝牙搜索界面辅助类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/21
 */
class BluetoothSearchHelper {

    companion object {

        /**最后一次搜索的时间*/
        var last_search_time: Long = 0

        /**是否显示信号强度*/
        var SHOW_RSSI = isDebug()

        /**联系客服*/
        var ON_CONTACT_ME_ACTION: Action? = null

        /**指定的蓝牙名称, 是否是LP设备*/
        fun isLpBluetoothDevice(deviceName: String?): Boolean {
            deviceName ?: return false
            val name = deviceName.lowercase()
            if (name.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX} ".lowercase())) {
                //LaserPecker BXX 这种情况
                return false
            }
            if (name.startsWith(LaserPeckerHelper.PRODUCT_PREFIX.lowercase())) {
                //LaserPeckerXXX 这种情况
                return true
            }
            if (name.startsWith("lp".lowercase())) {
                //LPXXX 这种情况
                return true
            }
            if (name.startsWith("lx".lowercase())) {
                //LXXXX 这种情况
                return true
            }
            return false
        }

        /**检查蓝牙权限, 并显示搜索对话框
         * [fragment] [fragmentActivity] 二选一
         *
         * [com.angcyo.laserpacker.device.wifi.AddWifiDeviceFragment.onFragmentFirstShow]
         * */
        fun checkAndSearchDevice(
            fragment: Fragment? = null,
            fragmentActivity: FragmentActivity? = null,
            dialogConfig: BluetoothSearchListDialogConfig.() -> Unit = {}
        ) {
            val context = fragment?.requireContext() ?: fragmentActivity ?: return

            fun permissionsResult(allGranted: Boolean) {
                if (allGranted) {
                    context.bluetoothSearchListDialog {
                        connectedDismiss = true
                        dialogConfig()
                    }
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

            fragment?.dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                permissionsResult(allGranted)
            }
            fragmentActivity?.dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                permissionsResult(allGranted)
            }
        }

        /**保证有设备连接后, 才能进行的操作. 否则显示搜索并连接设备的界面*/
        fun wrapDeviceAction(
            fragment: Fragment? = null,
            fragmentActivity: FragmentActivity? = null,
            action: Action
        ) {
            if (vmApp<DeviceStateModel>().isDeviceConnect()) {
                //已经连接了设备
                action()
                return
            }

            //没有连接设备, 显示搜索界面
            checkAndSearchDevice(fragment, fragmentActivity) {
                //连接成功后, 关闭界面
                connectedDismiss = true
            }
        }
    }

    /**连接成功后, 是否关闭界面*/
    var connectedDismiss: Boolean = false

    /**是否显示rssi信号强度*/
    var showRssi: Boolean = SHOW_RSSI

    val apiModel = vmApp<FscBleApiModel>()

    /**联系客服*/
    var onContactMeAction = {
        //toast("Features under development...")
        ON_CONTACT_ME_ACTION?.invoke()
    }

    /**设备过滤器*/
    val deviceFilter = SearchAdapterFilter()

    /**排序过滤器*/
    val sortFilter = SortAfterFilterInterceptor(true, false) {
        if (it is BluetoothConnectItem) {
            it.itemUpdateFlag = it.itemIndexPosition() < 2
            it.itemFscDevice?.rssi
        } else {
            0
        }
    }

    /**初始化布局*/
    @CallPoint
    fun initLayout(
        lifecycleOwner: LifecycleOwner,
        viewHolder: DslViewHolder,
        targetWindow: TargetWindow?
    ) {
        viewHolder.click(R.id.lib_loading_view) {
            toggleScan()
        }

        //扫描
        viewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {
            deviceFilter.init(this)
            sortFilter.install(this)

            renderAdapterEmptyStatus(R.layout.bluetooth_empty_layout) { itemHolder, state ->
                itemHolder.click(R.id.contact_me_view) {
                    onContactMeAction()
                }
                if (state == DslAdapterStatusItem.ADAPTER_STATUS_EMPTY) {
                    //未搜索到设备
                    UMEvent.APP_ERROR.umengEventValue {
                        put(UMEvent.KEY_NO_DEVICE_ERROR, "未搜索到设备")
                    }
                }
            }

            val list = apiModel.connectDeviceListData.value
            if (list.isNullOrEmpty()) {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
            } else {
                list.forEach {
                    renderBluetoothConnectItem(it.device)
                }
            }

            apiModel.apply {
                useSppModel = true

                //监听蓝牙设备发现
                bleDeviceData.observe(lifecycleOwner) { device ->
                    device?.let {
                        render(false) {

                            //移除旧的item
                            val find =
                                findItem(false) { it is BluetoothConnectItem && it.itemFscDevice == device }

                            //过滤
                            if (find == null) {
                                if (isLpBluetoothDevice(device.name) &&
                                    !DeviceConnectTipActivity.isWifiDevice(device.name)
                                ) {
                                    //添加新的item
                                    renderBluetoothConnectItem(device)

                                    autoAdapterStatus()
                                }
                            } else {
                                (find as BluetoothConnectItem).apply {
                                    itemData = device
                                    itemFscDevice = device
                                    itemUpdateFlag = true
                                }
                            }

                            //filter
                            checkAndShowFilterLayout(viewHolder, this)
                        }
                    }
                }
                startScan()

                //开始扫描的时间
                UMEvent.SEARCH_DEVICE.umengEventValue {
                    last_search_time = nowTime()
                    put(UMEvent.KEY_START_TIME, last_search_time.toString())
                    put(UMEvent.KEY_TIME_ZONE, LanguageModel.timeZoneId)
                    put(UMEvent.KEY_PHONE_API, "${Device.api}")
                    put(UMEvent.KEY_PHONE_DEVICE, Device.deviceName)
                    put(UMEvent.KEY_PHONE_LANGUAGE, LanguageModel.getCurrentLanguageTag())
                    put(
                        UMEvent.KEY_PHONE_NAME,
                        "${Device.deviceName} ${Device.api} ${LanguageModel.getCurrentLanguageTag()} ${LanguageModel.timeZoneId}"
                    )
                }
            }
        }

        //蓝牙状态监听
        apiModel.bleStateData.observe(lifecycleOwner, allowBackward = false) { state ->
            //loading
            viewHolder.view(R.id.lib_loading_view)?.apply {
                if (state == FscBleApiModel.BLUETOOTH_STATE_SCANNING) {
                    //animate().rotationBy(360f).setDuration(240).start()
                    rotateAnimation(duration = 1000, config = {
                        infinite()
                    })
                } else {
                    cancelAnimator()
                }
            }

            //radar
            viewHolder.v<RadarScanLoadingView>(R.id.radar_scan_loading_view)
                ?.loading(state == FscBleApiModel.BLUETOOTH_STATE_SCANNING)
            viewHolder.visible(
                R.id.radar_scan_loading_view,
                state == FscBleApiModel.BLUETOOTH_STATE_SCANNING
            )

            //state
            if (state == FscBleApiModel.BLUETOOTH_STATE_STOP) {
                viewHolder.rv(R.id.lib_recycler_view)?._dslAdapter?.updateAdapterState()
            }
        }

        //连接设备变化监听
        apiModel.connectDeviceListData.observe(lifecycleOwner) {
            viewHolder.rv(R.id.lib_recycler_view)?._dslAdapter?.updateAllItem()
        }

        //连接状态变化监听
        apiModel.connectStateData.observe(lifecycleOwner) { state ->
            if (state != null) {
                viewHolder.rv(R.id.lib_recycler_view)?._dslAdapter?.updateItem {
                    it is BluetoothConnectItem && it.itemFscDevice == state.device
                }
                if (state.state == DeviceConnectState.CONNECT_STATE_SUCCESS) {
                    //读取设备版本, 移至: com.angcyo.laserpacker.device.model.FscDeviceModel.initDevice
                    if (connectedDismiss) {
                        targetWindow?.dismissWindow()
                    }
                }
            }
        }
    }

    fun DslAdapter.renderBluetoothConnectItem(device: FscDevice) {
        BluetoothConnectItem()() {
            itemAnimateRes = R.anim.item_translate_to_left_animation
            itemData = device
            itemFscDevice = device
            itemShowRssi = showRssi
        }
    }

    /**当前需要过滤的固件名*/
    var _filterName: String? = null

    /**检查是否需要显示设备过滤布局, 当不同类型的设备数量大于指定数量时, 显示过滤布局*/
    fun checkAndShowFilterLayout(viewHolder: DslViewHolder, adapter: DslAdapter) {
        val filterNameList = mutableSetOf<String>()
        adapter.adapterItems.forEach {
            if (it is BluetoothConnectItem) {
                it.itemFscDevice?.let {
                    val deviceName = DeviceConnectTipActivity.formatDeviceName(it.name)
                    deviceName?.split("-")?.getOrNull(0)?.let {
                        filterNameList.add(it)
                    }
                }
            }
        }
        val showFilterLayout =
            adapter.adapterItems.size() >= HawkEngraveKeys.showDeviceFilterCount && filterNameList.size() >= 2
        if (showFilterLayout) {
            sortFilter.isEnable = true
            //有2种设备, 并且数量很多
            viewHolder.visible(R.id.device_filter_tab_layout)
            viewHolder.tab(R.id.device_filter_tab_layout)?.apply {
                tabDefaultIndex = filterNameList.indexOf(_filterName)
                dslSelector.dslSelectorConfig.dslMinSelectLimit = -1
                resetChild(
                    filterNameList.toList(),
                    R.layout.lib_segment_layout
                ) { itemView, item, itemIndex ->
                    itemView.find<TextView>(R.id.lib_text_view)?.text = item
                }
                observeIndexChange { fromIndex, toIndex, reselect, fromUser ->
                    if (fromUser) {
                        _filterName = filterNameList.toList().getOrNull(toIndex)
                        adapter.changingAllItem()
                        deviceFilter.filter(_filterName)
                    }
                }
            }
        } else {
            sortFilter.isEnable = false
            _filterName = null
            deviceFilter.filter(_filterName)
            viewHolder.gone(R.id.device_filter_tab_layout)
        }
    }

    /**切换扫描状态*/
    fun toggleScan() {
        if (apiModel.bleStateData.value == FscBleApiModel.BLUETOOTH_STATE_SCANNING) {
            apiModel.stopScan()
        } else {
            apiModel.startScan()
        }
    }

    /**停止扫描*/
    fun stopScan() {
        apiModel.stopScan()
    }


}