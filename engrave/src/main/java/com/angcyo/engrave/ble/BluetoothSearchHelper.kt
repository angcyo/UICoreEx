package com.angcyo.engrave.ble

import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.vmApp
import com.angcyo.dialog.TargetWindow
import com.angcyo.dialog.dismissWindow
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.filter.SortAfterFilterInterceptor
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.dslitem.BluetoothConnectItem
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.component.SearchAdapterFilter
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.*
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
        var SHOW_RSSI = isDebugType()

        /**联系客服*/
        var ON_CONTACT_ME_ACTION: Action? = null
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
                                val name = device.name?.lowercase() ?: ""
                                if (name.isNotBlank() &&
                                    name.startsWith(LaserPeckerHelper.PRODUCT_PREFIX.lowercase()) &&
                                    !name.startsWith("${LaserPeckerHelper.PRODUCT_PREFIX} ".lowercase()) //LaserPecker Bxx 这种情况
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
                }
            }
        }

        //蓝牙状态监听
        apiModel.bleStateData.observe(lifecycleOwner, allowBackward = false) {
            //loading
            viewHolder.view(R.id.lib_loading_view)?.apply {
                if (it == FscBleApiModel.BLUETOOTH_STATE_SCANNING) {
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
                ?.loading(it == FscBleApiModel.BLUETOOTH_STATE_SCANNING)
            viewHolder.visible(
                R.id.radar_scan_loading_view,
                it == FscBleApiModel.BLUETOOTH_STATE_SCANNING
            )

            //state
            if (it == FscBleApiModel.BLUETOOTH_STATE_STOP) {
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
                    //读取设备版本, 移至: com.angcyo.engrave.model.FscDeviceModel.initDevice
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
        if (adapter.adapterItems.size() >= HawkEngraveKeys.showDeviceFilterCount && filterNameList.size() >= 2) {
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