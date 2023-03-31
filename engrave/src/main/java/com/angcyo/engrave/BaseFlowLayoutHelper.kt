package com.angcyo.engrave

import android.content.Context
import android.graphics.Color
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.bluetooth.fsc.laserpacker.parse.toDeviceStr
import com.angcyo.bluetooth.fsc.laserpacker.parse.toLaserPeckerVersionName
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.Strategy
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.showIn
import com.angcyo.core.tgStrokeLoadingCaller
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.laserpacker.device.HawkEngraveKeys
import com.angcyo.engrave.dslitem.preview.DeviceInfoTipItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.http.base.toJson
import com.angcyo.iview.BaseRecyclerIView
import com.angcyo.laserpacker.device.EngraveNotifyHelper
import com.angcyo.laserpacker.device.ble.BluetoothSearchHelper
import com.angcyo.laserpacker.device.ble.DeviceConnectTipActivity
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component._delay
import com.angcyo.library.component.isNotificationsEnabled
import com.angcyo.library.component.openNotificationSetting
import com.angcyo.library.component.pad.isInPadMode
import com.angcyo.library.ex.*
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.libCacheFile
import com.angcyo.library.toast
import com.angcyo.library.toastQQ
import com.angcyo.library.utils.fileNameTime
import com.angcyo.library.utils.writeTo
import com.angcyo.widget.span.span

/**
 * 雕刻流程相关布局基类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/06/07
 */
abstract class BaseFlowLayoutHelper : BaseRecyclerIView() {

    companion object {

        /**单元素雕刻参数配置*/
        const val ENGRAVE_FLOW_ITEM_CONFIG = 0x01

        /**雕刻流程: 预览前的配置
         * C1 握笔模块需要先校准对齐*/
        const val ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG = ENGRAVE_FLOW_ITEM_CONFIG shl 1

        /**雕刻流程: 预览中*/
        const val ENGRAVE_FLOW_PREVIEW = ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG shl 1

        /**数据传输之前的配置*/
        const val ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG = ENGRAVE_FLOW_PREVIEW shl 1

        /**自动开始传输数据, 并不会创建数据, 而是直接传输*/
        const val ENGRAVE_FLOW_AUTO_TRANSFER = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG shl 1

        /**雕刻流程: 雕刻数据传输中...*/
        const val ENGRAVE_FLOW_TRANSMITTING = ENGRAVE_FLOW_AUTO_TRANSFER shl 1

        /**雕刻流程: 雕刻前的配置*/
        const val ENGRAVE_FLOW_BEFORE_CONFIG = ENGRAVE_FLOW_TRANSMITTING shl 1

        /**雕刻流程: 雕刻中...*/
        const val ENGRAVE_FLOW_ENGRAVING = ENGRAVE_FLOW_BEFORE_CONFIG shl 1

        /**雕刻流程: 雕刻完成.*/
        const val ENGRAVE_FLOW_FINISH = ENGRAVE_FLOW_ENGRAVING shl 1

        /**安全提示, 是否不再提示, 每个版本提示一次*/
        const val KEY_SAFETY_TIPS = "SAFETY_TIPS_NOT_PROMPT"

        /**焦距提示, 是否不再提示, 每个版本提示一次*/
        const val KEY_FOCAL_DISTANCE_TIPS = "FOCAL_DISTANCE_TIPS_NOT_PROMPT"

        /**是否已经检查过通知权限*/
        var _isCheckedEngraveNotify = false
    }

    /**当前处于那个雕刻流程, -1未初始化*/
    var engraveFlow: Int = -1
        set(value) {
            val old = field
            field = value
            onEngraveFlowChanged(old, value)
        }

    /**是否初始化过*/
    val isInitialize: Boolean
        get() = engraveFlow != -1

    /**流程任务id, 建议每次显示页面时都创建一个新的任务id
     * [com.angcyo.engrave.BaseFlowLayoutHelper.generateFlowId]
     * [com.angcyo.engrave.BaseFlowLayoutHelper.clearFlowId]
     * */
    var flowTaskId: String? = null

