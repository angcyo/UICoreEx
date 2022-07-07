package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.RectF
import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.R
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.sendCommand
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryVersionParser
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.ex._string
import com.angcyo.library.toast
import com.angcyo.viewmodel.*

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
class LaserPeckerModel : ViewModel(), IViewModel {

    /**当前设备的模式
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.WORK_MODE_IDLE]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.WORK_MODE_ENGRAVE]
     * [com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW]*/
    val deviceModelData: MutableLiveData<Int> = vmData(QueryStateParser.WORK_MODE_IDLE)

    /**设备版本*/
    val deviceVersionData: MutableLiveData<QueryVersionParser?> = vmDataNull()

    /**设备状态,蓝牙断开后,清空设备状态
     *
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]
     * */
    val deviceStateData: MutableHoldLiveData<QueryStateParser?> = vmHoldDataNull()

    /**设备设置状态
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]
     * */
    val deviceSettingData: MutableHoldLiveData<QuerySettingParser?> = vmHoldDataNull()

    /**连接的设备产品信息
     * [deviceVersionData]*/
    var productInfoData: MutableLiveData<LaserPeckerProductInfo?> = vmDataNull()

    /**更新设备模式*/
    @AnyThread
    fun updateDeviceModel(model: Int) {
        deviceModelData.postValue(model)
    }

    @AnyThread
    fun updateDeviceVersion(queryVersionParser: QueryVersionParser) {
        L.i("设备版本:$queryVersionParser".writeBleLog())
        val productInfo = LaserPeckerHelper.parseProductInfo(queryVersionParser.softwareVersion)
        productInfo.hardwareVersion = queryVersionParser.hardwareVersion
        productInfoData.postValue(productInfo)
        deviceVersionData.postValue(queryVersionParser)
    }

    @AnyThread
    fun updateDeviceState(queryStateParser: QueryStateParser) {
        L.i("设备状态:$queryStateParser".writeBleLog())
        if (queryStateParser.error != 0) {
            doMain {
                toast(
                    when (queryStateParser.error) {
                        1 -> _string(R.string.ex_tips_one)
                        2 -> _string(R.string.ex_tips_two)
                        3 -> _string(R.string.ex_tips_three)
                        4 -> _string(R.string.ex_tips_four)
                        5 -> _string(R.string.ex_tips_five)
                        6 -> _string(R.string.ex_tips_six)
                        7 -> _string(R.string.ex_tips_seven)
                        8 -> _string(R.string.ex_tips_eight)
                        9 -> _string(R.string.ex_tips_nine)
                        else -> _string(R.string.ex_tips_six)
                    }
                )
            }
        }
        deviceStateData.postValue(queryStateParser)
        updateDeviceModel(queryStateParser.mode)
    }

    @AnyThread
    fun updateDeviceSettingState(querySettingParser: QuerySettingParser) {
        L.i("设备设置状态:$querySettingParser".writeBleLog())
        if (QuerySettingParser.Z_MODEL == -1) {
            //本地未初始化第三轴模式
            QuerySettingParser.Z_MODEL = if (querySettingParser.zDir == 1) {
                //圆柱
                2
            } else {
                0
            }
        }
        deviceSettingData.postValue(querySettingParser)
    }

    /**z轴是否打开, 需要先打开设置, 再连接上*/
    fun isZOpen(): Boolean {
        return deviceSettingData.value?.zFlag == 1 && deviceStateData.value?.zConnect == 1
    }

    /**空闲模式*/
    fun isIdleMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_IDLE
    }

    /**雕刻预览模式, 并且非显示中心*/
    fun isEngravePreviewMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState != 0x07
    }

    /**是否是雕刻预览模式下的显示中心*/
    fun isEngravePreviewShowCenterMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState == 0x07
    }

    /**是否是雕刻预览模式下的显示中心*/
    fun isEngravePreviewPause(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState == 0x04
    }

    /**Z轴滚动预览中*/
    fun isEngravePreviewZ(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW &&
                deviceState.workState == 0x05
    }

    //<editor-fold desc="Command">

    /**发送更新预览范围指令, 支持Z轴判断*/
    fun sendUpdatePreviewRange(
        bounds: RectF,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction = { _, _ -> }
    ) {
        val cmd = if (isZOpen()) {
            EngravePreviewCmd.previewZRange(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.width().toInt(),
                bounds.height().toInt()
            )
        } else {
            EngravePreviewCmd.previewRange(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.width().toInt(),
                bounds.height().toInt()
            )
        }

        cmd.sendCommand(progress, action)
    }

    /**查询设备状态*/
    fun queryDeviceState(
        flag: Int = CommandQueueHelper.FLAG_NORMAL,
        block: IReceiveBeanAction = { _, _ -> }
    ) {
        QueryCmd.workState.enqueue(flag) { bean, error ->
            bean?.let {
                it.parse<QueryStateParser>()?.let {
                    updateDeviceState(it)
                }
            }
            block(bean, error)
        }
    }

    //</editor-fold desc="Command">
}
