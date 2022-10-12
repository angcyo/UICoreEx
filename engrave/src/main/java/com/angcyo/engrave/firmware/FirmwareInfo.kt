package com.angcyo.engrave.firmware

import com.angcyo.engrave.firmware.FirmwareUpdateFragment.Companion.FIRMWARE_EXT
import com.angcyo.library.ex.file
import com.angcyo.library.ex.lastName

/**
 * 待升级的固件信息
 *
 * L2_N32_V3.5.7.lpbin
 * L2_N32_V3.5.7_2022-7-8.lpbin
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/08
 */
data class FirmwareInfo(
    /**文件路径*/
    val path: String,
    /**文件名*/
    val name: String,
    /**版本号*/
    val version: Int,
    /**文件数据*/
    val data: ByteArray
)

/**从路径中获取固件版本*/
fun String.getFirmwareVersion(ex: String = FIRMWARE_EXT): Int {
    val path = substring(lastIndexOf("/") + 1, length - ex.length)

    val builder = StringBuilder()
    var isStart = false

    for (char in path.lowercase()) {
        if (isStart) {
            if (char in '0'..'9') {
                //如果是数字
                builder.append(char)
            } else if (char == '.') {
                //继续
            } else {
                //中断
                break
            }
        } else {
            if (char == 'v') {
                isStart = true
            }
        }
    }
    if (builder.isEmpty()) {
        return -1
    }
    return builder.toString().toIntOrNull() ?: -1
}

/**文件路径转成固件信息*/
fun String.toFirmwareInfo(): FirmwareInfo {
    return FirmwareInfo(this, lastName(), getFirmwareVersion(), file().readBytes())
}