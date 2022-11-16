package com.angcyo.engrave.data

import androidx.annotation.Keep
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.library.annotation.FunctionConfig
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.HawkPropertyValue

/**
 * 数据持久化
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */

@Keep
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
    @Pixel
    var lastDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    /**短径, 小直径*/
    var lastMinDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    /**最后一次的加速级别*/
    var lastPrecision: Int by HawkPropertyValue<Any, Int>(1)

    /**持久化当前雕刻的次数, 用来生成文件名*/
    var lastEngraveCount: Int by HawkPropertyValue<Any, Int>(0)

    /**保存工程的次数*/
    var lastProjectCount: Int by HawkPropertyValue<Any, Int>(0)

    //

    /**版画阈值*/
    var lastPrintThreshold: Float by HawkPropertyValue<Any, Float>(CanvasProjectItemBean.DEFAULT_THRESHOLD)

    /**印章阈值*/
    var lastSealThreshold: Float by HawkPropertyValue<Any, Float>(CanvasProjectItemBean.DEFAULT_THRESHOLD)

    /**黑白阈值*/
    var lastBWThreshold: Float by HawkPropertyValue<Any, Float>(CanvasProjectItemBean.DEFAULT_THRESHOLD)

    //

    /**自动连接设备状态存储*/
    var AUTO_CONNECT_DEVICE: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否使用4点预览*/
    var USE_FOUR_POINTS_PREVIEW: Boolean by HawkPropertyValue<Any, Boolean>(false)

    //

    /**中心点预览的时候, 是否使用矩形的中心点坐标*/
    @FunctionConfig("物理中心点预览开关")
    var enableRectCenterPreview: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**最大允许雕刻的item数量*/
    var maxEngraveItemCountLimit: Int by HawkPropertyValue<Any, Int>(10)

    /**当设备数量大于10时, 显示过滤布局*/
    var showDeviceFilterCount: Int by HawkPropertyValue<Any, Int>(10)

    /**工程预览图, 输出的大小*/
    var projectOutSize: Int by HawkPropertyValue<Any, Int>(600)

    //

    /**需要关闭的功能
     * xx_xxx
     * [com.angcyo.canvas.laser.pecker.CanvasLayoutHelper.bindItems]
     * */
    var closeCanvasItemsFun: String by HawkPropertyValue<Any, String>("")

}