package com.angcyo.laserpacker.device.firmware

/** LpBin数据结构, 拼接在原始bin文件后面的额外信息
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/11/04
 */
data class LPBinBean(
    /**n:固件名称*/
    val n: String?,
    /**t:构建时间*/
    val t: Long,
    /**v:固件版本*/
    val v: Long,
    /**d:版本描述*/
    val d: String?,
    /**r:升级范围[xx~xx xx~xx]*/
    val r: String?,
    /**固件内容数据的md5*/
    val md5: String?,
)
