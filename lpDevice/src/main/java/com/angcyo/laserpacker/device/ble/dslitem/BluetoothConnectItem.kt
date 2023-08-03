package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.http.tcp.Tcp
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.visible
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.image.ImageLoadingView
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 蓝牙设备连接的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */
class BluetoothConnectItem : BluetoothDeviceItem() {

    init {
        itemClick = {
            if (_isWifiDevice) {
                //wifi设备
                if (wifiApi.connectState(itemTcpDevice) == Tcp.CONNECT_STATE_CONNECT_SUCCESS) {
                    //已经连接
                    wifiApi.disconnect(true)
                } else {
                    //未连接
                    wifiApi.stopScan()
                    fscApi.disconnectAll(true) //断开所有蓝牙连接
                    wifiApi.connect(itemTcpDevice!!, false)
                    onSearchFinish()
                }
            } else {
                //蓝牙设备
                if (fscApi.isConnectState(itemFscDevice)) {
                    fscApi.disconnect(itemFscDevice)
                } else {
                    wifiApi.disconnectAll(true) //断开所有Wifi连接
                    fscApi.connect(itemFscDevice, false, true)
                    onSearchFinish()
                }
            }
        }
    }

    private fun onSearchFinish() {
        //连接后, 搜索结束
        UMEvent.SEARCH_DEVICE.umengEventValue {
            val nowTime = nowTime()
            put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
            put(
                UMEvent.KEY_DURATION,
                (nowTime - BluetoothSearchHelper.last_search_time).toString()
            )
        }
    }

    override fun onSetItemSelected(select: Boolean) {
        super.onSetItemSelected(select)
        if (!select) {
            if (itemSelectMutexFromItem != this) {
                fscApi.disconnect(itemFscDevice)
                //wifiApi.disconnect(itemTcpDevice, false)
            }
        }
    }

    /**是否连接成功*/
    private val isConnectSuccess: Boolean
        get() = if (_isWifiDevice) {
            wifiApi.connectState(itemTcpDevice) == Tcp.CONNECT_STATE_CONNECT_SUCCESS
        } else {
            fscApi.connectState(itemFscDevice) == DeviceConnectState.CONNECT_STATE_SUCCESS
        }

    /**是否连接开始了*/
    private val isConnectStart: Boolean
        get() = if (_isWifiDevice) {
            wifiApi.connectState(itemTcpDevice) == Tcp.CONNECT_STATE_CONNECTING
        } else {
            fscApi.connectState(itemFscDevice) == DeviceConnectState.CONNECT_STATE_START
        }

    /**是否断开开始了*/
    private val isDisconnectStart: Boolean
        get() = if (_isWifiDevice) {
            wifiApi.connectState(itemTcpDevice) == Tcp.CONNECT_STATE_DISCONNECTING
        } else {
            fscApi.connectState(itemFscDevice) == DeviceConnectState.CONNECT_STATE_DISCONNECT_START
        }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemIsSelected = isConnectSuccess
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        updateDeviceState(itemHolder, false)

        when {
            isConnectSuccess -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
                    visible(true)
                    _rotateDegrees = 0f
                    setLoadingRes(R.drawable.dialog_confirm_svg, false)
                }
                itemHolder.tv(R.id.device_flag_view)?.text = _string(R.string.device_connected)
                updateDeviceState(itemHolder, true)
            }

            isConnectStart -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
                    visible(true)
                    setLoadingRes(R.drawable.ble_loading_svg, true)
                }
                itemHolder.tv(R.id.device_flag_view)?.text = _string(R.string.connecting)
            }

            isDisconnectStart -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
                    visible(true)
                    setLoadingRes(R.drawable.ble_loading_svg, true)
                }
                itemHolder.tv(R.id.device_flag_view)?.text = _string(R.string.blue_disconnecting)
            }
            /*DeviceConnectState.CONNECT_STATE_DISCONNECT -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_lib_scan_disconnected)
            }*/
            else -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text = _string(R.string.not_connected)
            }
        }
    }

}