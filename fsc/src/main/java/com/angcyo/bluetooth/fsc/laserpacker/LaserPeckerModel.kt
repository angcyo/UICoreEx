package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.RectF
import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.ISendProgressAction
import com.angcyo.bluetooth.fsc.laserpacker.command.PrintPreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.DeviceStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.DeviceVersionParser
import com.angcyo.core.vmApp
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
class LaserPeckerModel : ViewModel(), IViewModel {

    companion object {

        /**设备空闲*/
        const val DEVICE_MODEL_IDLE = -1

        /**预览模式, 表示设备正在预览模式*/
        const val DEVICE_MODEL_PREVIEW = 0x02
    }

    /**当前设备的模式*/
    val deviceModelData: MutableLiveData<Int> = vmData(DEVICE_MODEL_IDLE)

    /**设备版本*/
    val deviceVersionData: MutableLiveData<DeviceVersionParser?> = vmDataNull()

    /**设备状态*/
    val deviceStateData: MutableLiveData<DeviceStateParser?> = vmDataNull()

    /**版本名称*/
    var productName: String = LaserPeckerProduct.UNKNOWN

    /**更新设备模式*/
    @AnyThread
    fun updateDeviceModel(model: Int) {
        deviceModelData.postValue(model)
    }

    @AnyThread
    fun updateDeviceVersion(deviceVersionParser: DeviceVersionParser) {
        productName = LaserPeckerProduct.parseProductName(deviceVersionParser.softwareVersion)
        deviceVersionData.postValue(deviceVersionParser)
    }

    @AnyThread
    fun updateDeviceState(deviceStateParser: DeviceStateParser) {
        deviceStateData.postValue(deviceStateParser)
    }

    //<editor-fold desc="Command">

    /**发送更新预览范围指令*/
    fun sendUpdatePreviewRange(
        bounds: RectF,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction = { _, _ -> }
    ) {
        vmApp<FscBleApiModel>().connectDeviceListData.value?.firstOrNull()
            ?.let { deviceState ->
                val cmd = PrintPreviewCmd.previewRange(
                    bounds.left.toInt(),
                    bounds.top.toInt(),
                    bounds.width().toInt(),
                    bounds.height().toInt()
                )
                if (cmd != null) {
                    LaserPeckerHelper.sendCommand(
                        deviceState.device.address, cmd, progress = progress, action = action
                    )
                }
            }
    }

    //</editor-fold desc="Command">

}