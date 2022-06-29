package com.angcyo.engrave.ble

import android.app.Dialog
import android.content.Context
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.FscBleApiModel.Companion.BLUETOOTH_STATE_SCANNING
import com.angcyo.bluetooth.fsc.core.DeviceConnectState.Companion.CONNECT_STATE_SUCCESS
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.core.vmApp
import com.angcyo.dialog.BaseDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dsladapter.*
import com.angcyo.engrave.R
import com.angcyo.library.ex._string
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.loading.RadarScanLoadingView
import com.angcyo.widget.loading.TGStrokeLoadingView
import com.angcyo.widget.recycler.renderDslAdapter

/**
 * SPP模式蓝牙搜索列表界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/26
 */
class BluetoothSearchListDialogConfig(context: Context? = null) : BaseDialogConfig(context) {

    /**连接成功后, 是否关闭界面*/
    var connectedDismiss: Boolean = false

    val apiModel = vmApp<FscBleApiModel>()

    init {
        dialogLayoutId = R.layout.dialog_bluetooth_search_list_layout
        dialogTitle = _string(R.string.bluetooth_ft_blue_connect_act)
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
                        render {

                            //移除旧的item

                            val find =
                                findItem { it is BluetoothConnectItem && it.itemFscDevice == device }

                            //过滤
                            if (find == null) {
                                if (device.name?.startsWith(LaserPeckerHelper.PRODUCT_PREFIX) == true) {
                                    //添加新的item
                                    BluetoothConnectItem()() {
                                        itemAnimateRes = R.anim.item_translate_to_left_animation
                                        itemData = device
                                        itemFscDevice = device
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
                        }
                    }
                }
                startScan()
            }
        }

        //蓝牙状态监听
        apiModel.bleStateData.observe(this) {
            //loading
            dialogViewHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)
                ?.loading(it == BLUETOOTH_STATE_SCANNING)

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
}

/**蓝牙搜索列表对话框*/
fun Context.bluetoothSearchListDialog(config: BluetoothSearchListDialogConfig.() -> Unit) {
    return BluetoothSearchListDialogConfig(this).run {
        configBottomDialog(this@bluetoothSearchListDialog)
        config()
        show()
    }
}

