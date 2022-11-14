package com.angcyo.engrave.ble

import android.app.Dialog
import android.content.Context
import android.widget.TextView
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.FscBleApiModel.Companion.BLUETOOTH_STATE_SCANNING
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_SUCCESS
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.vmApp
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.filter.SortAfterFilterInterceptor
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.dslitem.BluetoothConnectItem
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.component.SearchAdapterFilter
import com.angcyo.library.ex.*
import com.angcyo.viewmodel.observe
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.resetChild
import com.angcyo.widget.loading.RadarScanLoadingView
import com.angcyo.widget.recycler.renderDslAdapter
import com.angcyo.widget.tab
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * SPP模式蓝牙搜索列表界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */
class BluetoothSearchListDialogConfig(context: Context? = null) : BaseDialogConfig(context) {

    companion object {

        /**最后一次搜索的时间*/
        var last_search_time: Long = 0

        /**联系客服*/
        var ON_CONTACT_ME_ACTION: Action? = null
    }

    /**连接成功后, 是否关闭界面*/
    var connectedDismiss: Boolean = false

    /**是否显示rssi信号强度*/
    var showRssi: Boolean = isDebugType()

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

    init {
        dialogLayoutId = R.layout.dialog_bluetooth_search_list_layout
        dialogTitle = _string(R.string.blue_connect)
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.initDialogView(dialog, dialogViewHolder)

        dialogViewHolder.click(R.id.lib_loading_view) {
            if (apiModel.bleStateData.value == BLUETOOTH_STATE_SCANNING) {
                apiModel.stopScan()
            } else {
                apiModel.startScan()
            }
        }

        //扫描
        dialogViewHolder.rv(R.id.lib_recycler_view)?.renderDslAdapter {
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
                    BluetoothConnectItem()() {
                        itemAnimateRes = R.anim.item_translate_to_left_animation
                        itemData = it.device
                        itemFscDevice = it.device
                    }
                }
            }

            apiModel.apply {
                useSppModel = true

                //监听蓝牙设备发现
                bleDeviceData.observe(this@BluetoothSearchListDialogConfig) { device ->
                    device?.let {
                        render(false) {

                            //移除旧的item

                            val find =
                                findItem(false) { it is BluetoothConnectItem && it.itemFscDevice == device }

                            //过滤
                            if (find == null) {
                                if (device.name?.startsWith(LaserPeckerHelper.PRODUCT_PREFIX) == true) {
                                    //添加新的item
                                    BluetoothConnectItem()() {
                                        itemAnimateRes = R.anim.item_translate_to_left_animation
                                        itemData = device
                                        itemFscDevice = device
                                        itemShowRssi = showRssi
                                    }

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
                            checkAndShowFilterLayout(dialogViewHolder, this)
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
        apiModel.bleStateData.observe(this, allowBackward = false) {
            //loading
            dialogViewHolder.view(R.id.lib_loading_view)?.apply {
                if (it == BLUETOOTH_STATE_SCANNING) {
                    //animate().rotationBy(360f).setDuration(240).start()
                    rotateAnimation(duration = 1000, config = {
                        infinite()
                    })
                } else {
                    cancelAnimator()
                }
            }

            //radar
            dialogViewHolder.v<RadarScanLoadingView>(R.id.radar_scan_loading_view)
                ?.loading(it == BLUETOOTH_STATE_SCANNING)
            dialogViewHolder.visible(R.id.radar_scan_loading_view, it == BLUETOOTH_STATE_SCANNING)

            //state
            if (it == FscBleApiModel.BLUETOOTH_STATE_STOP) {
                dialogViewHolder.rv(R.id.lib_recycler_view)?._dslAdapter?.updateAdapterState()
            }
        }

        //连接设备变化监听
        apiModel.connectDeviceListData.observe(this) {
            dialogViewHolder.rv(R.id.lib_recycler_view)?._dslAdapter?.updateAllItem()
        }

        //连接状态变化监听
        apiModel.connectStateData.observe(this) { state ->
            if (state != null) {
                dialogViewHolder.rv(R.id.lib_recycler_view)?._dslAdapter?.updateItem {
                    it is BluetoothConnectItem && it.itemFscDevice == state.device
                }
                if (state.state == CONNECT_STATE_SUCCESS) {
                    //读取设备版本, 移至: com.angcyo.engrave.model.FscDeviceModel.initDevice
                    if (connectedDismiss) {
                        dialog.dismiss()
                    }
                }
            }
        }
    }

    override fun onDialogCancel(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogCancel(dialog, dialogViewHolder)
    }

    override fun onDialogDestroy(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogDestroy(dialog, dialogViewHolder)
        apiModel.stopScan()
    }

    /**当前需要过滤的固件名*/
    var _filterName: String? = null

    /**检查是否需要显示设备过滤布局, 当不同类型的设备数量大于指定数量时, 显示过滤布局*/
    fun checkAndShowFilterLayout(dialogViewHolder: DslViewHolder, adapter: DslAdapter) {
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
            dialogViewHolder.visible(R.id.device_filter_tab_layout)
            dialogViewHolder.tab(R.id.device_filter_tab_layout)?.apply {
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
            dialogViewHolder.gone(R.id.device_filter_tab_layout)
        }
    }
}

/**蓝牙搜索列表对话框*/
fun Context.bluetoothSearchListDialog(config: BluetoothSearchListDialogConfig.() -> Unit) {
    return BluetoothSearchListDialogConfig(this).run {
        configBottomDialog(this@bluetoothSearchListDialog)
        // dialogThemeResId = R.style.LibDialogBaseFullTheme
        config()
        show()
    }
}

