package com.angcyo.engrave.firmware

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.R
import com.angcyo.engrave.firmware.FirmwareUpdateFragment.Companion.FIRMWARE_EXT
import com.angcyo.http.base.fromJson
import com.angcyo.library.ex.*

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
    val data: ByteArray,
    /**bin文件自带的数据信息*/
    val lpBin: LPBinBean? = null
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
@Throws(FirmwareException::class)
fun String.toFirmwareInfo(): FirmwareInfo {
    var bytes = file().readBytes()
    val size = bytes.size
    var lpBinBean: LPBinBean? = null

    try {
        val lengthBytes = bytes.slice(size - 4 until size)
        val length = lengthBytes.toByteArray().toByteInt()//数据总长度
        if (length > 0) {
            //val int2 = lengthBytes.toByteArray().toHexInt()
            val startIndex = size - length
            if (startIndex > 0) {
                val headBytes = bytes.slice(startIndex until startIndex + 2)
                val head = headBytes.toByteArray().toHexString(false)

                if (head == LaserPeckerHelper.PACKET_HEAD) {
                    //真实的lp数据结构
                    val dataBytes = bytes.slice(startIndex + 2 until size - 4)
                    val data = dataBytes.toByteArray().toString(Charsets.UTF_8)
                    lpBinBean = data.fromJson<LPBinBean>()

                    //截取固件真实的数据内容
                    bytes = bytes.slice(0 until startIndex).toByteArray()

                    if (bytes.md5()?.lowercase() == lpBinBean?.md5) {
                        //固件验证成功
                    } else {
                        throw FirmwareException(_string(R.string.firmware_corrupted))
                    }
                }
            }
        }
    } catch (e: FirmwareException) {
        throw e
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val firmwareVersion = lpBinBean?.v ?: getFirmwareVersion()
    return FirmwareInfo(this, lastName(), firmwareVersion.toInt(), bytes, lpBinBean)
}