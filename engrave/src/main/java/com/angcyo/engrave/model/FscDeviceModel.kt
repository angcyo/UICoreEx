package com.angcyo.engrave.model

import android.app.Activity
import android.os.Debug
import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.core.DeviceConnectState
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.data.toMm
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.item.component.DebugAction
import com.angcyo.item.component.DebugFragment
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.app
import com.angcyo.library.component.OnBackgroundObserver
import com.angcyo.library.component.RBackground
import com.angcyo.library.ex._string
import com.angcyo.library.ex.nowTime
import com.angcyo.library.toast
import com.angcyo.library.utils.LogFile
import com.angcyo.library.utils.appFolderPath
import com.angcyo.library.utils.toLogFilePath
import com.angcyo.objectbox.findLastList
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.saveEntity
import com.angcyo.viewmodel.observe
import com.angcyo.viewmodel.observeOnce
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue

/**
 * 蓝牙设备模式
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/21
 */
class FscDeviceModel : LifecycleViewModel() {

    companion object {
        /**自动连接阈值, 2次连接间隔大于此值才触发*/
        var AUTO_CONNECT_THRESHOLD = 1 * 60 * 1_000L
    }

    val bleApiModel = vmApp<FscBleApiModel>()
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    /**最后一次触发自动连接的时间, 毫秒*/
    var lastConnectTime: Long = -1

    /**初始化*/
    @CallPoint
    fun initDevice() {
        //蓝牙状态监听
        bleApiModel.connectStateData.observe(this, allowBackward = false) {
            it?.let { deviceConnectState ->

                if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_START) {
                    //
                    UMEvent.CONNECT_DEVICE.umengEventValue {
                        put(UMEvent.KEY_START_TIME, nowTime().toString())
                    }
                } else if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_DISCONNECT) {
                    val lastState = bleApiModel.connectStateData.lastValue?.state
                    if (lastState == DeviceConnectState.CONNECT_STATE_SUCCESS ||
                        lastState == DeviceConnectState.CONNECT_STATE_DISCONNECT_START
                    ) {
                        //蓝牙设备断开
                        toast(_string(R.string.blue_disconnected))
                    }

                    //蓝牙断开后,清空设备状态
                    laserPeckerModel.apply {
                        deviceStateData.postValue(null)
                        initializeData.postValue(false)
                    }
                } else if (deviceConnectState.state == DeviceConnectState.CONNECT_STATE_SUCCESS) {
                    //蓝牙已连接

                    //发送初始化指令
                    LaserPeckerHelper.sendInitCommand(
                        deviceConnectState.device.name,
                        deviceConnectState.device.address
                    )

                    //入库
                    DeviceConnectEntity::class.saveEntity(LPBox.PACKAGE_NAME) {
                        deviceAddress = deviceConnectState.device.address
                        deviceName = deviceConnectState.device.name
                    }

                    if (deviceConnectState.isAutoConnect) {
                        //自动连接成功后, 显示连接提示
                        lastConnectTime = nowTime()

                        laserPeckerModel.productInfoData.observeOnce(allowBackward = false) {
                            //等待设备信息读取结束之后才显示
                            if (it != null) {
                                (RBackground.lastActivityRef?.get() ?: app()).dslAHelper {
                                    start(DeviceConnectTipActivity::class)
                                }
                            }
                            it != null
                        }
                    } else {
                        toast(_string(R.string.blue_connected))
                    }

                    //
                    UMEvent.CONNECT_DEVICE.umengEventValue {
                        val nowTime = nowTime()
                        put(UMEvent.KEY_FINISH_TIME, nowTime.toString())
                        put(
                            UMEvent.KEY_DURATION,
                            (nowTime - deviceConnectState.connectTime).toString()
                        )
                    }
                }
            }
        }

        //自动连接监听
        RBackground.registerObserver(object : OnBackgroundObserver() {
            override fun onActivityLifecycleChanged(activity: Activity, state: String) {
                super.onActivityLifecycleChanged(activity, state)
                if (state == RBackground.RESUMED) {
                    if (HawkEngraveKeys.AUTO_CONNECT_DEVICE && !bleApiModel.haveDeviceConnected() /*无设备连接*/) {
                        //需要自动连接设备
                        val nowTime = nowTime()
                        if (nowTime - lastConnectTime > AUTO_CONNECT_THRESHOLD || Debug.isDebuggerConnected()) {
                            //1分钟
                            lpBoxOf(DeviceConnectEntity::class).findLastList().lastOrNull()?.let {
                                L.i("准备自动连接设备:${it.deviceName} ${it.deviceAddress}")
                                bleApiModel.connect(it.deviceAddress, it.deviceName, true)
                            }
                        }
                    }
                }
            }
        })

        //监听设备变化
        laserPeckerModel.productInfoData.observe(this) {
            it?.let { product ->

                //
                GraphicsHelper._minLeft = product.previewBounds.left.toMm()
                GraphicsHelper._minTop = product.previewBounds.top.toMm()

                //材质列表初始化, 按需初始化, 节省内存
                if (product.isLI()) {
                    EngraveHelper.initL1MaterialList()
                } else if (product.isLII()) {
                    EngraveHelper.initL2MaterialList()
                } else if (product.isLIII()) {
                    EngraveHelper.initL3MaterialList()
                }
                /*vmApp<EngraveModel>().engraveOptionInfoData.value?.let { option ->
                    if (product.laserTypeList.isNotEmpty() && !product.laserTypeList.contains(option.type)) {
                        //当前设备不支持选中的激光类型, 则调整一下
                        option.type = product.laserTypeList.first()
                    }
                }*/
            }
        }

        //ble日志
        DebugFragment.DEBUG_ACTION_LIST.add(
            DebugAction(LogFile.ble, LogFile.ble.toLogFilePath())
        )

        //雕刻目录
        DebugFragment.DEBUG_ACTION_LIST.add(
            DebugAction("engrave", appFolderPath(CanvasConstant.ENGRAVE_FILE_FOLDER))
        )

        //设备主动退出工作模式
        //AA BB 08 FF 00 00 00 00 00 00 FF
    }

}