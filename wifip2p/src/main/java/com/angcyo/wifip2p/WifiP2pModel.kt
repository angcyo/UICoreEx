package com.angcyo.wifip2p

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import androidx.annotation.AnyThread
import androidx.lifecycle.ViewModel
import com.angcyo.viewmodel.vmData
import com.angcyo.viewmodel.vmDataNull
import com.angcyo.viewmodel.vmDataOnce
import com.angcyo.wifip2p.data.ConnectStateWrap
import com.angcyo.wifip2p.data.WifiP2pDeviceWrap

/**
 * [WifiP2p]数据模型
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/09
 */
class WifiP2pModel : ViewModel() {

    /**对等的配对设备列表*/
    val peerDeviceListData = vmDataNull<List<WifiP2pDevice>>()

    /**当发现服务设备时触发*/
    val foundServiceDeviceData = vmDataOnce<WifiP2pDeviceWrap>()

    /**发现的所有服务设备信息*/
    val serviceDeviceListData = vmDataNull<List<WifiP2pDeviceWrap>>()

    /**连上的wifi信息*/
    val connectWifiP2pInfoData = vmDataNull<WifiP2pInfo>()

    /**是否正在发现服务*/
    val isDiscoverData = vmData(false)

    /**是否激活了P2P直连*/
    val isEnableWifiP2pData = vmData(true)

    /**
     * [android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener]
     * */
    @AnyThread
    fun onDnsSdTxtRecordAvailable(
        serviceFullDomainName: String,
        record: Map<String, String>,
        sourceDevice: WifiP2pDevice
    ) {
        val device = WifiP2pDeviceWrap(
            record[WifiP2p.KEY_INSTANCE_NAME],
            record[WifiP2p.KEY_SERVICE_TYPE],
            sourceDevice,
            hashMapOf<String, String>().apply { putAll(record) }
        )

        //缓存找到的设备
        val list = serviceDeviceListData.value?.toMutableList() ?: mutableListOf()
        list.removeIf { it.sourceDevice.deviceAddress == device.sourceDevice.deviceAddress }
        list.add(device)

        serviceDeviceListData.postValue(list)

        //单消息通知
        foundServiceDeviceData.postValue(device)
    }

    /**
     * [android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener]
     * */
    @AnyThread
    fun onDnsSdServiceAvailable(
        instanceName: String,
        serviceNameAndTP: String,
        sourceDevice: WifiP2pDevice
    ) {
        //更新部分数据
        serviceDeviceListData.value?.find {
            it.sourceDevice.deviceAddress == sourceDevice.deviceAddress
        }?.let {
            if (it.instanceName.isNullOrEmpty()) {
                it.instanceName = instanceName
            }
            if (it.registrationType.isNullOrEmpty()) {
                it.registrationType = serviceNameAndTP
            }
        }
    }

    /**连接状态改变通知, 只能连接一台设备*/
    val connectStateData = vmDataNull<ConnectStateWrap>(null)

    /**连接状态*/
    fun connectState(device: WifiP2pDeviceWrap): ConnectStateWrap {
        val connectStateWrap = connectStateData.value
        if (connectStateWrap?.deviceWrap?.sourceDevice?.deviceAddress == device.sourceDevice.deviceAddress) {
            return connectStateWrap!!
        }
        return ConnectStateWrap(0, device)
    }

    /**更新连接状态
     * [STATE_CONNECT_START]
     * [STATE_CONNECT_SUCCESS]
     * [STATE_CONNECT_FAILURE]
     * */
    fun updateConnectState(device: WifiP2pDeviceWrap, state: Int) {
        val stateWrap = connectState(device)
        stateWrap.state = state
        stateWrap.deviceWrap = device
        connectStateData.postValue(stateWrap)
    }

    /**更新连接的网络信息*/
    @AnyThread
    fun updateConnectWifiP2pInfo(wifiP2pInfo: WifiP2pInfo?) {
        connectStateData.value?.deviceWrap?.wifiP2pInfo = wifiP2pInfo
        connectWifiP2pInfoData.postValue(wifiP2pInfo)

        connectStateData.value?.deviceWrap?.let {
            if (wifiP2pInfo == null) {
                updateConnectState(it, ConnectStateWrap.STATE_CONNECT_FAILURE)
            } else {
                updateConnectState(it, ConnectStateWrap.STATE_CONNECT_SUCCESS)
            }
            //停止扫描
            isDiscoverData.postValue(false)
        }
    }

    /**自身设备信息监听*/
    val selfWifiP2pDeviceData = vmDataNull<WifiP2pDevice>()
}