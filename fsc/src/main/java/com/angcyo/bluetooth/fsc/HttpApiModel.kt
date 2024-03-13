package com.angcyo.bluetooth.fsc

import androidx.lifecycle.ViewModel
import com.angcyo.bluetooth.fsc.bean.DHttpBean
import com.angcyo.bluetooth.fsc.bean.DServerInfoBean
import com.angcyo.bluetooth.fsc.bean.dBeanType
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.host
import com.angcyo.core.vmApp
import com.angcyo.http.rx.observe
import com.angcyo.http.toApi
import com.angcyo.http.toBean
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmDataNull

/**
 * http 接口请求相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/03/13
 */
class HttpApiModel : ViewModel() {

    /**主机地址, http:///xxx*/
    val _host: String?
        get() = vmApp<WifiApiModel>().tcpStateData.value?.tcpDevice?.host

    /**服务信息*/
    val serverInfoData = vmDataNull<DServerInfoBean>()

    /**查询服务的信息*/
    fun fetchServerInfo(
        host: String? = _host,
        onEnd: ((data: DServerInfoBean?, error: Throwable?) -> Unit)? = null
    ) {
        com.angcyo.http.get {
            url = "server/info".toApi(host)
            isSuccessful = { it.isSuccessful }
        }.observe { data, error ->
            val bean =
                data?.toBean<DHttpBean<DServerInfoBean>>(dBeanType(DServerInfoBean::class.java))
            if (error == null) {
                bean?.result?.let { vmApp<LaserPeckerModel>().updateHttpServerInfo(it) }
                serverInfoData.updateValue(bean?.result)
            }
            onEnd?.invoke(bean?.result, error)
        }
    }

}