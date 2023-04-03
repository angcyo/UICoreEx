package com.angcyo.bluetooth.fsc.laserpacker

import com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceSettingBean
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
    const val DEVICE_SETTING_CONFIG_FILE_NAME = "lp_setting_config.json"

    /**[DEVICE_CONFIG_FILE_NAME]配置地址*/
    const val DEVICE_CONFIG_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_CONFIG_FILE_NAME}"

    /**[DEVICE_SETTING_CONFIG_FILE_NAME]配置地址*/
    const val DEVICE_SETTING_CONFIG_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_SETTING_CONFIG_FILE_NAME}"

    /**入口*/
    @CallPoint
    fun init() {
        fetchDeviceConfig()
        fetchDeviceSettingConfig()
    }

    //region---拉取配置---

    /**从网络中获取[DEVICE_CONFIG_FILE_NAME]配置, 并且存储到本地*/
    fun fetchDeviceConfig() {
        Gitee.getString(DEVICE_CONFIG_URL) { data, error ->
            data?.let {
                //写入到本地缓存
                _deviceConfigList = null
                it.writeTo(libCacheFile(DEVICE_CONFIG_FILE_NAME), false)
            }
        }
    }

    /**从网络中获取[DEVICE_SETTING_CONFIG_FILE_NAME]配置, 并且存储到本地*/
    fun fetchDeviceSettingConfig() {
        Gitee.getString(DEVICE_SETTING_CONFIG_URL) { data, error ->
            data?.let {
                //写入到本地缓存
                _deviceSettingBean = null
                it.writeTo(libCacheFile(DEVICE_SETTING_CONFIG_FILE_NAME), false)
            }
        }
    }

    //endregion---拉取配置---

    //region---读取配置---

    private var _deviceConfigList: List<DeviceConfigBean>? = null

    /**从本地缓存中读取[DEVICE_CONFIG_FILE_NAME]配置, 缓存没有, 则从[assets]中读取*/
    fun readDeviceConfig(): List<DeviceConfigBean>? {
        if (_deviceConfigList != null) {
            return _deviceConfigList
        }
        val json = libCacheFile(DEVICE_CONFIG_FILE_NAME).readText()
            ?: lastContext.readAssets(DEVICE_CONFIG_FILE_NAME) ?: return null
        val configList =
            json.fromJson<List<DeviceConfigBean>>(listType(DeviceConfigBean::class)) ?: return null
        _deviceConfigList = configList
        return configList
    }

    private var _deviceSettingBean: DeviceSettingBean? = null

    /**从本地缓存中读取[DEVICE_SETTING_CONFIG_FILE_NAME]配置, 缓存没有, 则从[assets]中读取*/
    fun readDeviceSettingConfig(): DeviceSettingBean? {
        if (_deviceSettingBean != null) {
            return _deviceSettingBean
        }
        val json = libCacheFile(DEVICE_SETTING_CONFIG_FILE_NAME).readText()
            ?: lastContext.readAssets(DEVICE_SETTING_CONFIG_FILE_NAME) ?: return null
        val settingBean = json.fromJson<DeviceSettingBean>() ?: return null
        _deviceSettingBean = settingBean
        return settingBean
    }

    //endregion---读取配置---

}