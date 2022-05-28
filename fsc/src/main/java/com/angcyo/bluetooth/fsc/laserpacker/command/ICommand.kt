package com.angcyo.bluetooth.fsc.laserpacker.command

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface ICommand {

    /**转换成十六进制指令, 可以包含空格.
     * 最后转换ByteArray时, 会剔除空格*/
    fun toHexCommandString(): String

}