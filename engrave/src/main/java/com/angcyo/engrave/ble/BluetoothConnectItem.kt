package com.angcyo.engrave.ble

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.library.ex._string
import com.angcyo.library.ex.visible

import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.loading.TGStrokeLoadingView
import com.feasycom.common.bean.FscDevice

/**
 * 蓝牙设备连接的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */
class BluetoothConnectItem : DslAdapterItem() {

    /**设备*/
    var itemFscDevice: FscDevice? = null

    val fscApi = vmApp<FscBleApiModel>()

    init {
        itemLayoutId = R.layout.item_bluetooth_connect_layout
        itemSingleSelectMutex = true

        itemClick = {
            if (fscApi.isConnectState(itemFscDevice)) {
                fscApi.disconnect(itemFscDevice)
            } else {
                fscApi.connect(itemFscDevice, true)
            }
        }
    }

    override fun onSetItemSelected(select: Boolean) {
        super.onSetItemSelected(select)
        if (!select) {
            fscApi.disconnect(itemFscDevice)
        }
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        val connectState = fscApi.connectState(itemFscDevice)
        itemIsSelected = connectState == DeviceConnectState.CONNECT_STATE_SUCCESS
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.selected(itemIsSelected)

        itemHolder.tv(R.id.device_name_view)?.text = buildString {
            appendLine(itemFscDevice?.name)
            append(itemFscDevice?.address)
        }

        when (connectState) {
            DeviceConnectState.CONNECT_STATE_SUCCESS -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_ft_connected)
            }
            DeviceConnectState.CONNECT_STATE_START -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(true)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_ft_blue_connecting)
            }
            DeviceConnectState.CONNECT_STATE_DISCONNECT_START -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(true)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_ft_blue_disconnecting)
            }
            /*DeviceConnectState.CONNECT_STATE_DISCONNECT -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_lib_scan_disconnected)
            }*/
            else -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_ft_mtu_no_device_connected)
            }
        }
    }
}