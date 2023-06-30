package com.angcyo.bluetooth.fsc.annotation

/**
 * 当前设备指令, 需要激光先出光
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/06/30
 */
@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class NeedLaserLight
