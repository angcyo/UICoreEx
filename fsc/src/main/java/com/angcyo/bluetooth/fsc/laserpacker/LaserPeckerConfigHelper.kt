package com.angcyo.bluetooth.fsc.laserpacker

import com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker.bean.DeviceSettingBean
import com.angcyo.core.Debug
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.http.gitee.Gitee
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.HAWK_SPLIT_CHAR
import com.angcyo.library.ex.ResultThrowable
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

    /**材质*/
    const val DEVICE_MATERIAL_BASE_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/"

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
                readDeviceSettingConfig()?.let {
                    if (!it.updateHawkCommand.isNullOrBlank()) {
                        Debug.parseHawkKeys(it.updateHawkCommand?.split(HAWK_SPLIT_CHAR))
                    }
                }
            }
        }
    }

    /**从网络上拉取材质配置, 并保存到本地*/
    fun fetchMaterialConfig(configName: String, action: ResultThrowable? = null) {
        Gitee.getString("${DEVICE_MATERIAL_BASE_URL}$configName") { data, error ->
            data?.let {
                //写入到本地缓存
                it.writeTo(libCacheFile(configName), false)
            }
            action?.invoke(error)
        }
    }

    fun readMaterialConfig(configName: String): String? {
        return libCacheFile(configName).readText()
    }

    //endregion---拉取配置---

    //region---读取配置---

    private var _deviceConfigList: List<DeviceConfigBean>? = null

    /**从本地缓存中读取[DEVICE_CONFIG_FILE_NAME]配置, 缓存没有, 则从[assets]中读取*/
    fun readDeviceConfig(): List<DeviceConfigBean>? {
        if (HawkEngraveKeys.closeOnlineConfig) {
            val json = lastContext.readAssets(DEVICE_CONFIG_FILE_NAME)
            return json.fromJson<List<DeviceConfigBean>>(listType(DeviceConfigBean::class))
        }
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
        if (HawkEngraveKeys.closeOnlineConfig) {
            val json = lastContext.readAssets(DEVICE_SETTING_CONFIG_FILE_NAME)
            return json.fromJson<DeviceSettingBean>()
        }
        if (_deviceSettingBean != null) {
            return _deviceSettingBean
        }
        val json = libCacheFile(DEVICE_SETTING_CONFIG_FILE_NAME).readText()
            ?: lastContext.readAssets(DEVICE_SETTING_CONFIG_FILE_NAME) ?: return null
        val settingBean = json.fromJson<DeviceSettingBean>() ?: return null
        _deviceSettingBean = settingBean
        return settingBean
    }

    /**是否有新功能提示*/
    fun haveNew(key: String?): Boolean = key != null &&
            readDeviceSettingConfig()?.newHawkKeyStr?.contains(key) == true

    //endregion---读取配置---

}