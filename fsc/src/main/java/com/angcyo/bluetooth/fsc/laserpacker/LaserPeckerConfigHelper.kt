package com.angcyo.bluetooth.fsc.laserpacker

import com.angcyo.bluetooth.fsc.laserpacker.data.DeviceConfigBean
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.http.gitee.Gitee
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.readAssets
import com.angcyo.library.ex.readText
import com.angcyo.library.libCacheFile
import com.angcyo.library.utils.writeTo

/**
 * 配置助手, 在线配置, 本地配置, 本地缓存配置
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/03
 */
object LaserPeckerConfigHelper {

    const val DEVICE_CONFIG_FILE_NAME = "lp_device_config.json"

    /**[lp_device_config.json]配置地址*/
    const val DEVICE_CONFIG_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_CONFIG_FILE_NAME}"

    /**入口*/
    @CallPoint
    fun init() {
        fetchDeviceConfig()
    }

    /**从网络中获取[lp_device_config.json]配置, 并且存储到本地*/
    fun fetchDeviceConfig() {
        Gitee.getString(DEVICE_CONFIG_URL) { data, error ->
            data?.let {
                //写入到本地缓存
                it.writeTo(libCacheFile(DEVICE_CONFIG_FILE_NAME), false)
            }
        }
    }

    /**从本地缓存中读取[lp_device_config.json]配置, 缓存没有, 则从[assets]中读取*/
    fun readDeviceConfig(): List<DeviceConfigBean>? {
        val json = libCacheFile(DEVICE_CONFIG_FILE_NAME).readText()
            ?: lastContext.readAssets("lp_device_config.json") ?: return null
        val configList =
            json.fromJson<List<DeviceConfigBean>>(listType(DeviceConfigBean::class)) ?: return null
        return configList
    }
}