package com.angcyo.engrave.data

/**
 * 雕刻预览的信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/29
 */
data class EngravePreviewInfo(

    /**正在预览的item*/
    var itemUuid: String? = null,

    /**4点预览的旋转标识*/
    var rotate: Float? = null,
)
