package com.angcyo.bluetooth.fsc.laserpacker.command

/**
 * 解析返回的包数据
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */
interface IPacketParser<T> {

    fun parse(packet: ByteArray): T?
}