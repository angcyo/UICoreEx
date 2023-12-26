package com.angcyo.bluetooth.fsc.laserpacker.bean

/**
 * AT指令返回值
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since  2023-12-20
 */

/**U盘/sd卡文件信息*/
data class FileIndexBean(
    var index: Int = -1,
    var name: String? = null,
    /**
     * [TYPE_SD]
     * [TYPE_USB]
     * */
    var mount: Int = 1
)