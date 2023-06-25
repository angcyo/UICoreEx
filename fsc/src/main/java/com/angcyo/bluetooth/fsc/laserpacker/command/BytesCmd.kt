package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.library.ex.size
import com.angcyo.library.ex.toSizeString

/**
 * 直接发送字节数据的指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/25
 */
data class BytesCmd(val bytes: ByteArray /*直接可以发送的指令数据*/) : BaseCommand() {

    override fun toByteArray(): ByteArray = bytes

    override fun toCommandLogString(): String {
        return "字节指令${bytes.size().toSizeString()}"
    }
}
