package com.angcyo.device.client

import androidx.annotation.AnyThread
import androidx.lifecycle.ViewModel
import com.angcyo.device.bean.DeviceBean
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataOnce

/**
 * 设备发现通知
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/12/02
 */
class DeviceDiscoverModel : ViewModel() {

    /**所有发现的设备*/
    val deviceList = mutableListOf<DeviceBean>()

    /**[deviceList], 设备列表通知*/
    val deviceListData = vmData(deviceList)

    /**发现新设备通知提示*/
    val deviceFoundData = vmDataOnce<DeviceBean>()

    /**发现设备, 但是不一定是新设备, 需要过滤一下*/
    @AnyThread
    fun onDiscoverDevice(bean: DeviceBean?) {
        bean?.let {
            val find = deviceList.find { it == bean }

            //旧设备, 直接更新数据
            deviceList.remove(bean)
            deviceList.add(0, bean)
            deviceListData.updateValue(deviceList)

            if (find == null) {
                //未找到, 则表示是新设备
                deviceFoundData.updateValue(bean)
            }
        }
    }

}