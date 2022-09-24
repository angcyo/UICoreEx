package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.RectF
import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.*
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryStateParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryVersionParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.toErrorStateString
import com.angcyo.core.vmApp
import com.angcyo.http.rx.doMain
import com.angcyo.library.L
import com.angcyo.library.ex.isDebug
import com.angcyo.library.model.toFourPoint
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
     * [com.angcyo.engrave.model.FscDeviceModel.initDevice]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]
     * */
    val deviceStateData: MutableHoldLiveData<QueryStateParser?> = vmHoldDataNull()

    /**设备设置状态
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]
     * */
    val deviceSettingData: MutableHoldLiveData<QuerySettingParser?> = vmHoldDataNull()

    /**连接的设备产品信息
     * [deviceVersionData]*/
    val productInfoData: MutableLiveData<LaserPeckerProductInfo?> = vmDataNull()

    /**初始化指令是否全部成功完成
     * [com.angcyo.engrave.model.FscDeviceModel.initDevice]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]*/
    val initializeData = vmData(false)

    /**初始化指令是否成功通知*/
    val initializeOnceData = vmDataOnce(false)

    /**预览的时候, 矩形是否溢出了*/
    val overflowRectData = vmData(false)

    /**更新设备模式*/
    @AnyThread
    fun updateDeviceModel(model: Int) {
        deviceModelData.postValue(model)
    }

    @AnyThread
    fun updateDeviceVersion(queryVersionParser: QueryVersionParser) {
        L.i("设备版本:$queryVersionParser".writeBleLog())
        val productInfo = LaserPeckerHelper.parseProductInfo(queryVersionParser.softwareVersion)
        productInfo.deviceName = LaserPeckerHelper.initDeviceName
        productInfo.deviceAddress = LaserPeckerHelper.initDeviceAddress
        productInfo.softwareVersion = queryVersionParser.softwareVersion
        productInfo.hardwareVersion = queryVersionParser.hardwareVersion
        productInfoData.postValue(productInfo)
        deviceVersionData.postValue(queryVersionParser)
    }

    @AnyThread
    fun updateDeviceState(queryStateParser: QueryStateParser) {
        L.i("设备状态:$queryStateParser".writeBleLog())
        if (queryStateParser.error != 0) {
            doMain {
                toast(queryStateParser.error.toErrorStateString())
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

    /**是否连接了扩展设备*/
    fun haveExDevice(): Boolean = isZOpen() || isROpen() || isSOpen()

    /**z轴是否打开, 需要先打开设置, 再连接上 */
    fun isZOpen(): Boolean {
        return deviceSettingData.value?.zFlag == 1 && deviceStateData.value?.zConnect == 1
    }

    /**旋转轴是否打开, 需要先打开设置, 再连接上 */
    fun isROpen(): Boolean {
        return deviceSettingData.value?.rFlag == 1 && (deviceStateData.value?.rConnect == 1 || isDebug())
    }

    /**滑台是否打开, 需要先打开设置, 再连接上 */
    fun isSOpen(): Boolean {
        return deviceSettingData.value?.sFlag == 1 && (deviceStateData.value?.sConnect == 1 || isDebug())
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

    /**发送更新预览范围指令, 支持Z轴判断
     * [bounds] 未旋转的矩形
     * [rotateBounds] [bounds]旋转后的矩形, 真实需要雕刻的范围
     * [rotate] [bounds]需要旋转的角度, 如果设置了, 则自动开启4点预览
     * [zPause] 是否需要第三轴暂停预览
     * [async] 是否是异步指令
     * [diameter] 物体直径，保留小数点后两位。D = d*100，d为物体直径，单位mm。（旋转轴打开时有效）
     * */
    fun sendUpdatePreviewRange(
        bounds: RectF,
        rotateBounds: RectF,
        rotate: Float?,
        pwrProgress: Float,
        async: Boolean,
        zPause: Boolean = false,
        diameter: Int = 0,
        address: String? = null,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction = { _, _ -> }
    ) {
        val cmd = if (zPause) {
            //外接设备暂停预览
            EngravePreviewCmd.previewZRange(
                rotateBounds.left.toInt(),
                rotateBounds.top.toInt(),
                rotateBounds.width().toInt(),
                rotateBounds.height().toInt(),
                pwrProgress
            )
        } else {
            if (rotate != null) {
                //需要4点预览
                EngravePreviewCmd.previewFourPoint(bounds.toFourPoint(rotate), pwrProgress)
            } else {
                EngravePreviewCmd.previewRange(
                    rotateBounds.left.toInt(),
                    rotateBounds.top.toInt(),
                    rotateBounds.width().toInt(),
                    rotateBounds.height().toInt(),
                    pwrProgress,
                    diameter
                )
            }
        }

        //send
        val flag =
            if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
        cmd.enqueue(flag, address, progress, action)
    }

    /**中心点预览指令*/
    fun previewShowCenter(
        bounds: RectF, pwrProgress: Float, async: Boolean,
        address: String? = null,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction = { _, _ -> }
    ) {
        val cmd = EngravePreviewCmd.previewShowCenter(pwrProgress, bounds)
        //send
        val flag =
            if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
        cmd.enqueue(flag, address, progress, action)
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

/**静态方法*/
fun queryDeviceState(flag: Int = CommandQueueHelper.FLAG_ASYNC) {
    vmApp<LaserPeckerModel>().queryDeviceState(flag)
}