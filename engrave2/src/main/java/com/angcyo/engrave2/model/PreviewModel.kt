package com.angcyo.engrave2.model

import android.graphics.RectF
import androidx.annotation.AnyThread
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave2.data.PreviewInfo
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.library.annotation.MM
import com.angcyo.library.annotation.Pixel
import com.angcyo.library.annotation.Private
import com.angcyo.library.unit.toPixel
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmHoldDataNull
import kotlin.math.max
import kotlin.math.min

/**
 * 预览数据存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
class PreviewModel : LifecycleViewModel() {

    companion object {

        /**创建一个预览信息*/
        fun createPreviewInfo(list: List<TransferDataEntity>?): PreviewInfo? {
            if (list.isNullOrEmpty()) return null

            val result = PreviewInfo()

            defaultPreviewInfo(result)

            @MM
            var left: Float? = null
            var top: Float? = null
            var right: Float? = null
            var bottom: Float? = null

            list.forEach {
                val originX = it.originX ?: 0f
                val originY = it.originY ?: 0f
                val width = it.originWidth ?: 0f
                val height = it.originHeight ?: 0f
                val originRight = originX + width
                val originBottom = originY + height

                left = min(originX, left ?: originX)
                top = min(originY, top ?: originY)
                right = max(originRight, right ?: originRight)
                bottom = max(originBottom, bottom ?: originBottom)
            }

            @Pixel
            val rect = RectF(left.toPixel(), top.toPixel(), right.toPixel(), bottom.toPixel())
            result.originBounds = RectF(rect)
            return result
        }

        /**一些默认初始化*/
        fun defaultPreviewInfo(info: PreviewInfo) {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            if (laserPeckerModel.haveExDevice()) {
                info.zState = PreviewInfo.Z_STATE_PAUSE//有外设的情况下, z轴优先暂停滚动
            } else {
                info.zState = null
            }
        }
    }

    /**当前正在预览的信息, 退出预览模式时, 需要清空数据
     * [com.angcyo.engrave.EngravePreviewLayoutHelper.bindDeviceState] 清空数据
     * */
    val previewInfoData = vmHoldDataNull<PreviewInfo?>()

    val engraveModel = vmApp<EngraveModel>()
    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    init {
        //监听设备状态
        deviceStateModel.deviceStateData.observe(this) { queryState ->
            if (queryState?.isModeEngravePreview() == true) {
                //no op
            } else {
                //非预览模式, 清空预览数据, 路径预览时退出后~, 移到关闭界面时, 清除数据
                //previewInfoData.value = null
            }
        }
    }

    /**清理预览信息*/
    @AnyThread
    fun clearPreviewInfo() {
        previewInfoData.updateValue(null)
    }

    /**开始预览*/
    @AnyThread
    fun startPreview(previewInfo: PreviewInfo?, async: Boolean = true) {
        previewInfoData.updateValue(previewInfo)
        previewInfo?.let {
            val originBounds = previewInfo.originBounds
            val zPause = previewInfo.zState
            if (zPause == null) {
                //非第三轴预览模式下
                if (previewInfo.isCenterPreview) {
                    //需要中心点预览
                    if (laserPeckerModel.isC1()) {
                        //C1设备显示中心点
                        _previewShowCenter(originBounds, async)
                    } else {
                        if (HawkEngraveKeys.enableRectCenterPreview) {
                            //矩形中心点预览
                            originBounds?.let {
                                val centerX = it.centerX()
                                val centerY = it.centerY()
                                val bounds = RectF(centerX, centerY, centerX + 1f, centerY + 1f)
                                _previewRangeRect(bounds, async)
                            }
                        } else {
                            //设备中心点
                            _previewShowCenter(originBounds, async)
                        }
                    }
                } else {
                    //需要范围预览
                    _previewRangeRect(originBounds, async)
                }
            } else {
                //第三轴预览模式
                if (previewInfo.isStartPreview) {
                    if (previewInfo.updatePwr || zPause == PreviewInfo.Z_STATE_PAUSE) {
                        //2023-3-23 更新pwr时, 强制进入第三轴暂停状态
                        //第三轴需要处于暂停状态
                        _previewRangeRect(
                            originBounds,
                            async,
                            true
                        )
                    } else if (zPause == PreviewInfo.Z_STATE_SCROLL) {
                        //C1 专属第三轴滚动
                        _zScrollPreview(async)
                    } else {
                        //第三轴继续预览
                        _zContinuePreview(async)
                    }
                } else {
                    //没有开始预览则需要新进入预览状态
                    _previewRangeRect(originBounds, async)
                    previewInfo.isStartPreview = true
                    previewInfo.zState = PreviewInfo.Z_STATE_PAUSE
                }
            }
            previewInfo.updatePwr = false
        }
    }

    /**发送预览中心点指令*/
    @Private
    fun _previewShowCenter(bounds: RectF?, async: Boolean) {
        laserPeckerModel.previewShowCenter(bounds, HawkEngraveKeys.lastPwrProgress, async)
    }

    /**发送预览中心点指令
     * [rotate] [bounds]需要旋转的角度, 如果设置了, 则自动开启4点预览
     * */
    @Private
    fun _previewRangeRect(
        bounds: RectF?,
        async: Boolean,
        zPause: Boolean = false,
    ) {
        bounds ?: return
        laserPeckerModel.sendUpdatePreviewRange(
            bounds,
            bounds,
            null,
            HawkEngraveKeys.lastPwrProgress,
            async,
            zPause,
            EngraveHelper.getDiameter()
        )
    }

    /**z轴继续滚动预览*/
    @Private
    fun _zContinuePreview(async: Boolean = true) {
        val cmd = EngravePreviewCmd.previewZContinueCmd(HawkEngraveKeys.lastPwrProgress)
        val flag =
            if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
        cmd.enqueue(flag)
    }

    /**C1专属 z轴滚动预览*/
    @Private
    fun _zScrollPreview(async: Boolean = true) {
        val cmd = EngravePreviewCmd.previewZScrollCmd(HawkEngraveKeys.lastPwrProgress)
        val flag =
            if (async) CommandQueueHelper.FLAG_ASYNC else CommandQueueHelper.FLAG_NORMAL
        cmd.enqueue(flag)
    }

    /**刷新预览, 根据当前的状态, 择优发送指令*/
    @AnyThread
    fun refreshPreview(async: Boolean = true, action: PreviewInfo.() -> Unit = {}) {
        previewInfoData.value?.let {
            it.action()
            startPreview(it, async)
        }
    }

    /**更新预览的操作, 并且重新发送预览指定
     * [async] 是否需要异步发送指令
     * [sendCmd] 是否需要发送指令, 否则只更新信息
     * [restore] 是否需要恢复状态
     * */
    @AnyThread
    fun updatePreview(
        async: Boolean = true,
        sendCmd: Boolean = true,
        restore: Boolean = false,
        action: PreviewInfo.() -> Unit
    ) {
        var previewInfo = previewInfoData.value
        if (previewInfo == null && sendCmd && restore) {
            previewInfo = previewInfoData.beforeNonValue
        }
        previewInfo?.let {
            defaultPreviewInfo(it)
            it.action()
            if (sendCmd) {
                startPreview(it, async)
            } else {
                previewInfoData.updateValue(it)
            }
        }
    }
}