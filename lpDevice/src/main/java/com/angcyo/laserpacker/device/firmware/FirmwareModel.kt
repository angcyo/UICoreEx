package com.angcyo.laserpacker.device.firmware

import androidx.annotation.AnyThread
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.ReceiveCancelException
import com.angcyo.bluetooth.fsc.WaitReceivePacket
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.FirmwareUpdateCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FirmwareUpdateParser
import com.angcyo.bluetooth.fsc.listenerReceivePacket
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.vmApp
import com.angcyo.http.download.download
import com.angcyo.laserpacker.device.R
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.ex._string
import com.angcyo.library.utils.CRC16.crc16
import com.angcyo.viewmodel.vmDataOnce

/**
 * 固件升级模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/16
 */
class FirmwareModel : ViewModel() {

    /**状态通知*/
    val firmwareUpdateOnceData = vmDataOnce<FirmwareUpdateState>(null)

    /**开始升级固件
     * [url] 固件的在线地址
     * */
    @AnyThread
    fun startUpdate(
        url: String,
        verifyMd5: Boolean = true,
        verifyBin: Boolean = false,
        action: (firmwareInfo: FirmwareInfo?, error: Throwable?) -> Unit = { firmwareInfo, error ->
            firmwareInfo?.let {
                startUpdate(it, verifyBin)
            }
            error?.let {
                firmwareUpdateOnceData.postValue(
                    FirmwareUpdateState(FirmwareUpdateState.STATE_ERROR, -1, it)
                )
            }
        }
    ) {
        firmwareUpdateOnceData.postValue(FirmwareUpdateState(FirmwareUpdateState.STATE_DOWNLOAD))
        url.download { task, error ->
            if (task.isFinish) {
                try {
                    val firmwareInfo = task.savePath.toFirmwareInfo(verifyMd5, verifyBin)
                    action(firmwareInfo, null)
                } catch (e: FirmwareException) {
                    action(null, e)
                }
            }
            error?.let {
                action(null, it)
            }
        }
    }

    /**开始升级固件
     * [verifyBin] 是否要验证bin的固件升级范围*/
    @AnyThread
    fun startUpdate(firmwareInfo: FirmwareInfo, verifyBin: Boolean = false) {
        if (verifyBin) {
            var result = true
            vmApp<LaserPeckerModel>().productInfoData.value?.softwareVersion?.let {
                result = VersionMatcher.matches(it, firmwareInfo.lpBin?.r, false)
            }
            if (!result) {
                firmwareUpdateOnceData.postValue(
                    FirmwareUpdateState(
                        FirmwareUpdateState.STATE_ERROR,
                        -1,
                        FirmwareException(
                            _string(R.string.cannot_update_firmware_tip),
                            FirmwareException.TYPE_RANGE
                        )
                    )
                )
                return
            }
        }

        firmwareUpdateOnceData.postValue(FirmwareUpdateState(FirmwareUpdateState.STATE_UPDATE, 0))
        ExitCmd().enqueue()//先进入空闲模式
        FirmwareUpdateCmd.update(
            firmwareInfo.data.size,
            firmwareInfo.version,
            crc16 = firmwareInfo.data.crc16()
        ).enqueue { bean, error ->
            val parser = bean?.parse<FirmwareUpdateParser>()
            if (parser == null) {
                firmwareUpdateOnceData.postValue(
                    FirmwareUpdateState(FirmwareUpdateState.STATE_ERROR, -1, error)
                )
            } else {
                //进入模式成功, 开始发送数据
                DataCmd.data(firmwareInfo.data).enqueue(CommandQueueHelper.FLAG_NO_RECEIVE)
                listenerFinish()
            }
        }
    }

    var _waitReceivePacket: WaitReceivePacket? = null

    /**监听是否接收数据完成, 数据接收完成设备自动重启*/
    fun listenerFinish() {
        _waitReceivePacket = listenerReceivePacket(progress = {
            //进度
            firmwareUpdateOnceData.postValue(
                FirmwareUpdateState(
                    FirmwareUpdateState.STATE_UPDATE,
                    it.sendPacketPercentage
                )
            )
        }) { receivePacket, bean, error ->
            val isFinish = bean?.parse<FirmwareUpdateParser>()?.isUpdateFinish() == true
            if (isFinish || (error != null && error !is ReceiveCancelException)) {
                receivePacket.isCancel = true
            }
            if (isFinish) {
                firmwareUpdateOnceData.postValue(
                    FirmwareUpdateState(
                        FirmwareUpdateState.STATE_FINISH,
                        100
                    )
                )
            }
            error?.let {
                firmwareUpdateOnceData.postValue(
                    FirmwareUpdateState(FirmwareUpdateState.STATE_ERROR, -1, it)
                )
            }
        }
    }

    data class FirmwareUpdateState(
        /**当前更新的状态*/
        val state: Int,
        /**当前状态下的记录*/
        val progress: Int = -1,
        /**失败时的错误信息*/
        val error: Throwable? = null
    ) {
        companion object {
            const val STATE_NORMAL = 0
            const val STATE_DOWNLOAD = 1
            const val STATE_UPDATE = 2
            const val STATE_FINISH = 3
            const val STATE_ERROR = 4
        }
    }
}