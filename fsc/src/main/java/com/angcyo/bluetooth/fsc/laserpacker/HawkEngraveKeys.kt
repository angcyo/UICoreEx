package com.angcyo.bluetooth.fsc.laserpacker

import android.graphics.Color
import androidx.annotation.Keep
import com.angcyo.http.base.toJson
import com.angcyo.library.L
import com.angcyo.library.annotation.FunctionConfig
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.component.hawk.HawkPropertyValue
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.component.hawk.LibLpHawkKeys
import com.angcyo.library.ex.hawkGetList
import com.angcyo.library.ex.hawkPutList
import com.angcyo.library.ex.isDebug
import com.angcyo.objectbox.laser.pecker.bean.TransferLayerConfigBean
import com.angcyo.objectbox.laser.pecker.bean.getLayerConfigList
import com.angcyo.objectbox.laser.pecker.bean.updateLayerConfig

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

    /**最小温度显示阈值*/
    var minTempShowThreshold: Int by HawkPropertyValue<Any, Int>(-127)

    /**倾斜角度显示阈值, 角度不等于这个值时, 则显示*/
    var angleShowThreshold: Int by HawkPropertyValue<Any, Int>(0)

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

    /**默认的激光频率,Q值*/
    var defaultLaserFrequency: Int by HawkPropertyValue<Any, Int>(60)

    /**[defaultLaserFrequency]*/
    var lastLaserFrequency: Int? by HawkPropertyValue<Any, Int?>(null)

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

    /**最后一次传输的文件名*/
    var lastTransferName: String? by HawkPropertyValue<Any, String?>(null)

    /**保存工程的次数*/
    var lastProjectCount: Int by HawkPropertyValue<Any, Int>(0)

    /**雕刻失败后的重试次数*/
    var engraveRetryCount: Int by HawkPropertyValue<Any, Int>(3)

    /**最后一次的图层id*/
    var lastLayerId: String by HawkPropertyValue<Any, String>(LaserPeckerHelper.LAYER_FILL)

    /**每个图层对应的dpi
     * [lastDpi]
     * [List<TransferLayerConfigBean>]
     * */
    var lastDpiLayerJson: String? by HawkPropertyValue<Any, String?>(null)

    /**[lastDpiLayerJson]*/
    var _lastLayerConfigList: List<TransferLayerConfigBean>?
        get() = lastDpiLayerJson?.getLayerConfigList()
        set(value) = run { lastDpiLayerJson = value?.toJson() }

    /**获取图层最后一次的dpi*/
    fun getLastLayerDpi(layerId: String?) =
        _lastLayerConfigList?.find { it.layerId == (layerId ?: LaserPeckerHelper.LAYER_FILL) }?.dpi
            ?: LaserPeckerHelper.DPI_254

    /**获取图层最后一次的dpi
     * [dpi] 需要[com.angcyo.laserpacker.device.filterLayerDpi]
     * [getLayerConfigJson]
     * */
    fun updateLayerDpi(layerId: String, dpi: Float) {
        lastLayerId = layerId
        lastDpiLayerJson = lastDpiLayerJson.updateLayerConfig(layerId, dpi)
    }

    /**[TransferLayerConfigBean.getAndUpdateLayerConfigJson]*/
    fun getLayerConfigJson(
        layerId: String?,
        dpi: Float,
        from: String? = lastDpiLayerJson
    ): String? = TransferLayerConfigBean.getAndUpdateLayerConfigJson(layerId, dpi, from)

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

    /**最大输入文本的长度*/
    var maxInputTextLengthLimit: Int by HawkPropertyValue<Any, Int>(100)

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

    /**是否激活GCode路径优化, 激活了路径优化要关闭GCode指令压缩, 因为优化算法不支持压缩GCode数据*/
    var enableGCodePathOpt: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活元素从上往下的雕刻顺序*/
    var enableItemTopOrder: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活雕刻时的信息渲染*/
    var enableRenderEngraveInfo: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活再雕一次时,先预览的功能*/
    var enableAgainEngravePreview: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活雕刻时雕刻图层的信息渲染*/
    var enableLayerEngraveInfo: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活单元素自带的雕刻参数*/
    var enableItemEngraveParams: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活单元素传输雕刻, 一个传完雕一个, 这样可以突破同一时间雕刻item上限的问题*/
    var enableSingleItemTransfer: Boolean by HawkPropertyValue<Any, Boolean>(false) {
        LibHawkKeys.enableCanvasRenderLimit = !it
    }

    /**是否激活栅格化功能*/
    var enableRasterize: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活路径填充功能*/
    var enablePathFill: Boolean by HawkPropertyValue<Any, Boolean>(true)

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

    /**是否激活wifi实验性功能配置*/
    var enableWifiFunConfig: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**抖动算法的模式
     *  1: floyd
     *  2: atkinson
     *  3: stucki def
     *  4: burkes
     *  5: jarvis
     *  6: sierra3
     *  _: stucki
     * */
    var ditherModeConfig: String? by HawkPropertyValue<Any, String?>(null)

    /**抖动模式*/
    val ditherMode: Int
        get() = when (ditherModeConfig?.lowercase()) {
            "floyd" -> 1
            "atkinson" -> 2
            "stucki" -> 3
            "burkes" -> 4
            "jarvis" -> 5
            "sierra3" -> 6
            else -> 0
        }

    //

    /**最大的下位机能接收的文件大小 字节, 30MB */
    var maxTransferDataSize: Long by HawkPropertyValue<Any, Long>(28_000_000)

    /**最大的外部导入文件大小 字节, 50MB */
    var openFileDataSize: Long by HawkPropertyValue<Any, Long>(50_242_880)

    /**最大的外部导入文件行数, 2W行 */
    var openFileLineCount: Long by HawkPropertyValue<Any, Long>(2_0000)

    /**最大的外部导入文件字节数, 1MB 1048576*/
    var openFileByteCount: Long by HawkPropertyValue<Any, Long>(1 * 1024 * 1024)

    /**需要解析log文件成图片时, 文件的最大字节大小. 超过此大小不预览*/
    var previewFileByteCount: Long by HawkPropertyValue<Any, Long>(10 * 1024 * 1024)

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
    var transferIndexSleep: Long by HawkPropertyValue<Any, Long>(6)

    //---功能固件范围配置---

    /**
     * 多文件批量雕刻功能, 支持的固件范围
     * resValue "string", "lp_batch_engrave_firmware", '"650~699 6500~6599"'
     * */
    var batchEngraveSupportFirmware: String? by HawkPropertyValue<Any, String?>(null)

    /**支持自动激光雕刻的固件范围*/
    var autoCncEngraveSupportFirmware: String? by HawkPropertyValue<Any, String?>(null)

    /**GCode转换成0x30路径数据的固件范围*/
    var gcodeUsePathDataSupportFirmware: String? by HawkPropertyValue<Any, String?>(null)

    /**[gcodeUsePathDataSupportFirmware] 0x30数据一段数据最多多少个点, 超过后下一段*/
    var pathDataPointLimit: Int by HawkPropertyValue<Any, Int>(3000)

    /**是否激活雕刻参数配置信息使用icon*/
    var enableConfigIcon: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活黑白算法处理时, 移除透明颜色*/
    var enableRemoveBWAlpha: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**图片算法处理时, 是否使用透明背景, 否则默认是白色*/
    var enableBitmapHandleBgAlpha: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活数据bounds严格模式, 则超出最佳范围就不允许预览和雕刻*/
    var enableDataBoundsStrict: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**激活工程的自动保存, 如:添加数据后自动save*/
    var enableProjectAutoSave: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**自动保存限流延迟*/
    var autoSaveProjectDelay: Long by HawkPropertyValue<Any, Long>(3_000)

    /**是否忽略雕刻过程遇到的错误, 不忽略则会进入暂停雕刻*/
    var ignoreEngraveError: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活流畅模式, 关闭多余的动画*/
    var enableLowMode: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活移动网络同步数据*/
    var enableMobileNetworkSync: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**导入外部文件格式时, 是否要使用分组, 分组之后各元素的样式会保留, 否则
     * [autoEnableImportGroupLength]
     * [autoEnableImportGroupLines]*/
    var enableImportGroup: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活svg导入时1:1毫米还原*/
    var enableImportSvgScale: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活svg导入时, 内部的group属性*/
    var enableImportSvgGroup: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**当SVG文本字符小于此值时自动激活[enableImportGroup]
     * [autoEnableImportGroupLength]*/
    var autoEnableImportGroupLength: Long by HawkPropertyValue<Any, Long>(20_000)

    /**当SVG文本字符的行数小于此值时自动激活[enableImportGroup]
     * [autoEnableImportGroupLines]*/
    var autoEnableImportGroupLines: Long by HawkPropertyValue<Any, Long>(2_000)

    /**2次间隔同步延迟时长*/
    var syncDelay: Long by HawkPropertyValue<Any, Long>(10 * 60 * 1000)

    /**wifi扫描超时*/
    var scanTimeout: Int by HawkPropertyValue<Any, Int>(100)

    /**当切片的数量大于此值时, 需要延迟*/
    var sliceCountDelay: Int by HawkPropertyValue<Any, Int>(50)

    /**延迟时长*/
    var sliceDelay: Long by HawkPropertyValue<Any, Long>(160)

    /**是否强制开启切割图层, 不管什么设备*/
    var enableForceCut: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活LP4的切割图层*/
    var enableLp4Cut: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活op xor操作后的fill操作*/
    var enableXorFill: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**激活预览指令的抖动处理*/
    var enablePreviewDebounce: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否激活灰度图片*/
    var enableGrey: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活画笔数据偏移*/
    var enableCalibrationOffset: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**是否使用新的抖动算法*/
    var useNewDithering: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**最后一次预览的元素宽度*/
    @MM
    var lastPreviewWidth: Float by HawkPropertyValue<Any, Float>(0f)

    @MM
    var lastPreviewHeight: Float by HawkPropertyValue<Any, Float>(0f)

    /**是否要保存所有工程图层的参数, 否则只保存有数据的图层参数*/
    var saveAllProjectOptions: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**字体需要预览的文本*/
    var typefacePreviewText: String? by HawkPropertyValue<Any, String?>(null)

    /**是否关闭在线配置的读取*/
    var closeOnlineConfig: Boolean by HawkPropertyValue<Any, Boolean>(false)

    //2023-8-2

    /**是否使用旧的wifi扫描设备*/
    var useOldWifiScan: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**强制显示wifi扫描列表
     * [LibLpHawkKeys.enableWifiConfig]
     * [HawkEngraveKeys.forceUseWifi]
     * */
    var forceUseWifi: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**最后一次是否是使用wifi连接的
     * 最后一次连接的设备类型
     * */
    var lastConnectDeviceType: Int by HawkPropertyValue<Any, Int>(LaserPeckerHelper.DEVICE_TYPE_NONE)

    /**NSD探测服务的类型*/
    var nsdServiceType: String by HawkPropertyValue<Any, String>("_http._tcp") //2024-2-29

    /**最后一次配置的wifi设备ip, 192.168.2.136
     * 用来用来确定是否配置过wifi设备
     * 有可能是`LP5-6666E6.local`
     * */
    var lastWifiIp: String? by HawkPropertyValue<Any, String?>(null)

    /**最后一次配置的wifi密码*/
    var lastWifiPassword: String? by HawkPropertyValue<Any, String?>(null)

    /**最后一次配置的wifi名称*/
    var lastWifiSSID: String? by HawkPropertyValue<Any, String?>(null)

    /**是否配置过wifi*/
    val isConfigWifi: Boolean
        get() = !lastWifiIp.isNullOrEmpty()

    /**默认的wifi端口*/
    var wifiPort: Int by HawkPropertyValue<Any, Int>(1111)//1111

    /**是否要记住wifi密码*/
    var rememberWifiPassword: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**扫描ip的范围
     * ```
     * 1~254
     * ```
     * */
    var scanIpRange: String? by HawkPropertyValue<Any, String?>("1~254") //1~254

    /**显示激光白光频率参数设置*/
    var showLaserFrequencyRange: String? by HawkPropertyValue<Any, String?>(null)

    /**扫描成功的ip缓存, 方便下一次快速扫描*/
    private var _scanIpCache: String? by HawkPropertyValue<Any, String?>(null)

    val scanIpCacheList: List<String>
        get() = HawkEngraveKeys::scanIpCache.name.hawkGetList()

    var scanIpCache: String = ""
        set(value) {
            HawkEngraveKeys::scanIpCache.name.hawkPutList(value)
        }

    /**扫描开始的ip地址*/
    var scanStartIp: Int by HawkPropertyValue<Any, Int>(0)

    /**扫描端口超时时间, 1s*/
    var scanPortTimeout: Int by HawkPropertyValue<Any, Int>(1000)

    /**灰度数据处理时, 需要使用的颜色通道*/
    var grayChannelType: Int by HawkPropertyValue<Any, Int>(Color.GRAY)

    /**是否使用调试的配置信息*/
    var useDebugConfig: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**切片浮雕之后, 支架自动回升*/
    var autoPickUp: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**切片浮雕是否使用循环指令*/
    var loopGcodeDataCmd: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活图片流转处理, 也就是每次图片的输出结果, 当做下一次输入的入参. 否则则使用默认的原图*/
    var enableBitmapFlowHandle: Boolean by HawkPropertyValue<Any, Boolean>(false)

    /**是否激活渲染器大小分布*/
    var enableRendererSizeFlat: Boolean by HawkPropertyValue<Any, Boolean>(isDebug())

    /**是否激活使用绘制后的路径进行路径填充*/
    var enableDrawPathFill: Boolean by HawkPropertyValue<Any, Boolean>(true)

    /**最小最大浮雕强度设置*/
    var minReliefStrength: Int by HawkPropertyValue<Any, Int>(1)
    var maxReliefStrength: Int by HawkPropertyValue<Any, Int>(20)

    /**最大切片粒度*/
    var maxSliceGranularity: Int by HawkPropertyValue<Any, Int>(100)

    /**最小切片下降高度*/
    var minSliceHeight: Float by HawkPropertyValue<Any, Float>(0.1f)

    /**最大切片下降高度*/
    var maxSliceHeight: Float by HawkPropertyValue<Any, Float>(1f)

    @MM
    var lastOutlineSpan: Float by HawkPropertyValue<Any, Float>(2f)
    var lastOutlineKeepHole: Boolean by HawkPropertyValue<Any, Boolean>(true)

    var lastTracerFilter: Int by HawkPropertyValue<Any, Int>(4)
    var lastTracerCorner: Float by HawkPropertyValue<Any, Float>(180f)
    var lastTracerLength: Float by HawkPropertyValue<Any, Float>(4f)
    var lastTracerSplice: Float by HawkPropertyValue<Any, Float>(0.5f)

    /**已经显示了提示的版本号*/
    var showPowerTipVersion: String? by HawkPropertyValue<Any, String?>(null)
    var showSpeedTipVersion: String? by HawkPropertyValue<Any, String?>(null)
    var showTimesTipVersion: String? by HawkPropertyValue<Any, String?>(null)

    var showSpeedConvertTipVersion: String? by HawkPropertyValue<Any, String?>(null)
}

/**最后一次是否是http设备连接*/
val lastIsHttpDevice: Boolean
    get() = HawkEngraveKeys.lastConnectDeviceType == LaserPeckerHelper.DEVICE_TYPE_HTTP