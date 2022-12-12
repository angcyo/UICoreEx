package com.angcyo.engrave

import android.content.Context
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.ExitCmd
import com.angcyo.bluetooth.fsc.laserpacker.isOverflowProductBounds
import com.angcyo.bluetooth.fsc.laserpacker.parse.toLaserPeckerVersionName
import com.angcyo.bluetooth.fsc.laserpacker.syncQueryDeviceState
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.core.showIn
import com.angcyo.core.vmApp
import com.angcyo.dialog.messageDialog
import com.angcyo.engrave.BaseFlowLayoutHelper.Companion.ENGRAVE_FLOW_FINISH
import com.angcyo.engrave.BaseFlowLayoutHelper.Companion.ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG
import com.angcyo.engrave.ble.DeviceConnectTipActivity
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.engrave.transition.EngraveTransitionManager
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.http.base.toJson
import com.angcyo.iview.BaseRecyclerIView
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component._delay
import com.angcyo.library.component.isNotificationsEnabled
import com.angcyo.library.component.openNotificationSetting
import com.angcyo.library.ex.*
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.libCacheFile
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

        /**雕刻流程: 预览前的配置
         * C1 握笔模块需要先校准对齐*/
        const val ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG = 0x01

        /**雕刻流程: 预览中*/
        const val ENGRAVE_FLOW_PREVIEW = ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG shl 1

        /**数据传输之前的配置*/
        const val ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG = ENGRAVE_FLOW_PREVIEW shl 1

        /**雕刻流程: 雕刻数据传输中...*/
        const val ENGRAVE_FLOW_TRANSMITTING = ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG shl 1

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

    /**当前处于那个雕刻流程*/
    var engraveFlow: Int = 0
        set(value) {
            val old = field
            field = value
            onEngraveFlowChanged(old, value)
        }

    /**流程任务id, 建议每次显示页面时都创建一个新的任务id*/
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

    /**是否循环检测设备状态*/
    var loopCheckDeviceState: Boolean = false

    //

    override fun onIViewCreate() {
        super.onIViewCreate()
        bindDeviceState()

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

    override fun onIViewRemove() {
        super.onIViewRemove()
        //重新分配一个id
        clearFlowId()
        loopCheckDeviceState = false
        if (engraveFlow == ENGRAVE_FLOW_PREVIEW) {
            //在预览界面
            if (laserPeckerModel.deviceStateData.value?.isModeEngravePreview() == true) {
                //关闭界面时, 如果在预览状态, 则退出预览, 并清除预览信息
                previewModel.previewInfoData.value = null
                ExitCmd().enqueue()
                syncQueryDeviceState()
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
        }
    }

    override fun hide(end: (() -> Unit)?) {
        if (cancelable && engraveBackFlow > 0 && engraveBackFlow != engraveFlow) {
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
        //雕刻中保持常亮
        engraveCanvasFragment?.fragment?._vh?.itemView?.keepScreenOn = to == ENGRAVE_FLOW_ENGRAVING

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
        if (flowTaskId == null) {
            flowTaskId = uuid()
        }
        return flowTaskId!!
    }

    /**清空流程id*/
    open fun clearFlowId() {
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
                _dslAdapter?.updateAllItem()
            }
        }
    }

    /**开始预览*/
    @CallPoint
    fun startPreview() {
        /*engraveFlow = if (laserPeckerModel.isPenMode()) {
            ENGRAVE_FLOW_PREVIEW_BEFORE_CONFIG
        } else {
            ENGRAVE_FLOW_PREVIEW
        }*/ //2022-12-7
        engraveFlow = ENGRAVE_FLOW_PREVIEW
    }

    /**根据不同的流程, 渲染不同的界面*/
    open fun renderFlowItems() {
        //no op
    }

    /**持续检查工作作态*/
    fun delayLoopQueryDeviceState() {
        _delay(HawkEngraveKeys.minQueryDelayTime) {
            //延迟1秒后, 继续查询状态
            laserPeckerModel.queryDeviceState { bean, error ->
                if (error != null || loopCheckDeviceState) {
                    //出现了错误, 继续查询
                    delayLoopQueryDeviceState()
                }
            }
        }
    }

    /**显示焦距提示*/
    fun showFocalDistance(context: Context?, action: () -> Unit) {
        val KEY = "${KEY_FOCAL_DISTANCE_TIPS}${getAppVersionCode()}"
        if (KEY.hawkGetBoolean()) {
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
        if (KEY.hawkGetBoolean()) {
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

    fun Int.toDeviceNoConnectStr(): String = when (this) {
        1 -> _string(R.string.device_ex_z_label)
        2 -> _string(R.string.device_ex_s_label)
        3 -> _string(R.string.device_ex_r_label)
        else -> ""
    }

    /**预览时, 第三轴的连接状态提示*/
    fun previewExDeviceNoConnectTip() {
        val noConnectType = exDeviceNoConnectType()
        if (noConnectType > 0) {
            engraveCanvasFragment?.fragment?.fContext()?.messageDialog {
                dialogMessageLeftIco = _drawable(R.mipmap.safe_tips)
                dialogMessage = _string(
                    R.string.device_ex_discontent_tips,
                    noConnectType.toDeviceNoConnectStr()
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
                    noConnectType.toDeviceNoConnectStr()
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
    fun checkLoopQueryDeviceState() {
        val stateParser = laserPeckerModel.deviceStateData.value ?: return
        if (engraveModel._listenerEngraveState) {
            //已在循环查询状态
            return
        }
        if (!stateParser.isModeIdle()) {
            //非空闲状态
            if (!loopCheckDeviceState) {
                loopCheckDeviceState = true
                delayLoopQueryDeviceState()
            }
        }
    }

    /**恢复雕刻状态, 请在初始化指令发送完成之后, 再去检查
     * @return true 需要恢复状态*/
    fun checkRestoreEngrave(fragment: AbsLifecycleFragment, group: ViewGroup? = null): Boolean {
        val stateParser = laserPeckerModel.deviceStateData.value ?: return false
        if (stateParser.isModeEngrave()) {
            if (stateParser.isEngraveStop()) {
                toastQQ(_string(R.string.engrave_stopping_tip))
                checkLoopQueryDeviceState()
                return true
            }

            //设备正在雕刻中, 则通过index查询是否是本机app发送的任务
            val transferData = EngraveFlowDataHelper.getTransferData(stateParser.index)

            //如果是, 恢复界面, 如果不是, 则只是弹窗提示
            if (transferData != null && transferData.isTransfer && transferData.taskId != null) {
                flowTaskId = transferData.taskId
                engraveFlow = ENGRAVE_FLOW_ENGRAVING
                showIn(fragment, group)
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

            if (allRendererList.size() > HawkEngraveKeys.maxEngraveItemCountLimit) {
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
            val fContext = engraveCanvasFragment?.fragment?.fContext()
            if (!isNotificationsEnabled()) {
                //未打开通知
                fContext?.messageDialog {
                    dialogTitle = _string(R.string.engrave_warn)
                    dialogMessage = "打开通知,接收通知进度?"
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
                    dialogMessage = "打开雕刻通道,接收通知进度?"
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

}

/**是否进入了雕刻流程, 并且非雕刻完成*/
fun Int.isEngraveFlow() = this in ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG until ENGRAVE_FLOW_FINISH