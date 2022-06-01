package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.RectF
import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.ISendProgressAction
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.ProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryVersionParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.library.L
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull

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

    /**设备状态*/
    val deviceStateData: MutableLiveData<QueryStateParser?> = vmDataNull()

    /**连接的设备产品信息*/
    var productInfoData: MutableLiveData<ProductInfo?> = vmDataNull()

    /**更新设备模式*/
    @AnyThread
    fun updateDeviceModel(model: Int) {
        deviceModelData.postValue(model)
    }

    @AnyThread
    fun updateDeviceVersion(queryVersionParser: QueryVersionParser) {
        L.i("设备版本:$queryVersionParser")
        productInfoData.postValue(LaserPeckerProduct.parseProduct(queryVersionParser.softwareVersion))
        deviceVersionData.postValue(queryVersionParser)
    }

    @AnyThread
    fun updateDeviceState(queryStateParser: QueryStateParser) {
        L.i("设备状态:$queryStateParser")
        deviceStateData.postValue(queryStateParser)
        updateDeviceModel(queryStateParser.mode)
    }

    /**空闲模式*/
    fun isIdleMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_IDLE
    }

    /**雕刻预览模式, 并且非显示中心*/
    fun isEngravePreviewMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW && deviceState.workState != 7
    }

    /**是否是雕刻预览模式下的显示中心*/
    fun isEngravePreviewShowCenterMode(): Boolean {
        val deviceState = deviceStateData.value
        return deviceState?.mode == QueryStateParser.WORK_MODE_ENGRAVE_PREVIEW && deviceState.workState == 7
    }

    //<editor-fold desc="Command">

    /**发送更新预览范围指令*/
    fun sendUpdatePreviewRange(
        bounds: RectF,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction = { _, _ -> }
    ) {
        val cmd = EngravePreviewCmd.previewRange(
            bounds.left.toInt(),
            bounds.top.toInt(),
            bounds.width().toInt(),
            bounds.height().toInt()
        )
        LaserPeckerHelper.sendCommand(cmd, progress, action)
    }

    /**查询设备状态*/
    fun queryDeviceState(block: IReceiveBeanAction = { _, _ -> }) {
        LaserPeckerHelper.sendCommand(QueryCmd(0x00)) { bean, error ->
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