    /**当前[engraveFlow]能够回退到的模式*/
    var engraveBackFlow: Int = 0

    /**雕刻绑定的界面*/
    var engraveCanvasFragment: IEngraveCanvasFragment? = null

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    //ble
    val fscBleApiModel = vmApp<FscBleApiModel>()

    /**是否循环检测设备状态*/
    var loopCheckDeviceState: Boolean = false

    /**是否暂停循环查询状态, 暂停不会退出循环*/
    var pauseLoopCheckDeviceState: Boolean = false

    /**是否处于最小化预览*/
    var isMinimumPreview: Boolean = false

    init {
        iViewLayoutId = R.layout.canvas_engrave_flow_layout
    }

    //

    override fun onIViewCreate() {
        super.onIViewCreate()
        bindDeviceState()

        if (isInPadMode()) {
            viewHolder?.gone(R.id.dialog_title_line_view)
            viewHolder?.view(R.id.lib_iview_wrap_layout)?.setBackgroundColor(Color.WHITE)
        }

        if (isDebugType()) {
            //重新刷新界面
            viewHolder?.click(R.id.lib_title_view) {
                renderFlowItems()
            }
        }
        if (isDebug()) {
            //长按标题分享工程
            viewHolder?.longClick(R.id.lib_title_view) {
                engraveCanvasFragment?.canvasDelegate?.apply {
                    val file = libCacheFile(fileNameTime("yyyy-MM-dd", CanvasConstant.PROJECT_EXT))
                    val bean = getCanvasDataBean(null, HawkEngraveKeys.projectOutSize)
                    val json = bean.toJson()
                    json.writeTo(file, false)
                    file.shareFile()
                }
            }
        }
    }

    override fun onIViewShow() {
        super.onIViewShow()
        renderFlowItems()
    }

    /**
     * 单元素雕刻参数设置
     * [com.angcyo.engrave.BaseFlowLayoutHelper.onEngraveFlowChanged]
     * [com.angcyo.engrave.BaseFlowLayoutHelper.onIViewRemove]
     * */
    var _engraveItemRenderer: DataItemRenderer? = null

