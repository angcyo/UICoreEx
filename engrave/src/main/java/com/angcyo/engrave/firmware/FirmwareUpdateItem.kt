package com.angcyo.engrave.firmware

import android.content.Context
import androidx.fragment.app.Fragment
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FirmwareUpdateCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FirmwareUpdateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.toFirmwareVersionString
import com.angcyo.bluetooth.fsc.laserpacker.parse.toLaserPeckerVersionName
import com.angcyo.core.component.dslPermissions
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dialog.normalDialog
import com.angcyo.drawable.loading.BaseTGLoadingDrawable
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.UpdateAdapterProperty
import com.angcyo.dsladapter.item.IFragmentItem
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.bluetoothSearchListDialog
import com.angcyo.library.L
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.ex.*
import com.angcyo.library.toast
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.loading.TGStrokeLoadingView
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

    /**进度*/
    var itemUpdateProgress: Int by UpdateAdapterProperty(0)

    /**是否升级完成*/
    var itemIsFinish: Boolean by UpdateAdapterProperty(false)

    val peckerModel = vmApp<LaserPeckerModel>()

    val apiModel = vmApp<FscBleApiModel>()

    /**开始的时间*/
    var _startTime: Long = -1

    /**完成的时间*/
    var _finishTime: Long = -1

    init {
        itemLayoutId = R.layout.item_firmware_update
    }

    /**重置状态*/
    fun reset() {
        _startTime = -1
        _finishTime = -1
        itemIsFinish = false
        itemIsUpdating = false
        itemUpdateProgress = 0
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val lpBin = itemFirmwareInfo?.lpBin
        itemHolder.tv(R.id.lib_text_view)?.text = span {
            val name = if (lpBin == null) {
                itemFirmwareInfo?.name
            } else {
                lpBin.n ?: itemFirmwareInfo?.name
            }
            append(name)

            lpBin?.let {
                appendln()
                append("${_string(R.string.firmware_version)}:${it.v.toFirmwareVersionString()}")
                if (it.t > 0) {
                    appendln()
                    append("${_string(R.string.firmware_time)}:${it.t.toTime("yyyy-MM-dd HH:mm:ss")}")
                }
                if (!it.d.isNullOrEmpty()) {
                    appendln()
                    append(it.d)
                }
            }

            if (apiModel.haveDeviceConnected()) {
                peckerModel.deviceVersionData.value?.softwareVersionName?.let {
                    appendln()
                    append("${_string(R.string.device_firmware_version)}:$it")
                }
            }
            if (itemIsFinish) {
                appendln()
                append(_string(R.string.upgrade_completed))

                appendln()
                if (_startTime > 0) {
                    append("${_string(R.string.upgrade_duration)}:${(_finishTime - _startTime).toElapsedTime()}")
                } else {
                    append(_finishTime.fullTime())
                }
            }
        }
        itemHolder.visible(R.id.lib_loading_view, itemIsUpdating && !itemIsFinish)
        itemHolder.gone(
            R.id.device_button,
            apiModel.haveDeviceConnected() || itemIsFinish || itemIsUpdating
        )
        itemHolder.gone(R.id.start_button, itemIsFinish || itemIsUpdating)

        if (itemIsUpdating) {
            //进度
            itemHolder.v<TGStrokeLoadingView>(R.id.lib_loading_view)
                ?.firstDrawable<BaseTGLoadingDrawable>()?.apply {
                    isIndeterminate = itemUpdateProgress <= 0
                    progress = itemUpdateProgress
                }
        }

        //设备连接
        itemHolder.click(R.id.device_button) {
            searchDeviceList()
        }

        //开始升级
        itemHolder.click(R.id.start_button) {
            if (apiModel.haveDeviceConnected()) {
                //开始升级
                itemFirmwareInfo?.let { info ->
                    if (peckerModel.deviceVersionData.value?.softwareVersion == info.version) {
                        itemHolder.context.normalDialog {
                            dialogTitle = _string(R.string.engrave_warn)
                            dialogMessage = "相同版本的固件, 是否继续升级?"
                            positiveButtonListener = { dialog, dialogViewHolder ->
                                dialog.dismiss()
                                startUpdate(itemHolder.context, info)
                            }
                        }
                    } else {
                        startUpdate(itemHolder.context, info)
                    }
                }.elseNull {
                    itemIsUpdating = false
                    toast(_string(R.string.data_exception))
                }
            } else {
                itemHolder.clickCallView(R.id.device_button)
            }
        }
    }

    /**搜索设备*/
    fun searchDeviceList() {
        itemFragment?.apply {
            dslPermissions(FscBleApiModel.bluetoothPermissionList()) { allGranted, foreverDenied ->
                if (allGranted) {
                    //vmApp<FscBleApiModel>().connect("DC:0D:30:10:05:E7")
                    context?.bluetoothSearchListDialog {
                        connectedDismiss = true
                    }
                } else {
                    toast("蓝牙权限被禁用!")
                }
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
        _waitReceivePacket = listenerReceivePacket(progress = {
            //进度
            itemUpdateProgress = it.sendPacketPercentage
            L.w("进度:$itemUpdateProgress")
        }) { receivePacket, bean, error ->
            val isFinish = bean?.parse<FirmwareUpdateParser>()?.isUpdateFinish() == true
            if (isFinish || (error != null && error !is ReceiveCancelException)) {
                receivePacket.isCancel = true
            }
            if (isFinish) {
                itemIsFinish = true
                itemIsUpdating = false
                _finishTime = nowTime()
                //断开蓝牙设备
                apiModel.disconnectAll()
            }
        }
    }

    /**检查更新的固件版本是否匹配*/
    fun checkVersionMatch(info: FirmwareInfo): Boolean {
        var result = true
        peckerModel.productInfoData.value?.softwareVersion?.let {
            result = VersionMatcher.matches(it, info.lpBin?.r)
        }
        return result
    }

    /**开始更新*/
    fun startUpdate(context: Context, info: FirmwareInfo) {
        if (checkVersionMatch(info)) {
            itemIsFinish = false
            itemIsUpdating = true
            _startTime = nowTime()
            ExitCmd().enqueue()//先进入空闲模式
            FirmwareUpdateCmd.update(info.data.size, info.version)
                .enqueue { bean, error ->
                    bean?.parse<FirmwareUpdateParser>()?.let {
                        //进入模式成功, 开始发送数据
                        DataCmd.data(info.data).enqueue(CommandQueueHelper.FLAG_NO_RECEIVE)
                        listenerFinish()
                    }.elseNull {
                        itemIsUpdating = false
                        toast(_string(R.string.data_exception))
                    }
                }
        } else {
            context.messageDialog {
                dialogTitle = _string(R.string.engrave_warn)
                dialogMessage = buildString {
                    appendLine(_string(R.string.cannot_update_firmware_tip))
                    append(peckerModel.productInfoData.value?.softwareVersion?.toLaserPeckerVersionName())
                }
            }
        }
    }

}