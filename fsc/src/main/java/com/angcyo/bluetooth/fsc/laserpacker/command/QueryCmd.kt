package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper.checksum
import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toHexString

/**
 * 查询指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */

data class QueryCmd(
    // 0:表示查询工作状态
    // 1:表示查询文件列表
    // 2:表示查询设置状态
    // 3:表示查询版本
    val state: Byte,
    val custom: Byte = 0 //自定义的数据
) : BaseCommand() {

    companion object {

        /**指令*/
        const val QUERY_FUNC: Byte = 0x00

        /**指令状态*/
        const val QUERY_WORK: Byte = 0x00
        const val QUERY_FILE: Byte = 0x01
        const val QUERY_SETTING: Byte = 0x02
        const val QUERY_VERSION: Byte = 0x03
        const val QUERY_SAFE_CODE: Byte = 0x04
        const val QUERY_LOG: Byte = 0x06

        /**查询工作状态*/
        val workState: QueryCmd
            get() = QueryCmd(QUERY_WORK)

        /**查询文件列表/历史记录*/
        val fileList: QueryCmd
            get() = QueryCmd(QUERY_FILE)

        /**查询设置状态*/
        val settingState: QueryCmd
            get() = QueryCmd(QUERY_SETTING)

        /**查询版本信息*/
        val version: QueryCmd
            get() = QueryCmd(QUERY_VERSION)

        /**查询安全码和用户账号*/
        val safeCode: QueryCmd
            get() = QueryCmd(QUERY_SAFE_CODE)

        /**查询日志
         * AABB080006000000000006*/
        val log: QueryCmd
            get() = QueryCmd(QUERY_LOG)
    }

    //功能码
    override fun commandFunc(): Byte = QUERY_FUNC

    override fun toHexCommandString(): String {
        val dataLength = 8 //数据长度
        val data = buildString {
            append(commandFunc().toHexString())
            append(state.toHexString())
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        append(" 查询:")
        append(
            when (state) {
                0x00.toByte() -> "工作状态"
                0x01.toByte() -> "历史记录"
                0x02.toByte() -> "设置状态"
                0x03.toByte() -> "版本信息"
                0x04.toByte() -> "用户账号"
                0x06.toByte() -> "日志"
                else -> "Unknown"
            }
        )
    }
}
