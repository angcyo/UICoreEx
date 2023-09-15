package com.angcyo.bluetooth.fsc.laserpacker.bean

/**
 * 气泵风速配置信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/15
 */
data class PumpConfigBean(
    /**风速等级
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.pump]*/
    val value: Int = 0
)
