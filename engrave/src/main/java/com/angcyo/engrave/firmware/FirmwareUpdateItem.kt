package com.angcyo.engrave.firmware

import androidx.fragment.app.Fragment
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FirmwareUpdateCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FirmwareUpdateParser
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.vmApp
import com.angcyo.dialog.normalDialog
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.UpdateAdapterProperty
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.bluetoothSearchListDialog
import com.angcyo.library.ex.elseNull
import com.angcyo.library.toast
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/08
 */
class FirmwareUpdateItem : DslAdapterItem(), IFragmentItem {

    override var itemFragment: Fragment? = null

    /**需要更新的固件信息*/
    var itemFirmwareInfo: FirmwareInfo? = null

    /**是否正在升级中*/
    var itemIsUpdating: Boolean by UpdateAdapterProperty(false)

    /**是否升级完成*/
    var itemIsFinish: Boolean by UpdateAdapterProperty(false)

    val peckerModel = vmApp<LaserPeckerModel>()

    val apiModel = vmApp<FscBleApiModel>()

    init {
        itemLayoutId = R.layout.item_firmware_update
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = span {
            append(itemFirmwareInfo?.name)
            if (apiModel.haveDeviceConnected()) {
                peckerModel.deviceVersionData.value?.softwareVersionName?.let {
                    appendln()
                    append("当前版本:$it")
                }
            }
            if (itemIsFinish) {
                appendln()
                append("升级完成!")
            }
        }
        itemHolder.visible(R.id.lib_loading_view, itemIsUpdating && !itemIsFinish)
        itemHolder.gone(R.id.device_button, itemIsFinish || itemIsUpdating)
        itemHolder.gone(R.id.start_button, itemIsFinish || itemIsUpdating)

        //设备连接
        itemHolder.click(R.id.device_button) {
            itemFragment?.dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                if (allGranted) {
                    //vmApp<FscBleApiModel>().connect("DC:0D:30:10:05:E7")
                    itemHolder.context.bluetoothSearchListDialog {
                        connectedDismiss = true
                    }
                } else {
                    toast("蓝牙权限被禁用!")
                }
            }
        }

        //开始升级
        itemHolder.click(R.id.start_button) {
            if (apiModel.haveDeviceConnected()) {
                //开始升级
                itemFirmwareInfo?.let { info ->
                    if (peckerModel.deviceVersionData.value?.softwareVersion == info.version) {
                        itemHolder.context.normalDialog {
                            dialogTitle = "警告"
                            dialogMessage = "相同版本的固件, 是否继续升级?"
                            positiveButtonListener = { dialog, dialogViewHolder ->
                                dialog.dismiss()
                                startUpdate(info)
                            }
                        }
                    } else {
                        startUpdate(info)
                    }
                }.elseNull {
                    itemIsUpdating = false
                    toast("data exception!")
                }
            } else {
                itemHolder.clickCallView(R.id.device_button)
            }
        }
    }

    override fun onItemViewRecycled(itemHolder: DslViewHolder, itemPosition: Int) {
        super.onItemViewRecycled(itemHolder, itemPosition)
        _waitReceivePacket?.end()
    }

    var _waitReceivePacket: WaitReceivePacket? = null

    /**监听是否接收数据完成, 数据接收完成设备自动重启*/
    fun listenerFinish() {
        _waitReceivePacket = listenerReceivePacket { receivePacket, bean, error ->
            val isFinish = bean?.parse<FirmwareUpdateParser>()?.isUpdateFinish() == true
            if (isFinish || (error != null && error !is ReceiveCancelException)) {
                receivePacket.isCancel = true
            }
            if (isFinish) {
                itemIsFinish = isFinish
                //断开蓝牙设备
                apiModel.disconnectAll()
            }
        }
    }

    /**开始更新*/
    fun startUpdate(info: FirmwareInfo) {
        itemIsUpdating = true
        FirmwareUpdateCmd.update(info.data.size, info.version)
            .enqueue { bean, error ->
                bean?.parse<FirmwareUpdateParser>()?.let {
                    //进入模式成功, 开始发送数据
                    DataCmd.data(info.data)
                        .enqueue(CommandQueueHelper.FLAG_NO_RECEIVE)
                    listenerFinish()
                }.elseNull {
                    itemIsUpdating = false
                    toast("data exception!")
                }
            }
    }

}