package com.angcyo.engrave.ble.dslitem

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.BluetoothSearchListDialogConfig
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.visible
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.image.ImageLoadingView
import com.feasycom.common.bean.FscDevice
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 蓝牙设备连接的item
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */
class BluetoothConnectItem : DslAdapterItem() {

    /**设备*/
    var itemFscDevice: FscDevice? = null

    /**是否显示信号强度*/
    var itemShowRssi: Boolean = false

    val fscApi = vmApp<FscBleApiModel>()

    init {
        itemLayoutId = R.layout.item_bluetooth_connect_layout
        itemSingleSelectMutex = true

        itemClick = {
            if (fscApi.isConnectState(itemFscDevice)) {
                fscApi.disconnect(itemFscDevice)
            } else {
                fscApi.connect(itemFscDevice, false, true)

                //连接后, 搜索结束
                UMEvent.SEARCH_DEVICE.umengEventValue {
                    val nowTime = nowTime()
                    put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                    put(
                        UMEvent.KEY_DURATION,
                        (nowTime - BluetoothSearchListDialogConfig.last_search_time).toString()
                    )
                }
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
        //L.e("connectState:$connectState")
        itemIsSelected = connectState == DeviceConnectState.CONNECT_STATE_SUCCESS
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.selected(itemIsSelected)

        itemHolder.tv(R.id.device_name_view)?.text = buildString {
            append(itemFscDevice?.name)
            //append(itemFscDevice?.address)
            if (itemShowRssi) {
                append(" ${itemFscDevice?.rssi}")
            }
        }

        //image
        itemHolder.img(R.id.device_image_view)
            ?.setImageResource(DeviceConnectTipActivity.getDeviceImageRes(itemFscDevice?.name))

        when (connectState) {
            DeviceConnectState.CONNECT_STATE_SUCCESS -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
                    visible(true)
                    setLoadingRes(R.drawable.dialog_confirm_svg, false)
                }
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.device_connected)
            }
            DeviceConnectState.CONNECT_STATE_START -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
                    visible(true)
                    setLoadingRes(R.drawable.ic_loading_svg, true)
                }
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.blue_connecting)
            }
            DeviceConnectState.CONNECT_STATE_DISCONNECT_START -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
                    visible(true)
                    setLoadingRes(R.drawable.ic_loading_svg, true)
                }
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.blue_disconnecting)
            }
            /*DeviceConnectState.CONNECT_STATE_DISCONNECT -> {
                itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.bluetooth_lib_scan_disconnected)
            }*/
            else -> {
                itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.visible(false)
                itemHolder.tv(R.id.device_flag_view)?.text =
                    _string(R.string.blue_no_device_connected)
            }
        }
    }
}