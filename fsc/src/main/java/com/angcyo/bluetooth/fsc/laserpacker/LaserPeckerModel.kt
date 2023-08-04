package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.RectF
import androidx.annotation.AnyThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.IReceiveBeanAction
import com.angcyo.bluetooth.fsc.ISendProgressAction
import com.angcyo.bluetooth.fsc.R
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.bluetooth.fsc.laserpacker.data.LaserPeckerProductInfo
import com.angcyo.bluetooth.fsc.laserpacker.data.OverflowInfo
import com.angcyo.bluetooth.fsc.laserpacker.parse.QuerySettingParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryVersionParser
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.ex._string
import com.angcyo.library.model.toFourPoint
import com.angcyo.library.toast
import com.angcyo.viewmodel.IViewModel
import com.angcyo.viewmodel.MutableHoldLiveData
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import com.angcyo.viewmodel.vmHoldDataNull

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/25
 */
class LaserPeckerModel : ViewModel(), IViewModel {

    /**设备版本*/
    val deviceVersionData: MutableLiveData<QueryVersionParser?> = vmDataNull()

    /**设备设置状态
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]
     * */
    val deviceSettingData: MutableHoldLiveData<QuerySettingParser?> = vmHoldDataNull()

    /**连接的设备产品信息
     * [deviceVersionData]*/
    val productInfoData: MutableLiveData<LaserPeckerProductInfo?> = vmDataNull()

    /**初始化指令是否全部成功完成, 蓝牙断开之后清空值
     * [com.angcyo.laserpacker.device.model.FscDeviceModel.initDevice]
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.sendInitCommand]*/
    val initializeData = vmData(false)

    /**初始化指令是否成功通知*/
    val initializeOnceData = vmDataOnce(false)

    /**设备被USB占用提示通知*/
    val deviceBusyOnceData = vmDataOnce(false)

    /**通知, 设备的设置是否发生了改变*/
    val updateSettingOnceData = vmDataOnce(false)

    /**预览的时候, 矩形是否溢出了*/
    val overflowInfoData = vmDataNull<OverflowInfo>()

    /**更新设备版本, 设备信息.*/
    @AnyThread
    fun updateDeviceVersion(queryVersionParser: QueryVersionParser) {
        "设备版本:$queryVersionParser".writeBleLog(L.INFO)
        val productInfo = LaserPeckerHelper.parseProductInfo(
            queryVersionParser.softwareVersion,
            queryVersionParser.hardwareVersion
        )
        if (productInfo == null) {
            toast(_string(R.string.not_support))
        }
        productInfo?.deviceName = LaserPeckerHelper.initDeviceName
        productInfo?.deviceAddress = LaserPeckerHelper.initDeviceAddress
        productInfoData.postValue(productInfo)
        deviceVersionData.postValue(queryVersionParser)
    }

    @AnyThread
    fun updateDeviceSettingState(querySettingParser: QuerySettingParser) {
        "设备设置状态:$querySettingParser".writeBleLog(L.INFO)
        if (QuerySettingParser.Z_MODEL_STR.isBlank()) {
            //本地未初始化第三轴模式
            QuerySettingParser.Z_MODEL_STR = if (querySettingParser.zDir == 1 || isCSeries()) {
                //圆柱, C1只有圆柱
                QuerySettingParser.Z_MODEL_CYLINDER
            } else {
                //平板
                QuerySettingParser.Z_MODEL_FLAT
            }
        } else if (isCSeries()) {
            QuerySettingParser.Z_MODEL_STR = QuerySettingParser.Z_MODEL_CYLINDER
        }
        deviceSettingData.postValue(querySettingParser)
    }

    //---

    /**获取外部设备描述*/
    fun getExDevice(): String? = when {
        isZOpen() -> QuerySettingParser.EX_Z
        isROpen() -> QuerySettingParser.EX_R
        isSOpen() -> QuerySettingParser.EX_S
        isCarConnect() -> QuerySettingParser.EX_CAR
        else -> null
    }

    /**是否连接了扩展设备*/
    fun haveExDevice(): Boolean = isZOpen() || isROpen() || isSOpen()

