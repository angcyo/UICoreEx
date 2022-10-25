package com.angcyo.bluetooth.fsc.laserpacker.command

/**
 * 自定义的指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/25
 */
class CustomCmd(val hex: String /*直接可以发送的指令*/) : BaseCommand() {

    override fun toHexCommandString(): String = hex

    override fun toCommandLogString(): String = "$hex 自定义指令"
}