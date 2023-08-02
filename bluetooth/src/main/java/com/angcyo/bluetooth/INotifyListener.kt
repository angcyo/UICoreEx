package com.angcyo.bluetooth

import com.clj.fastble.exception.BleException

/**
 * 蓝牙模块发上来的数据监听
 *
 * [com.clj.fastble.callback.BleNotifyCallback]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/08/02
 */

/**
 * [com.clj.fastble.callback.BleNotifyCallback.onNotifySuccess] 时2个参数都是空
 * [com.clj.fastble.callback.BleNotifyCallback.onNotifyFailure] 时,[exception]有值
 * [com.clj.fastble.callback.BleNotifyCallback.onCharacteristicChanged] 时,[data]有值
 *
 * */
typealias INotifyAction = (data: ByteArray?, exception: BleException?) -> Unit