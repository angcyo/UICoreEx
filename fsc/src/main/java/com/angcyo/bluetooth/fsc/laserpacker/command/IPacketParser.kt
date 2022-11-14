package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.parse.*
import com.angcyo.library.ex.toHexByteArray

/**
 * 解析返回的包数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface IPacketParser<T> {

    fun parse(packet: ByteArray): T?
}

/**将指令的返回值hex字符串解析成对应的结构*/
fun String.parsePacketLog(): IPacketParser<*>? {
    val list = mutableListOf<IPacketParser<*>>()
    //list.add(QuerySettingParser())
    list.add(QueryStateParser())
    //list.add(QueryVersionParser())
    //list.add(QueryEngraveFileParser())
    //list.add(QueryLogParser())
    //list.add(QuerySafeCodeParser())
    list.add(EngravePreviewParser())
    list.add(BracketParser())
    list.add(EngraveReceiveParser())
    list.add(FirmwareUpdateParser())
    list.add(FileTransferParser())

    list.forEach {
        try {
            val result = it.parse(toHexByteArray()) as? IPacketParser<*>?
            if (result != null) {
                return result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}