package com.angcyo.laserpacker.bean

import com.angcyo.laserpacker.bean.LPVariableBean.Companion.TYPE_DATE
import com.angcyo.laserpacker.bean.LPVariableBean.Companion.TYPE_NUMBER
import com.angcyo.laserpacker.bean.LPVariableBean.Companion.TYPE_TIME
import com.angcyo.library.component.parser.parseNumberTemplate
import com.angcyo.library.ex.uuid

/**
 * 变量模板, 变量类型
 *
 * [结构说明](https://www.showdoc.com.cn/2057569273029235/10477346881182875)
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/01
 */
data class LPVariableBean(
    //---FIXED
    /**变量类型*/
    var type: String = TYPE_FIXED,
    var content: String = "", // string 内容
    //---NUMBER
    var min: Long = 0, // 开始序号
    var max: Long = 9999, // 最大序号
    /**递变的增量*/
    var value: Long = 1, // 递变数(正负整数) 序号增量
    /**当前数字的序号, 或者当前文件的行数, 从0开始*/
    var current: Long = 0, // 当前序号
    /**雕刻多少次之后, 开始递变*/
    var step: Long = 1, // 雕刻几次开始递增
    /**格式
     * [TYPE_NUMBER] "0000"
     * [TYPE_DATE] "yyyy-MM-dd"
     * [TYPE_TIME] "HH:mm:ss"
     * */
    var format: String? = null, // 位数仅有0 几个0代表几个长度 0001
    var formatType: String = NUMBER_TYPE_DEC, // 字符格式化 dec十进制 HEX大写十六进制 hex小写十六进制
    //---DATE TIME
    /**时间增量单位*/
    var stepType: String = "D", // 递增方式: `D`:天  `M`:月 `Y`:年
    /**自动时间
     * 手动时间: [content] 13位时间戳*/
    var auto: Boolean = true, // 自动时间
    //---TXT
    var stepVal: Long = 1, // 递增量
    var fileName: String? = null, // 文件名
    var fileUri: String? = null, // 文件路径
    /**是否自动从头开始*/
    var reset: Boolean = false, // boolean 自动复位
    /**文本分割类型*/
    var splitType: String = "LINE", // 分割方式 1.0.0 只有行分割
    //---EXCEL
    var sheet: String? = null, // 工作表
    var column: String? = null, // 表格列
    //---cache
    var printCount: Long = 0, // 当前雕刻次数 `每当用户手动变更参数时都会置为0 重新计数`
    /**唯一标识符*/
    val key: String = uuid(),
) {
    companion object {
        /**变量类型: 固定文本*/
        const val TYPE_FIXED = "FIXED"

        /**变量类型: 序列号*/
        const val TYPE_NUMBER = "NUMBER"

        /**变量类型: 日期*/
        const val TYPE_DATE = "DATE"

        /**变量类型: 时间*/
        const val TYPE_TIME = "TIME"

        /**变量类型: 文本文件*/
        const val TYPE_TXT = "TXT"

        /**变量类型: 表格文件, 仅支持97~03 xls文档*/
        const val TYPE_EXCEL = "EXCEL"

        /**十进制*/
        const val NUMBER_TYPE_DEC = "dec"

        /**大写十六进制*/
        const val NUMBER_TYPE_HEX = "HEX"

        /**小写十六进制*/
        const val NUMBER_TYPE_HEX_LOWER = "hex"

        /**默认序号格式*/
        const val DEFAULT_NUMBER_FORMAT = "0000"
    }

    /**是否是回车*/
    val _isEnter: Boolean
        get() = type == TYPE_FIXED && content == "\n"

    /**重置缓存数据*/
    fun reset() {
        //current = 0
        printCount = 0
    }

    /**[TYPE_NUMBER]*/
    val numberFormatText: String
        get() {
            val format = format ?: DEFAULT_NUMBER_FORMAT
            return if (formatType == NUMBER_TYPE_HEX) {
                //大写 十六进制
                format.parseNumberTemplate(current.toString(16).uppercase())
            } else if (formatType == NUMBER_TYPE_HEX_LOWER) {
                //小写 十六进制
                format.parseNumberTemplate(current.toString(16).lowercase())
            } else {
                format.parseNumberTemplate(current.toString())
            }
        }

}
