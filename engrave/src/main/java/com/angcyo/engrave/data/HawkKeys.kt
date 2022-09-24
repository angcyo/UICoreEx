package com.angcyo.engrave.data

import com.angcyo.library.annotation.MM
import com.angcyo.library.component.HawkPropertyValue

/**
 * 数据持久化
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
object HawkKeys {

    /**最后一次设置的支架升降高度*/
    @MM
    var lastBracketHeight: Float by HawkPropertyValue<Any, Float>(1f)

    /**最后一次预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.5f)

    /**最后一次功率*/
    var lastPower: Int by HawkPropertyValue<Any, Int>(100)

    /**最后一次深度*/
    var lastDepth: Int by HawkPropertyValue<Any, Int>(10)

    /**最后一次的物理尺寸, 像素*/
    var lastDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

}