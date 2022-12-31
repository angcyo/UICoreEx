package com.angcyo.engrave.data

import androidx.annotation.Keep
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.library.annotation.FunctionConfig
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.component.LibHawkKeys
import com.angcyo.library.ex.isDebug

/**
 * 数据持久化
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */

@Keep
object HawkEngraveKeys {

    /**循环查询状态最小延迟间隔, 毫秒*/
    var minQueryDelayTime: Long by HawkPropertyValue<Any, Long>(1_000)

    /**最后一次设置的支架升降高度*/
    @MM
    var lastBracketHeight: Float by HawkPropertyValue<Any, Float>(1f)

    /**最后一次预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.5f)

    /**最后一次功率*/
    var lastPower: Int by HawkPropertyValue<Any, Int>(100)

    /**最后一次深度*/
    var lastDepth: Int by HawkPropertyValue<Any, Int>(10)

    /**最后一次的加速级别*/
    var lastPrecision: Int by HawkPropertyValue<Any, Int>(1)

    /**最后一次的激光类型*/
    var lastType: Int by HawkPropertyValue<Any, Int>(LaserPeckerHelper.LASER_TYPE_BLUE.toInt())

    /**最后一次的物理尺寸, 像素
     *长径, 大直径*/
    @Pixel
    var lastDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    /**短径, 小直径*/
    var lastMinDiameterPixel: Float by HawkPropertyValue<Any, Float>(300f)

    /**持久化当前雕刻的次数, 用来生成文件名*/
    var lastEngraveCount: Int by HawkPropertyValue<Any, Int>(0)

    /**保存工程的次数*/
    var lastProjectCount: Int by HawkPropertyValue<Any, Int>(0)

    /**雕刻失败后的重试次数*/
    var engraveRetryCount: Int by HawkPropertyValue<Any, Int>(3)

    /**最后一次传输的dpi*/
    var lastDpi: Float by HawkPropertyValue<Any, Float>(LaserPeckerHelper.DPI_254)

    //

    /**版画阈值*/
    var lastPrintThreshold: Float by HawkPropertyValue<Any, Float>(CanvasProjectItemBean.DEFAULT_THRESHOLD)

    /**印章阈值*/
    var lastSealThreshold: Float by HawkPropertyValue<Any, Float>(CanvasProjectItemBean.DEFAULT_THRESHOLD)

    /**黑白阈值*/
    var lastBWThreshold: Float by HawkPropertyValue<Any, Float>(CanvasProjectItemBean.DEFAULT_THRESHOLD)

    /**参数表网格数量
     * [com.angcyo.canvas.graphics.GraphicsKtxKt.addParameterComparisonTable]
     */
    var lastGridCount: Int by HawkPropertyValue<Any, Int>(10)

    /**功率*深度 <= this 时才需要添加到参数表
     * [com.angcyo.canvas.graphics.GraphicsKtxKt.addParameterComparisonTable]
     * */
    var lastPowerDepth: Int by HawkPropertyValue<Any, Int>(2500)

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

    /**是否显示设备原始历史, 本机不存在也显示*/
    var showDeviceHistory: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活传输数据时的索引检查*/
    var enableTransferIndexCheck: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活第三方GCode数据全转换*/
    var enableGCodeTransform: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活元素从上往下的雕刻顺序*/
    var enableItemTopOrder: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活雕刻时的信息渲染*/
    var enableRenderEngraveInfo: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活单元素自带的雕刻参数*/
    var enableItemEngraveParams: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活单元素传输雕刻, 一个传完雕一个, 这样可以突破同一时间雕刻item上限的问题*/
    var enableSingleItemTransfer: Boolean by HawkPropertyValue<Any, Boolean>(false) {
        LibHawkKeys.enableCanvasRenderLimit = !it
    }

    /**是否激活栅格化功能*/
    var enableRasterize: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活路径填充功能*/
    var enablePathFill: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    //

    /**需要关闭的功能
     * xx_xxx
     * [com.angcyo.canvas.laser.pecker.CanvasLayoutHelper.bindItems]
     * */
    var closeCanvasItemsFun: String by HawkPropertyValue<Any, String>("")

    /**需要关闭的图片算法功能
     * xx_xxx
     * [CanvasEditLayoutHelper.renderImageEditItems]
     * */
    var closeImageEditItemsFun: String by HawkPropertyValue<Any, String>("")

    /**需要关闭的文本功能
     * xx_xxx
     * [CanvasEditLayoutHelper.renderTextEditItems]
     * */
    var closeTextEditItemsFun: String by HawkPropertyValue<Any, String>("")

    /**最大的下位机能接收的文件大小 字节, 30MB */
    var maxTransferDataSize: Long by HawkPropertyValue<Any, Long>(28_000_000)

    /**最大的外部导入文件大小 字节, 5MB */
    var openFileDataSize: Long by HawkPropertyValue<Any, Long>(5_000_000)

    /**最大的外部导入文件行数, 10W行 */
    var openFileLineCount: Long by HawkPropertyValue<Any, Long>(10_0000)

    /**最小的文本字体大小, 像素*/
    @Pixel
    var minTextSize: Float by HawkPropertyValue<Any, Float>(5f)

    @Pixel
    var maxTextSize: Float by HawkPropertyValue<Any, Float>(500f)

    //---功能固件范围配置---

    /**
     * 多文件批量雕刻功能, 支持的固件范围
     * resValue "string", "lp_batch_engrave_firmware", '"650~699 6500~6599"'
     * */
    var batchEngraveSupportFirmware: String? by HawkPropertyValue<Any, String?>(null)
}