package com.angcyo.bluetooth.fsc.laserpacker.bean

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
    /**需要激活的功能项*/
    var enableFun: String? = null,
    /**[com.angcyo.laserpacker.device.HawkEngraveKeys.maxEngraveItemCountLimit]*/
    var maxEngraveItemCount: Int? = null,
    /**用来更新Hawk的值, 指令.
     * [@key#int=value|@key#int=value|@key#int=value]*/
    var updateHawkCommand: String? = null,
    /**新功能提示字符串*/
    var newHawkKeyStr: String? = null,
    //endregion---Android端设置项---
)