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
    const val DEVICE_CONFIG_FILE_NAME_DEBUG = "lp_device_config_debug.json"
    const val DEVICE_SETTING_CONFIG_FILE_NAME = "lp_setting_config.json"
    const val DEVICE_SETTING_CONFIG_FILE_NAME_DEBUG = "lp_setting_config_debug.json"

    /**[DEVICE_CONFIG_FILE_NAME]配置地址*/
    const val DEVICE_CONFIG_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_CONFIG_FILE_NAME}"
    const val DEVICE_CONFIG_URL_DEBUG =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_CONFIG_FILE_NAME_DEBUG}"

    /**[DEVICE_SETTING_CONFIG_FILE_NAME]配置地址*/
    const val DEVICE_SETTING_CONFIG_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_SETTING_CONFIG_FILE_NAME}"
    const val DEVICE_SETTING_CONFIG_URL_DEBUG =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/${DEVICE_SETTING_CONFIG_FILE_NAME_DEBUG}"

    /**材质*/
    const val DEVICE_MATERIAL_BASE_URL =
        "https://laserpecker-prod.oss-cn-hongkong.aliyuncs.com/config/"

    /**入口*/
    @CallPoint
    fun init() {
        fetchDeviceConfig(DEVICE_CONFIG_URL, DEVICE_CONFIG_FILE_NAME)
        fetchDeviceConfig(DEVICE_CONFIG_URL_DEBUG, DEVICE_CONFIG_FILE_NAME_DEBUG)
        fetchDeviceSettingConfig(DEVICE_SETTING_CONFIG_URL, DEVICE_SETTING_CONFIG_FILE_NAME)
        fetchDeviceSettingConfig(
            DEVICE_SETTING_CONFIG_URL_DEBUG,
            DEVICE_SETTING_CONFIG_FILE_NAME_DEBUG
        )
    }

    //region---拉取配置---

    /**从网络中获取[DEVICE_CONFIG_FILE_NAME]配置, 并且存储到本地*/
    fun fetchDeviceConfig(url: String, name: String) {
        Gitee.getString(url) { data, error ->
            data?.let {
                //写入到本地缓存
                it.writeTo(libCacheFile(name), false)
            }
        }
    }

    /**从网络中获取[DEVICE_SETTING_CONFIG_FILE_NAME]配置, 并且存储到本地*/
    fun fetchDeviceSettingConfig(url: String, name: String) {
        Gitee.getString(url) { data, error ->
            data?.let {
                //写入到本地缓存
                it.writeTo(libCacheFile(name), false)
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
        val api = "${DEVICE_MATERIAL_BASE_URL}$configName"
        Gitee.getString(api) { data, error ->
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

    /**从本地缓存中读取[DEVICE_CONFIG_FILE_NAME]配置, 缓存没有, 则从[assets]中读取*/
    fun readDeviceConfig(debugConfig: Boolean = HawkEngraveKeys.useDebugConfig): List<DeviceConfigBean>? {
        val name = if (debugConfig) {
            DEVICE_CONFIG_FILE_NAME_DEBUG
        } else {
            DEVICE_CONFIG_FILE_NAME
        }

        val result: List<DeviceConfigBean>? = if (HawkEngraveKeys.closeOnlineConfig) {
            val json = lastContext.readAssets(name)
            json.fromJson<List<DeviceConfigBean>>(listType(DeviceConfigBean::class))
        } else {
            val json = libCacheFile(name).readText() ?: lastContext.readAssets(name)
            json.fromJson<List<DeviceConfigBean>>(listType(DeviceConfigBean::class))
        }

        if (result.isNullOrEmpty()) {
            if (debugConfig) {
                //自动使用正式配置
                return readDeviceConfig(false)
            }
        }

        return result
    }

    /**从本地缓存中读取[DEVICE_SETTING_CONFIG_FILE_NAME]配置, 缓存没有, 则从[assets]中读取*/
    fun readDeviceSettingConfig(debugConfig: Boolean = HawkEngraveKeys.useDebugConfig): DeviceSettingBean? {
        val name = if (debugConfig) {
            DEVICE_SETTING_CONFIG_FILE_NAME_DEBUG
        } else {
            DEVICE_SETTING_CONFIG_FILE_NAME
        }

        val result: DeviceSettingBean? = if (HawkEngraveKeys.closeOnlineConfig) {
            val json = lastContext.readAssets(name)
            json.fromJson<DeviceSettingBean>()
        } else {
            val json = libCacheFile(name).readText() ?: lastContext.readAssets(name)
            json.fromJson<DeviceSettingBean>()
        }

        if (result == null) {
            if (debugConfig) {
                //自动使用正式配置
                return readDeviceSettingConfig(false)
            }
        }

        return result

    }

    /**是否有新功能提示*/
    fun haveNew(key: String?): Boolean = key != null &&
            readDeviceSettingConfig()?.newHawkKeyStr?.contains(key) == true

    /**是否要开放指定的功能, 会自动拼上,号`.`防止子包含*/
    fun isOpenFun(key: String?): Boolean {
        if (key.isNullOrEmpty()) {
            return false
        }
        val openFun = readDeviceSettingConfig()?.openFun ?: return false
        if (openFun.isBlank()) {
            //空字符串, 表示开放所有功能
            return true
        }
        return openFun.contains("${key},")
    }

    //endregion---读取配置---
}

/**设备配置, App配置信息*/
val deviceSettingBean: DeviceSettingBean?
    get() = LaserPeckerConfigHelper.readDeviceSettingConfig()