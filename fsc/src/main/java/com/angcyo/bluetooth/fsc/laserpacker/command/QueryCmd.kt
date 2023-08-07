package com.angcyo.bluetooth.fsc.laserpacker.command

import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd.Companion.QUERY_FILE_NAME_LIST
import com.angcyo.library.ex.removeAll
import com.angcyo.library.ex.toHexString

/**
 * 查询指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/26
 */

data class QueryCmd(
    val log: String,
    // 0:表示查询工作状态
    // 1:表示查询文件列表
    // 2:表示查询设置状态
    // 3:表示查询版本
    val state: Byte,
    val custom: Byte = 0, //自定义的数据
    /**
     * 当mount=0时查询U盘列表。
     * 当mount=1时查询SD卡文件列表
     *
     * [QUERY_FILE_NAME_LIST]
     * */
    val mount: Byte? = null //自定义的数据
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
        const val QUERY_FILE_NAME_LIST: Byte = 0x07
        const val QUERY_DEVICE_NAME: Byte = 0x08

        /**
         * ```
         * 当mount=0时查询U盘列表。
         * 当mount=1时查询SD卡文件列表
         * ```
         * */
        const val TYPE_USB = 0
        const val TYPE_SD = 1

        /**查询工作状态*/
        val workState: QueryCmd
            get() = QueryCmd("查询工作状态", QUERY_WORK)

        /**查询文件列表/历史记录*/
        val fileList: QueryCmd
            get() = QueryCmd("查询历史记录", QUERY_FILE)

        /**查询设置状态*/
        val settingState: QueryCmd
            get() = QueryCmd("查询设置状态", QUERY_SETTING)

        /**查询版本信息*/
        val version: QueryCmd
            get() = QueryCmd("查询版本信息", QUERY_VERSION)

        /**查询安全码和用户账号*/
        val safeCode: QueryCmd
            get() = QueryCmd("查询安全码和用户账号", QUERY_SAFE_CODE)

        /**查询日志
         * AABB080006000000000006*/
        val log: QueryCmd
            get() = QueryCmd("查询日志", QUERY_LOG)

        /**查询U盘文件名列表：(L5支持)
         * 2023年6月17日*/
        val fileUsbNameList: QueryCmd
            get() = QueryCmd("查询U盘文件名列表", QUERY_FILE_NAME_LIST, mount = TYPE_USB.toByte())

        /**查询Sd卡文件名列表：(L5支持)
         * 2023年6月17日*/
        val fileSdNameList: QueryCmd
            get() = QueryCmd("查询Sd卡文件名列表", QUERY_FILE_NAME_LIST, mount = TYPE_SD.toByte())

        /**查询设备蓝牙名称查询：(L5支持) //2023年8月2日*/
        val deviceName: QueryCmd
            get() = QueryCmd("查询设备蓝牙名称查询", QUERY_DEVICE_NAME)
    }

    //功能码
    override fun commandFunc(): Byte = QUERY_FUNC

    override fun toHexCommandString(): String {
        val cmd = commandByteWriter {
            write(commandFunc())
            write(state)
            write(custom)
            mount?.let {
                write(it)
            }
        }.toHexString()!!

        /*val dataLength = 8 //数据长度
        val data = buildString {
            append(commandFunc().toHexString())
            append(state.toHexString())
            append(custom.toHexString())
        }.padHexString(dataLength - LaserPeckerHelper.CHECK_SIZE)
        val check = data.checksum() //“功能码”和“数据内容”在内的校验和
        val cmd = "${LaserPeckerHelper.PACKET_HEAD} ${dataLength.toHexString()} $data $check"*/
        return cmd
    }

    override fun toCommandLogString(): String = buildString {
        append(toHexCommandString().removeAll())
        append(" 查询指令:$log")
    }
}
