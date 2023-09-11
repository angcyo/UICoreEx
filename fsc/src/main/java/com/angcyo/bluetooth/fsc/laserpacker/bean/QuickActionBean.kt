package com.angcyo.bluetooth.fsc.laserpacker.bean

/**
 * 快捷指令数据结构
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/11
 */
data class QuickActionBean(
    /**标签, 通常用于界面展示*/
    var label: String? = null,

    /**值, 用于业务*/
    var value: String? = null,
)