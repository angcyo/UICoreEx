package com.angcyo.bluetooth.fsc.laserpacker.command

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface ICommand {

    /**转换成十六进制指令, 可以包含空格.
     * 最后转换ByteArray时, 会剔除空格
     *
     *
     * 长度：指由功能码开始到较验和一共包含的字节数，这里为不定长Len,一般情况下最小长度为0x08。
     *
     * 功能码：用来指定该数据包功能
     *
     * Data ：数据包携带的(Len - 2)字节数据
     *
     * 较验和：包含“功能码”“数据内容”在内，由“功能码”开始至数据结束内容的较验和运算结果，高字节先发送。
     *
     * */
    fun toHexCommandString(): String

}