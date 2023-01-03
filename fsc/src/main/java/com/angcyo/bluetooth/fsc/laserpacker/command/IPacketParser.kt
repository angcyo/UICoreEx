package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.parse.*
import com.angcyo.library.ex.toHexByteArray

/**
 * 解析返回的包数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface IPacketParser<T> {

    /**解析指令返回的字节数组[ByteArray]*/
    fun parse(packet: ByteArray): T?
}

/**将指令的返回值hex字符串解析成对应的结构
 * [func] 功能码, 更具指定的功能码, 解析对应的返回值*/
fun ByteArray.parseResultPacketLog(func: Int?, state: Int?): IPacketParser<*>? {
    val bytes = this
    return when (func) {
        QueryCmd.QUERY_FUNC.toInt() -> {
            return when (state) {
                QueryCmd.QUERY_WORK.toInt() -> QueryStateParser().parse(bytes)
                QueryCmd.QUERY_FILE.toInt() -> QueryEngraveFileParser().parse(bytes)
                QueryCmd.QUERY_SETTING.toInt() -> QuerySettingParser().parse(bytes)
                QueryCmd.QUERY_VERSION.toInt() -> QueryVersionParser().parse(bytes)
                QueryCmd.QUERY_LOG.toInt() -> QueryLogParser().parse(bytes)
                else -> MiniReceiveParser().parse(bytes)
            }
        }
        else -> MiniReceiveParser().parse(bytes)
    }
}

fun String.parseResultPacketLog(func: Int?, state: Int?): IPacketParser<*>? {
    val bytes = toHexByteArray()
    return bytes.parseResultPacketLog(func, state)
}