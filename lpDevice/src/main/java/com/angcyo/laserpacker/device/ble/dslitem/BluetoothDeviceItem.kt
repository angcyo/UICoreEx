package com.angcyo.laserpacker.device.ble.dslitem

import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.updateItemSelected
import com.angcyo.laserpacker.device.BuildConfig
import com.angcyo.laserpacker.device.R
import com.angcyo.laserpacker.device.ble.DeviceConnectTipActivity
import com.angcyo.library.ex._color
import com.angcyo.library.ex._dimen
import com.angcyo.library.ex._drawable
import com.angcyo.library.ex._string
import com.angcyo.library.ex.dpi
import com.angcyo.library.ex.isBuildDebug
import com.angcyo.library.ex.tintDrawable
import com.angcyo.library.ex.visible
import com.angcyo.library.extend.IToText
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.image.ImageLoadingView
import com.angcyo.widget.span.span
import com.clj.fastble.data.BleDevice
import com.feasycom.common.bean.FscDevice

/**
 * 蓝牙设备item, 不进行操作, 只进行显示
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/07/31
 */
open class BluetoothDeviceItem : DslAdapterItem(), IToText {

    /**飞易通:设备*/
    var itemFscDevice: FscDevice? = null

    /**ble设备*/
    var itemBleDevice: BleDevice? = null

    /**是否显示信号强度*/
    var itemShowRssi: Boolean = false

    val fscApi = vmApp<FscBleApiModel>()

    val _deviceName: String?
        get() = itemBleDevice?.name ?: itemFscDevice?.name

    init {
        itemLayoutId = R.layout.item_bluetooth_connect_layout
        itemSingleSelectMutex = true
        itemFirstPaddingTop = _dimen(R.dimen.lib_xhdpi)

        itemClick = {
            updateItemSelected(true)
            itemChanging = true
        }
    }

    override fun toText(): CharSequence? =
        DeviceConnectTipActivity.formatDeviceName(_deviceName)

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)
        itemHolder.selected(itemIsSelected)

        //device
        val deviceName = DeviceConnectTipActivity.formatDeviceName(_deviceName)
        itemHolder.tv(R.id.device_name_view)?.text = span {
            append(deviceName)
            if (BuildConfig.BUILD_TYPE.isBuildDebug()) {
                append("/${itemBleDevice?.device?.address ?: itemFscDevice?.address}") {
                    fontSize = 9 * dpi
                }
            }

            val rssi = itemBleDevice?.rssi ?: itemFscDevice?.rssi ?: 0
            if (itemShowRssi && rssi != 0) {
                append(" $rssi") {
                    foregroundColor = _color(R.color.text_sub_color)
                }
            }
        }
        //image
        itemHolder.img(R.id.device_image_view)
            ?.setImageResource(DeviceConnectTipActivity.getDeviceImageRes(deviceName))

        //select
        itemHolder.v<ImageLoadingView>(R.id.lib_loading_view)?.apply {
            visible(itemIsSelected)
            enableRotateAnimation = false
            _rotateDegrees = 0f
            setImageDrawable(_drawable(R.drawable.lib_confirm).tintDrawable(_color(R.color.lib_theme_icon_color)))
        }
        itemHolder.tv(R.id.device_flag_view)?.text = if (itemIsSelected)
            _string(R.string.selected) else _string(R.string.unselected)
    }
}