    /**z轴是否打开, 需要先打开设置, 再连接上 */
    fun isZOpen(): Boolean {
        return deviceSettingData.value?.zFlag == 1 //&& (deviceStateData.value?.zConnect == 1 || isDebug())
    }

    /**旋转轴是否打开, 需要先打开设置, 再连接上 */
    fun isROpen(): Boolean {
        return deviceSettingData.value?.rFlag == 1 //&& (deviceStateData.value?.rConnect == 1 || isDebug())
    }

    /**滑台是否打开, 需要先打开设置, 再连接上 */
    fun isSOpen(): Boolean {
        return deviceSettingData.value?.sFlag == 1 //&& (deviceStateData.value?.sConnect == 1 || isDebug())
    }

    /**C1 移动平台雕刻
     * [com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel.isCarMode]*/
    fun isCarConnect(): Boolean {
        //return deviceSettingData.value?.carFlag == 1 //&& (deviceStateData.value?.carConnect == 1 || isDebug())
        return vmApp<DeviceStateModel>().isCarMode()
    }

    /**滑台多文件雕刻模式*/
    fun isSRepMode(): Boolean {
        return deviceSettingData.value?.sRep == 1
    }

    /**LP5数据需要旋转, Y轴扫描
     * 0：从左到右，从上到下，X轴扫描；
     * 1：从上到下，从左到右，Y轴扫描。（2023/7/25）
     * */
    fun dataDir(): Int {
        when (val dataRotateEx = productInfoData.value?.deviceConfigBean?.dataRotateEx) {
            null -> return 0
            "*" -> return 1
            else -> {
                dataRotateEx.split(",").forEach {
                    if (it == QuerySettingParser.EX_Z && isZOpen()) {
                        return 1
                    }
                    if (it == QuerySettingParser.EX_R && isROpen()) {
                        return 1
                    }
                    if (it == QuerySettingParser.EX_S && isSOpen()) {
                        return 1
                    }
                    if (it == QuerySettingParser.EX_CAR && isCarConnect()) {
                        return 1
                    }
                }
            }
        }
        return 0
    }

    //---

    fun isL1() = productInfoData.value?.isL1() == true

    fun isL2() = productInfoData.value?.isL2() == true

    fun isL3() = productInfoData.value?.isL3() == true

    fun isL4() = productInfoData.value?.isL4() == true

    fun isL5() = productInfoData.value?.isL5() == true

    fun isC1() = productInfoData.value?.isC1() == true

    fun isC2() = productInfoData.value?.isC2() == true

    /**是否是C系列*/
    fun isCSeries() = productInfoData.value?.isCSeries() == true

    /**是否是lp系列*/
    fun isLPSeries() = productInfoData.value?.isLPSeries() == true

    fun isSupportDithering() = productInfoData.value?.supportDithering == true

    /**具有wifi功能的产品*/
    fun isWifiProduct() = LaserPeckerHelper.isWifiDevice(productInfoData.value?.deviceName)

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
        @Pixel bounds: RectF,
        @Pixel rotateBounds: RectF,
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
            EngravePreviewCmd.adjustPreviewZRangeCmd(
                rotateBounds.left,
                rotateBounds.top,
                rotateBounds.width(),
                rotateBounds.height(),
                pwrProgress,
                diameter
            )
        } else {
            if (rotate != null) {
                //需要4点预览
                EngravePreviewCmd.adjustPreviewFourPointCmd(bounds.toFourPoint(rotate), pwrProgress)
            } else {
                EngravePreviewCmd.adjustPreviewRangeCmd(
                    rotateBounds.left,
                    rotateBounds.top,
                    rotateBounds.width(),
                    rotateBounds.height(),
                    pwrProgress,
                    diameter
                )
            }
        }

        //send
        val flag =
            if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
        cmd?.enqueue(flag, address, progress, action)
    }

    /**中心点预览指令*/
    fun previewShowCenter(
        bounds: RectF?, pwrProgress: Float, async: Boolean,
        address: String? = null,
        progress: ISendProgressAction = {},
        action: IReceiveBeanAction = { _, _ -> }
    ) {
        bounds ?: return
        val cmd = EngravePreviewCmd.previewShowCenterCmd(pwrProgress, bounds)
        //send
        val flag =
            if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
        cmd?.enqueue(flag, address, progress, action)
    }

    //</editor-fold desc="Command">
}