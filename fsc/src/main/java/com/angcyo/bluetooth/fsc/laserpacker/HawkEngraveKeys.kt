package com.angcyo.bluetooth.fsc.laserpacker

import androidx.annotation.Keep
import com.angcyo.library.L
import com.angcyo.library.annotation.FunctionConfig
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.HawkPropertyValue
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.isDebug

/**
 * 数据持久化
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */

@Keep
object HawkEngraveKeys {

    /**默认的阈值*/
    val DEFAULT_THRESHOLD: Float = LibHawkKeys.grayThreshold.toFloat()

    /**超时时长*/
    val receiveTimeout: Long by HawkPropertyValue<Any, Long>(3_000)

    /**[receiveTimeout]最大*/
    val receiveTimeoutMax: Long by HawkPropertyValue<Any, Long>(30_000)

    //---

    /**循环查询状态最小延迟间隔, 毫秒*/
    var minQueryDelayTime: Long by HawkPropertyValue<Any, Long>(1_000)

    /**最后一次设置的支架升降高度*/
    @MM
    var lastBracketHeight: Float by HawkPropertyValue<Any, Float>(1f)

    /**最后一次预览光功率设置 [0~1f]*/
    var lastPwrProgress: Float by HawkPropertyValue<Any, Float>(0.1f)

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

    /**物理尺寸输入的精度, 小数点后几位*/
    var diameterPrecision: Int by HawkPropertyValue<Any, Int>(2)

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

    /**最大的选择添加图片的数量*/
    var maxSelectorPhotoCount: Int by HawkPropertyValue<Any, Int>(9)

    //

    /**版画阈值*/
    var lastPrintThreshold: Float by HawkPropertyValue<Any, Float>(DEFAULT_THRESHOLD)

    /**印章阈值*/
    var lastSealThreshold: Float by HawkPropertyValue<Any, Float>(DEFAULT_THRESHOLD)

    /**黑白阈值*/
    var lastBWThreshold: Float by HawkPropertyValue<Any, Float>(DEFAULT_THRESHOLD)

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
    var maxEngraveItemCountLimit: Int by HawkPropertyValue<Any, Int>(30)

    /**当设备数量大于10时, 显示过滤布局*/
    var showDeviceFilterCount: Int by HawkPropertyValue<Any, Int>(10)

    /**工程预览图, 输出的大小*/
    var projectOutSize: Int by HawkPropertyValue<Any, Int>(600)

    /**激活传输数据时的索引检查*/
    var enableTransferIndexCheck: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活第三方GCode数据全转换*/
    var enableGCodeTransform: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活压缩输出GCode*/
    var enableGCodeShrink: Boolean by HawkPropertyValue<Any, Boolean>(true)

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

    /**是否像素单位*/
    var enablePixelUnit: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活参数对照表*/
    var enableParameterComparisonTable: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活乘法口诀表*/
    var enableMultiplicationTable: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活视力表*/
    var enableVisualChartTable: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活后台雕刻, 在雕刻中可以关闭页面*/
    var enableBackEngrave: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否关闭抖动算法, 强制使用灰度图片算法?*/
    var forceGrey: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活雕刻历史记录中的设备地址显示[EngraveHistoryItem]*/
    var enableShowHistoryAddress: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活实验性功能*/
    var enableExperimental: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活Z轴的无差别px选择*/
    var enableZFlagPx: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活云端存储功能*/
    var enableCloudStorage: Boolean by HawkPropertyValue<Any, Boolean>(false)

    //

    /**最大的下位机能接收的文件大小 字节, 30MB */
    var maxTransferDataSize: Long by HawkPropertyValue<Any, Long>(28_000_000)

    /**最大的外部导入文件大小 字节, 50MB */
    var openFileDataSize: Long by HawkPropertyValue<Any, Long>(50_242_880)

    /**最大的外部导入文件行数, 2W行 */
    var openFileLineCount: Long by HawkPropertyValue<Any, Long>(2_0000)

    /**最大的外部导入文件字节数, 1MB 1048576*/
    var openFileByteCount: Long by HawkPropertyValue<Any, Long>(1 * 1024 * 1024)

    /**最小的文本字体大小, 像素*/
    @Pixel
    var minTextSize: Float by HawkPropertyValue<Any, Float>(5f)

    @Pixel
    var maxTextSize: Float by HawkPropertyValue<Any, Float>(500f)

    /**最大显示传输文件预览图的数量*/
    var maxShowTransferImageCount: Int by HawkPropertyValue<Any, Int>(3)

    /**用来控制雕刻数据的日志输出级别
     * [com.angcyo.engrave2.transition.EngraveTransitionHelper]
     * [L.NONE]     //不输出日志
     * [L.DEBUG]    //输出基础日志
     * [L.INFO]     //输出详细日志
     * [L.WARN]     //输出全部日志
     * */
    var engraveDataLogLevel: Int by HawkPropertyValue<Any, Int>(L.WARN)

    /**图片尺寸小于这个值时, 才开启预览日志输出*/
    var engraveBitmapLogSize: Int by HawkPropertyValue<Any, Int>(2000)

    /**是否检查32位的手机, 如果是32位的手机, 有些功能不开放*/
    var checkCpu32: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**保存工程时, 是否要保存过滤后的图片*/
    var saveFilterBitmap: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**传输数据索引生成时需要休眠的时长, 防止撞索引*/
    var transferIndexSleep: Long by HawkPropertyValue<Any, Long>(0)

    //---功能固件范围配置---

    /**
     * 多文件批量雕刻功能, 支持的固件范围
     * resValue "string", "lp_batch_engrave_firmware", '"650~699 6500~6599"'
     * */
    var batchEngraveSupportFirmware: String? by HawkPropertyValue<Any, String?>(null)

    /**是否激活雕刻参数配置信息使用icon*/
    var enableConfigIcon: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活黑白算法处理时, 移除透明颜色*/
    var enableRemoveBWAlpha: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**图片算法处理时, 是否使用透明背景, 否则默认是白色*/
    var enableBitmapHandleBgAlpha: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活数据bounds严格模式, 则超出最佳范围就不允许预览和雕刻*/
    var enableDataBoundsStrict: Boolean by HawkPropertyValue<Any, Boolean>(false)
}