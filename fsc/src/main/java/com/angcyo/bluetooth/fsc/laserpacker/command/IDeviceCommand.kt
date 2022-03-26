package com.angcyo.bluetooth.fsc.laserpacker.command

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface IDeviceCommand {

    /**转换成十六进制指令*/
    fun toHexCommandString(): String

}