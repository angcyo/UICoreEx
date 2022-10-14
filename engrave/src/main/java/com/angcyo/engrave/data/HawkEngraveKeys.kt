package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.HawkPropertyValue

/**
 * 数据持久化
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
object HawkEngraveKeys {

    /**最后一次设置的支架升降高度*/
    @MM
    var lastBracketHeight: Float by HawkPropertyValue<Any, Float>(1f)

    /**最后一次预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.5f)

    /**最后一次功率*/
    var lastPower: Int by HawkPropertyValue<Any, Int>(100)

    /**最后一次深度*/
    var lastDepth: Int by HawkPropertyValue<Any, Int>(10)

    /**最后一次的物理尺寸, 像素
     *长径, 大直径*/
    var lastDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    /**短径, 小直径*/
    var lastMinDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    /**最后一次的加速级别*/
    var lastPrecision: Int by HawkPropertyValue<Any, Int>(1)

    /**持久化当前雕刻的次数, 用来生成文件名*/
    var lastEngraveCount: Int by HawkPropertyValue<Any, Int>(0)

    //

    /**自动连接设备状态存储*/
    var AUTO_CONNECT_DEVICE: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否使用4点预览*/
    var USE_FOUR_POINTS_PREVIEW: Boolean by HawkPropertyValue<Any, Boolean>(false)

    //

    /**中心点预览的时候, 是否使用矩形的中心点坐标*/
    var enableRectCenterPreview: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**当前是否进入了矩形中心点预览模式*/
    var isRectCenterPreview: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否需要进入矩形中心点预览
     * C1 不支持, C1中心点预览还是 0x07 指令
     * [com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd]
     * */
    fun needRectCenterPreview(): Boolean {
        if (vmApp<LaserPeckerModel>().productInfoData.value?.isCI() == true) {
            return false
        }
        return enableRectCenterPreview
    }
}