package com.angcyo.bluetooth.fsc.laserpacker.bean

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.core.vmApp
import com.angcyo.library.annotation.Dp
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.component.pad.isInPadMode

/**
 * 设备设置界面相关的一些设置配置信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/04/03
 */
data class DeviceSettingBean(
    /**需要显示[工作提示音]设置项的固件版本范围, null表示未配置, *和空字符表示所有都支持*/
    var showBuzzerRange: String? = null,
    /**设置项[安全状态]*/
    var showSafeRange: String? = null,
    /**设置项[自由模式]*/
    var showFreeRange: String? = null,
    /**设置项[雕刻方向]*/
    var showPrintDirRange: String? = null,
    /**设置项[向量图预览]*/
    var showGcodeViewRange: String? = null,
    /**设置项[使用GCode指令功率]*/
    var showGcodePowerRange: String? = null,
    /**设置项[第三轴]*/
    var showZFlagRange: String? = null,
    /**设置项[旋转轴]*/
    var showRFlagRange: String? = null,
    /**设置项[滑台]*/
    var showSFlagRange: String? = null,
    /**设置项[滑台批量雕刻]*/
    var showSRepRange: String? = null,
    /**设置项[正转/反转]*/
    var showDirRange: String? = null,
    /**设置项[主机触摸按键]*/
    var showKeyViewRange: String? = null,
    /**设置项[批量雕刻按键]*/
    var showKeyPrintRange: String? = null,
    /**设置项[红光常亮]*/
    var showIrDstRange: String? = null,
    /**LX1支持切割图层的模块,号分割多个
     * [com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel.isCutModule]*/
    var cutLayerModule: String? = null,
    /**显示风速设置的固件范围, 同时还要对应的模块支持
     * [com.angcyo.bluetooth.fsc.laserpacker.data.LaserTypeInfo.showPump]*/
    var showPumpRange: String? = null,
    /**支持自动激光的固件版本*/
    var autoCncRange: String? = null,
    /**显示速度参考的固件范围*/
    var showRefVelocityRange: String? = null,
    //region---Android端设置项---

    /**设置项[自动连接蓝牙]*/
    var showAutoConnectRange: String? = null,
    /**设置项[实验性]*/
    var showExperimentalRange: String? = null,
    /**设置项[上传日志]*/
    var showUploadLogRange: String? = null,
    /**设置项[固件升级]*/
    var showFirmwareUpdateRange: String? = null,
    /**创作版支持的固件范围*/
    var lpSupportFirmware: String? = null,
    /**[lpSupportFirmware]调试模式下使用*/
    var lpSupportFirmwareDebug: String? = null,
    /**额外使用批量雕刻指令的固件范围*/
    var lpBatchEngraveFirmware: String? = null,
    /**需要关闭的功能*/
    var closeCanvasItemsFun: String? = null,
    /**需要关闭的图片算法功能*/
    var closeImageEditItemsFun: String? = null,
    /**需要关闭的文本功能*/
    var closeTextEditItemsFun: String? = null,
    /**需要激活的功能项,
     * ```
     * _MultiplicationTable__PixelUnit_
     * ```
     * */
    var enableFun: String? = null,
    /**
     * 需要开放的功能项,
     * ```
     * xxx,xxx,xxx,
     * ```
     * */
    var openFun: String? = null,
    /**[com.angcyo.laserpacker.device.HawkEngraveKeys.maxEngraveItemCountLimit]*/
    var maxEngraveItemCount: Int? = null,
    /**用来更新Hawk的值, 指令.
     * [@key#int=value|@key#int=value|@key#int=value]*/
    var updateHawkCommand: String? = null,
    /**新功能提示字符串*/
    var newHawkKeyStr: String? = null,
    /**数据偏移帮助文档地址, 默认跳转*/
    var dataOffsetHelpUrl: String? = null,
    /**中文特殊跳转*/
    var dataOffsetHelpUrlZh: String? = null,
    /**旋转轴帮助文档地址, 默认跳转*/
    @Deprecated("当前属性只支持LP4")
    var rotateFlagHelpUrl: String? = null,
    /**中文特殊跳转*/
    @Deprecated("当前属性只支持LP4")
    var rotateFlagHelpUrlZh: String? = null,

    /**
     * 实例  https://faq.hingin.com/docs/aor_lp4_zh
     *       https://faq.hingin.com/docs/aor_lx1_en
     * 滑台: slide
     * 旋转轴: aor
     * 滑台多文件: slide_files
     * 第三轴: third_axis
     * */
    var flagHelpBase: String? = "https://faq.hingin.com/docs",

    /**日期推荐格式列表*/
    var dateFormatList: List<String>? = null,
    /**时间推荐格式列表*/
    var timeFormatList: List<String>? = null,
    /**国内环境导入文件支持的软件包名*/
    var zhImportFilePackageNameList: List<String>? = null,
    /**国外环境导入文件支持的软件包名*/
    var importFilePackageNameList: List<String>? = null,
    /**导入文件帮助链接, 配置了才有帮助按钮入口*/
    var importFileHelpUrl: String? = null,
    /**中文帮助文档地址*/
    var importFileHelpUrlZh: String? = null,

    /**变量文本日期格式说明, 配置了才有帮助按钮入口*/
    var dateFormatHelpUrl: String? = null,
    /**中文帮助文档地址*/
    var dateFormatHelpUrlZh: String? = null,

    /**变量文本时间格式说明, 配置了才有帮助按钮入口*/
    var timeFormatHelpUrl: String? = null,
    /**中文帮助文档地址*/
    var timeFormatHelpUrlZh: String? = null,

    /**二维码支持的格式集合*/
    var barcode2DTypeList: List<String>? = null,
    /**一维码支持的格式集合*/
    var barcode1DTypeList: List<String>? = null,
    /**2D条形码默认的生成宽高*/
    @Dp
    var barcodeSize: Int? = null,
    /**1D条形码默认的生成宽高*/
    @Dp
    var barcodeWidth: Int? = null,
    @Dp
    var barcodeHeight: Int? = null,
    /**条形码内容默认的边距*/
    @Dp
    var barcode2DMargin: Int? = null,
    @Dp
    var barcode1DMargin: Int? = null,
    /**条形码文本与内容的距离*/
    @MM
    var barcode1DTextMargin: Float = 1f,
    /**条形码文本默认字体大小*/
    @MM
    var barcode1DTextSize: Float = 4f,
    /**条形码文本默认对齐方式*/
    var barcode1DTextAlign: String? = "center",
    /**二维码和一维码默认的预览内容*/
    var barcode2DPreviewContent: String? = "123456",
    var barcode1DPreviewContent: String? = "123456",
    /**条形码生成的前后背景色值
     * ```
     * #ffffff
     * ```*/
    var barcodeBackgroundColor: String? = null,
    var barcodeForegroundColor: String? = null,
    /**是否要显示工程分享按钮*/
    var showProjectShare: Boolean = false,
    /**显示变量模板功能的固件范围*/
    var showVariableTextRange: String? = null,
    /**快捷输入指令列表*/
    var quickTextInputAction: List<QuickActionBean>? = null,
    /**文件名需要移除的字符*/
    var fileNameRegex: String? = "[\\/:*?\"<>|]",
    /**是否显示变量元素ico提示*/
    var showVariableElementIco: Boolean = true,
    /**复制元素时, 是否需要偏移位置*/
    var copyElementOffset: Boolean = true,

    //endregion---Android端设置项---

    //region---Ble UUID---

    var lp5BleServiceUuid: String = "0000abf0-0000-1000-8000-00805f9b34fb",
    var lp5BleNotifyUuid: String = "0000abf4-0000-1000-8000-00805f9b34fb",
    var lp5BleWriteUuid: String = "0000abf3-0000-1000-8000-00805f9b34fb",

    //endregion---Ble UUID---

    //region---功能开关---

    /**是否激活元素的快捷操作*/
    var enableQuickOperation: Boolean = true

    //endregion---功能开关---
)

/**当前的字符串范围string, 是否批量当前的固件版本*/
fun String?.matchesProductVersion(defOrNull: Boolean = false, defOrEmpty: Boolean = true): Boolean {
    val productInfo = vmApp<LaserPeckerModel>().productInfoData.value
    return VersionMatcher.matches(productInfo?.version, this, defOrNull, defOrEmpty)
}

val _enableQuickOperation: Boolean
    get() = _deviceSettingBean?.enableQuickOperation == true && !isInPadMode()

/**是否使用自动控制激光
 * [com.angcyo.gcode.GCodeWriteHandler.isAutoCnc]*/
val _isAutoCnc: Boolean
    get() = HawkEngraveKeys.autoCncEngraveSupportFirmware.matchesProductVersion() ||
            _deviceSettingBean?.autoCncRange?.matchesProductVersion() == true

val _showRefVelocity: Boolean
    get() = _deviceSettingBean?.showRefVelocityRange?.matchesProductVersion() == true