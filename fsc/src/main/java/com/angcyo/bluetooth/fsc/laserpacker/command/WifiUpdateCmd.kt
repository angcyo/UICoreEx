package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.library.ex.removeAll

/**
 * Wifi模块固件升级指令, LP5
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/01/27
 */
data class WifiUpdateCmd(
    val url: String,
    val state: Byte = 0x3,
    val custom: Byte = 0,
) : BaseCommand() {

    override fun commandFunc(): Byte = 0x08

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        when (state) {
            0x03.toByte() -> {
                append(" WIFI固件升级:${url}")
            }
        }
    }

    override fun toByteArray(): ByteArray = commandByteWriter {
        write(commandFunc())
        write(state)
        write(custom)
        write("\"")
        write(url)
        write("\"")
        write(0)//结束字符
    }
}