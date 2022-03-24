package com.angcyo.bluetooth.fsc

import com.angcyo.library.ex.padHexString
import com.angcyo.library.ex.toHexByteArray
import com.angcyo.library.ex.toHexString

/**
 * https://docs.qq.com/doc/DWE1MVnVOQ3RJSXZ1
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/03/24
 */
object LaserPeckerHelper {

    const val PACKET_HEAD = "AABB"

    /**移除所有空格*/
    fun String.trimCmd() = replace(" ", "")

    fun ByteArray.checksum(length: Int = 2): String = sumCheck(this, length).toHexString(true)

    fun String.checksum(length: Int = 2): String =
        sumCheck(toHexByteArray(), length).toHexString(true)

    /**
     * 校验和，经亲自验证数据没有错
     * https://blog.csdn.net/educast/article/details/52909524?locationNum=2&fps=1
     * @param bytes 需要计算校验和的byte数组
     * @param length 校验和结果的数据存储的字节数
     * @return 计算出的校验和数组
     */
    fun sumCheck(bytes: ByteArray, length: Int = 2): ByteArray {
        var sum: Long = 0
        val result = ByteArray(length)

        /** 逐Byte添加位数和  */
        for (byteMsg in bytes) {
            val num = if (byteMsg.toLong() >= 0) byteMsg.toLong() else byteMsg.toLong() + 256
            sum += num
        }
        /** end of for (byte byteMsg : msg)  */

        /** 位数和转化为Byte数组  */
        for (count in 0 until length) {
            result[length - count - 1] = (sum shr count * 8 and 0xff).toByte()
        }
        /** end of for (int liv_Count = 0; liv_Count < length; liv_Count++)  */
        return result
    }

    /**查询指令
     * [state] 0:表示查询工作状态 1:表示查询文件列表 2:表示查询设置状态 3:表示查询版本
     * [custom] 自定义的数据*/
    fun stateCmd(state: Byte, custom: Byte = 0): String {
        val data = "${state.toHexString()} ${custom.toHexString()}".padHexString(5)
        val check = data.checksum()
        val cmd = "$PACKET_HEAD 08 00 $data $check"
        return cmd
    }
}