    override fun onIViewRemove() {
        super.onIViewRemove()
        //重新分配一个id
        _engraveItemRenderer = null
        clearFlowId()
        loopCheckDeviceState = false
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
            //在预览界面
            if (laserPeckerModel.deviceStateData.value?.isModeEngravePreview() == true) {
                //关闭界面时, 如果在预览状态, 则退出预览, 并清除预览信息
                if (!isMinimumPreview) {
                    previewModel.previewInfoData.value = null
                    ExitCmd().enqueue()
                    syncQueryDeviceState()
                }
            }
        }
        //界面移除, 回归默认
        engraveFlow = 0
    }

    /**回退*/
    fun back() {
        if (engraveBackFlow > 0) {
            engraveFlow = engraveBackFlow
            renderFlowItems()
        } else {
            hide()
        }
    }

    override fun hide(end: (() -> Unit)?) {
        if (cancelable && engraveFlow == ENGRAVE_FLOW_PREVIEW && engraveBackFlow <= 0 && fscBleApiModel.haveDeviceConnected()) {
            //预览中, 等待机器完全退出之后, 再关闭界面
            engraveCanvasFragment?.fragment?.tgStrokeLoadingCaller { isCancel, loadEnd ->
                ExitCmd().enqueue { bean, error ->
                    loadEnd(bean, error)
                    if (error != null) {
                        toastQQ(error.message)
                    } else {
                        super.hide(end)
                    }
                }
            }
        } else if (cancelable && engraveBackFlow > 0 && engraveBackFlow != engraveFlow) {
            //需要回退
            engraveFlow = engraveBackFlow
            renderFlowItems()
        } else {
            super.hide(end)
        }
    }

    /**雕刻模式改变通知*/
    var onEngraveFlowChangedAction: (from: Int, to: Int) -> Unit = { _, _ ->

    }

    /**雕刻模式改变通知*/
    open fun onEngraveFlowChanged(from: Int, to: Int) {
        //非输入框界面, 禁止输入键盘弹出
        engraveCanvasFragment?.fragment?._vh?.enable(
            R.id.lib_soft_input_layout,
            to == ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG,
            false
        )

        if (to != ENGRAVE_FLOW_ITEM_CONFIG) {
            _engraveItemRenderer = null
        }
        //雕刻中保持常亮
        engraveCanvasFragment?.fragment?._vh?.itemView?.keepScreenOn = to == ENGRAVE_FLOW_ENGRAVING

        if (from == ENGRAVE_FLOW_PREVIEW) {
            //从预览中切换~
            if (!isMinimumPreview) {
                loopCheckDeviceState = false
                previewModel.clearPreviewInfo()
            }
        }
        if (to != ENGRAVE_FLOW_ENGRAVING && to != ENGRAVE_FLOW_TRANSMITTING) {
            loopCheckDeviceState = false
        }
        onEngraveFlowChangedAction(from, to)
    }

    /**分配一个流程id
     * 每次发送数据之前, 都生成一个新的任务.
     * 在任务完成后清空id
     * */
    open fun generateFlowId(): String {
        val old = flowTaskId
        if (old == null) {
            flowTaskId = uuid()
        }
        "生成流程id[$old]->[$flowTaskId]".writeToLog()
        return flowTaskId!!
    }

    /**清空流程id*/
    open fun clearFlowId() {
        "清空流程id[$flowTaskId]".writeToLog()
        flowTaskId = null
    }

    //

    @CallPoint
    open fun bindDeviceState() {
        //模式改变监听, 改变预览控制按钮的文本
        laserPeckerModel.deviceStateData.observe(this) {
            if (it?.isModeEngrave() == true) {
                //雕刻模式下, 需要刷新雕刻进度和雕刻时间
                _dslAdapter?.updateAllItem()
            }
        }

        //预览信息改变时, 刷新路径预览
        previewModel.previewInfoData.observe(this) {
            if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
                if (!isInPadMode()) {
                    _dslAdapter?.updateAllItem()
                }
            }
        }
    }

    /**检查是否可以开始预览*/
    fun checkCanStartPreview(engraveFragment: IEngraveCanvasFragment): Boolean {
        //检查是否有设备连接
        if (!vmApp<FscBleApiModel>().haveDeviceConnected()) {
            BluetoothSearchHelper.checkAndSearchDevice(engraveFragment.fragment)
            return false
        }

        //检查是否处于关机状态
        if (laserPeckerModel.isShutdownMode()) {
            toast(_string(R.string.device_shutdown_tip))
            return false
        }

        //检查是否激光头异常
        if (laserPeckerModel.isLaserException()) {
            toast(_string(R.string.laser_not_plugged_tip))
            return false
        }

        //检查是否已经显示
        if (isAttach() && engraveFlow > ENGRAVE_FLOW_ITEM_CONFIG) {
            return false
        }

        //是否需要恢复之前的雕刻状态
        if (engraveFragment.engraveFlowLayoutHelper.checkRestoreEngrave(engraveFragment)) {
            return false
        }

        //是否可以开始预览
        if (!engraveFragment.engraveFlowLayoutHelper.checkStartPreview()) {
            return false
        }

        return true
    }

    /**开始预览*/
    @CallPoint
    fun startPreview(engraveFragment: IEngraveCanvasFragment) {
        if (!checkCanStartPreview(engraveFragment)) {
            return
        }

        //安全提示弹窗
        engraveFragment.engraveFlowLayoutHelper.showSafetyTips(engraveFragment.fragment.fContext()) {
            //如果有第三轴, 还需要检查对应的配置
            _startPreview(engraveFragment)
        }
    }

    //开始预览
    fun _startPreview(engraveFragment: IEngraveCanvasFragment) {
        //未选中元素的情况下, 自动选中有效的所有元素
        val selectedRenderer = engraveFragment.canvasDelegate?.getSelectedRenderer()
        if (selectedRenderer == null) {
            val list = engraveFragment.canvasDelegate?.getAllDependRendererList()
            engraveFragment.canvasDelegate?.selectGroupRenderer?.selectedRendererList(
                list ?: emptyList(),
                Strategy.preview
            )
        }

        //
        /*engraveFlow = if (laserPeckerModel.isPenMode()) {
                //提前对笔模块校准
                ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG
            } else {
                ENGRAVE_FLOW_PREVIEW
            }*/ //2022-12-7
        engraveFlow = if (laserPeckerModel.isROpen()) {
            //旋转轴打开的情况下,提前设置物理直径
            ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG
        } else {
            ENGRAVE_FLOW_PREVIEW
        }
        showIn(engraveFragment.fragment, engraveFragment.flowLayoutContainer)
    }

    override fun onIViewReShow() {
        super.onIViewReShow()
        renderFlowItems()//重新渲染界面
    }

    /**根据不同的流程, 渲染不同的界面*/
    open fun renderFlowItems() {
        //no op
        if (isAttach()) {
            //
        }
    }

    /**持续检查工作作态*/
    fun delayLoopQueryDeviceState() {
        _delay(HawkEngraveKeys.minQueryDelayTime) {
            //延迟1秒后, 继续查询状态
            if (pauseLoopCheckDeviceState) {
                delayLoopQueryDeviceState()
            } else {
                laserPeckerModel.queryDeviceState { bean, error ->
                    if (error != null || loopCheckDeviceState) {
                        //出现了错误, 继续查询
                        delayLoopQueryDeviceState()
                    }
                }
            }
        }
    }

    /**显示焦距提示*/
    fun showFocalDistance(context: Context?, action: () -> Unit) {
        val KEY = "${KEY_FOCAL_DISTANCE_TIPS}${getAppVersionCode()}"
        if (KEY.hawkGetBoolean() && !BuildConfig.DEBUG) {
            //不再提示
            action()
            return
        }
        context?.messageDialog {
            dialogMessageLargeDrawable = _drawable(DeviceConnectTipActivity.getDeviceImageRes())
            dialogTitle = _string(R.string.focal_distance_tip)
            negativeButtonText = _string(R.string.dialog_negative)
            dialogNotPromptKey = KEY

            positiveButton { dialog, dialogViewHolder ->
                KEY.hawkPut(_dialogIsNotPrompt)
                dialog.dismiss()
                action()
            }
        }
    }

    /**显示预览安全提示框*/
    fun showSafetyTips(context: Context?, action: () -> Unit) {
        val KEY = "${KEY_SAFETY_TIPS}${getAppVersionCode()}"
        if (KEY.hawkGetBoolean() && !BuildConfig.DEBUG) {
            //不再提示
            action()
            return
        }
        context?.messageDialog {
            dialogTitle = span {
                appendImage(_drawable(R.mipmap.safe_tips))
                appendln()
                append(_string(R.string.engrave_warn))
            }
            dialogMessage = _string(R.string.size_safety_content)
            negativeButtonText = _string(R.string.dialog_negative)
            dialogNotPromptKey = KEY

            positiveButton { dialog, dialogViewHolder ->
                KEY.hawkPut(_dialogIsNotPrompt)
                dialog.dismiss()
                action()
            }
        }
    }

    /**第三轴未连接连接*/
    fun exDeviceNoConnectType(): Int {
        var noConnectType = 0
        if (laserPeckerModel.needShowExDeviceTipItem()) {
            val stateParser = laserPeckerModel.deviceStateData.value
            if (laserPeckerModel.isZOpen()) {
                val connect = stateParser?.zConnect == 1
                if (!connect) {
                    //z轴没有连接, 但是开启了z轴flag
                    noConnectType = 1
                }
            } else if (laserPeckerModel.isSOpen() || laserPeckerModel.isSRepMode()) {
                val connect = stateParser?.sConnect == 1
                if (!connect) {
                    noConnectType = 2
                }
            } else if (laserPeckerModel.isROpen()) {
                val connect = stateParser?.rConnect == 1
                if (!connect) {
                    noConnectType = 3
                }
            }
        }
        return noConnectType
    }

    /**预览时, 第三轴的连接状态提示*/
    fun previewExDeviceNoConnectTip() {
        val noConnectType = exDeviceNoConnectType()
        if (noConnectType > 0) {
            engraveCanvasFragment?.fragment?.fContext()?.messageDialog {
                dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                dialogMessage = _string(
                    R.string.device_ex_discontent_tips,
                    noConnectType.toDeviceStr()
                )

                onDismissListener = {
                    laserPeckerModel.queryDeviceState()
                }
            }
        }
    }

    //---

    /**检查扩展设备是否处于连接状态*/
    fun checkExDevice(action: () -> Unit) {
        val noConnectType = exDeviceNoConnectType()
        if (noConnectType > 0) {
            viewHolder?.context?.messageDialog {
                dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                dialogMessage = _string(
                    R.string.engrave_ex_discontent_tips,
                    noConnectType.toDeviceStr()
                )

                negativeButtonText = _string(R.string.dialog_negative)
                positiveButtonListener = { dialog, dialogViewHolder ->
                    dialog.dismiss()
                }

                positiveButtonText = _string(R.string.dialog_continue)
                positiveButtonListener = { dialog, dialogViewHolder ->
                    dialog.dismiss()
                    action()
                }

                onDismissListener = {
                    laserPeckerModel.queryDeviceState()
                }
            }
        } else {
            action()
        }
    }

    //---

    /**检查设备状态, 返回设备是否可以开始预览.
     * 主要检测当前设备是否正在雕刻中
     * */
    fun checkStartPreview(): Boolean {
        if (laserPeckerModel.deviceStateData.value?.isModeEngrave() == true) {
            engraveCanvasFragment?.fragment?.fContext()?.messageDialog {
                dialogTitle = _string(R.string.device_engrave_ing_title)
                dialogMessage = _string(R.string.device_engrave_ing_des)

                positiveButtonText = _string(R.string.engrave_stop)
                positiveButtonListener = { dialog, dialogViewHolder ->
                    dialog.dismiss()
                    ExitCmd().enqueue()
                    syncQueryDeviceState()
                }

                negativeButton(_string(R.string.dialog_negative)) { dialog, dialogViewHolder ->
                    dialog.dismiss()
                }
            }
            return false
        }
        return true
    }

    /**检查是否需要循环查询设备的状态,
     * 如果是其他app控制的机器状态, 则需要循环查询*/
    fun checkLoopQueryDeviceState(needStartLoop: Boolean = false) {
        val stateParser = laserPeckerModel.deviceStateData.value ?: return
        if (engraveModel._listenerEngraveState) {
            //已在循环查询状态
            return
        }
        if (needStartLoop || !stateParser.isModeIdle()) {
            //非空闲状态
            if (!loopCheckDeviceState) {
                loopCheckDeviceState = true
                delayLoopQueryDeviceState()
            }
        }
    }

    /**恢复雕刻状态, 请在初始化指令发送完成之后, 再去检查
     * [checkStartPreview]
     * @return true 需要恢复状态*/
    fun checkRestoreEngrave(engraveFragment: IEngraveCanvasFragment): Boolean {
        val stateParser = laserPeckerModel.deviceStateData.value ?: return false
        if (stateParser.isModeEngrave()) {
            if (stateParser.isEngraveStop()) {
                toastQQ(_string(R.string.engrave_stopping_tip))
                checkLoopQueryDeviceState()
                return true
            }

            //设备正在雕刻中, 则通过index查询是否是本机app发送的任务
            val transferData = EngraveFlowDataHelper.getTransferData(
                stateParser.index,
                deviceAddress = LaserPeckerHelper.lastDeviceAddress()
            )

            //如果是, 恢复界面, 如果不是, 则只是弹窗提示
            if (transferData != null && transferData.isTransfer && transferData.taskId != null) {
                flowTaskId = transferData.taskId
                engraveFlow = ENGRAVE_FLOW_ENGRAVING
                showIn(engraveFragment.fragment, engraveFragment.flowLayoutContainer)
                engraveModel.restoreEngrave(transferData.taskId)
                return true
            }
        }
        return false
    }

    //---

    /**检查是否有元素超出物理雕刻范围
     * @return true 表示有超出范围的元素*/
    fun checkOverflowBounds(): Boolean {
        val canvasDelegate = engraveCanvasFragment?.canvasDelegate ?: return false
        val rendererList = EngraveTransitionManager.getRendererList(canvasDelegate, null, true)
        var result = false
        for (renderer in rendererList) {
            if (renderer.getRotateBounds().isOverflowProductBounds()) {
                result = true
                break
            }
        }

        if (result) {
            val fContext = engraveCanvasFragment?.fragment?.fContext()
            fContext?.messageDialog {
                dialogTitle = _string(R.string.engrave_warn)
                dialogMessage = _string(R.string.engrave_overflow_bounds_message)
            }
        }

        return result
    }

    /**检查传输的数据是否合法
     *
     * @return true 数据合法, 允许雕刻*/
    fun checkTransferData(): Boolean {
        val fContext = engraveCanvasFragment?.fragment?.fContext()
        if (!LaserPeckerHelper.isSupportFirmware()) {
            val version = vmApp<LaserPeckerModel>().productInfoData.value?.softwareVersion
            fContext?.messageDialog {
                dialogTitle = _string(R.string.engrave_warn)
                dialogMessage = _string(
                    R.string.unsupported_firmware_tip,
                    version?.toLaserPeckerVersionName() ?: "0"
                )
            }
            return false
        }

        val canvasDelegate = engraveCanvasFragment?.canvasDelegate
        if (canvasDelegate != null) {
            val allRendererList = EngraveTransitionManager.getRendererList(canvasDelegate)
            if (allRendererList.isEmpty()) {
                fContext?.messageDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = _string(R.string.no_data_transfer)
                }
                return false
            }

            if (!HawkEngraveKeys.enableSingleItemTransfer &&
                allRendererList.size() > HawkEngraveKeys.maxEngraveItemCountLimit
            ) {
                fContext?.messageDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = _string(
                        R.string.limit_engrave_count_tip,
                        HawkEngraveKeys.maxEngraveItemCountLimit
                    )
                }
                return false
            }
        }
        //验证数据
        return EngraveDataValidation.validation(
            engraveCanvasFragment?.fragment?.fContext(),
            canvasDelegate
        )
    }

    //---

    /**检查雕刻通知*/
    fun checkEngraveNotify(action: () -> Unit) {
        if (_isCheckedEngraveNotify) {
            action()
        } else {
            EngraveNotifyHelper.createEngraveChannel()
            val fContext = engraveCanvasFragment?.fragment?.fContext()
            if (!isNotificationsEnabled()) {
                //未打开通知
                fContext?.messageDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = _string(R.string.open_notify_tip)
                    negativeButton { dialog, dialogViewHolder ->
                        _isCheckedEngraveNotify = true
                        dialog.dismiss()
                        action()
                    }
                    needPositiveButton { dialog, dialogViewHolder ->
                        dialog.dismiss()
                        openNotificationSetting()
                    }
                }
            } else if (!EngraveNotifyHelper.isChannelEnable()) {
                //未打开对应的通道
                fContext?.messageDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = _string(R.string.open_notify_channel_tip)
                    negativeButton { dialog, dialogViewHolder ->
                        _isCheckedEngraveNotify = true
                        dialog.dismiss()
                        action()
                    }
                    needPositiveButton { dialog, dialogViewHolder ->
                        dialog.dismiss()
                        EngraveNotifyHelper.openChannelSetting()
                    }
                }
            } else {
                //可以通知
                _isCheckedEngraveNotify = true
                action()
            }
        }
    }

    //--

    /**判断是否需要设备信息显示. 温度/角度*/
    fun DslAdapter.renderDeviceInfoIfNeed() {
        val info = DeviceInfoTipItem.deviceInfoTip()
        if (info.isNotBlank()) {
            DeviceInfoTipItem()() {
                itemTip = info
            }
        }
    }
}

/**是否进入了雕刻流程, 这种状态下禁止画板手势操作.
 *预览之后, 就不允许调整元素  */
fun Int.isEngraveFlow() = this > BaseFlowLayoutHelper.ENGRAVE_FLOW_PREVIEW