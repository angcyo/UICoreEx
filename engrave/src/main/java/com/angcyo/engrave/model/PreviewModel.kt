package com.angcyo.engrave.model

import android.graphics.RectF
import androidx.annotation.AnyThread
import com.angcyo.bluetooth.fsc.CommandQueueHelper
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.EngravePreviewCmd
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.core.IRenderer
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.data.PreviewInfo
import com.angcyo.library.annotation.Private
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.viewmodel.updateValue
import com.angcyo.viewmodel.vmHoldDataNull

/**
 * 预览数据存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
class PreviewModel : LifecycleViewModel() {

    companion object {

        /**获取设备当前正在预览的信息, 如果不在预览模式, 则返回null*/
        /*fun getDevicePreviewInfo(): PreviewInfo? {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            val stateParser = laserPeckerModel.deviceStateData.value
            if (stateParser?.isModeEngravePreview() == true) {
                //设备在预览模式

                val previewInfo = PreviewInfo()

                //第三轴的flag状态
                val haveExDevice = laserPeckerModel.haveExDevice()
                when (stateParser.workState) {
                    //矢量预览
                    0x01 -> Unit
                    //范围预览
                    0x02 -> {

                    }
                }
            }
            return null
        }*/

        /**创建一个预览信息*/
        fun createPreviewInfo(canvasDelegate: CanvasDelegate?): PreviewInfo? {
            canvasDelegate ?: return null
            val selectedRenderer = canvasDelegate.getSelectedRenderer()

            val result = PreviewInfo()
            result.rotate = selectedRenderer?.rotate

            defaultPreviewInfo(result)
            updatePreviewInfo(result, selectedRenderer)
            return result
        }

        /**创建一个预览信息*/
        fun createPreviewInfo(transferDataEntity: TransferDataEntity?): PreviewInfo? {
            transferDataEntity ?: return null

            val result = PreviewInfo()

            defaultPreviewInfo(result)

            val rect = RectF(
                transferDataEntity.x.toFloat(),
                transferDataEntity.y.toFloat(),
                (transferDataEntity.x + transferDataEntity.width).toFloat(),
                (transferDataEntity.y + transferDataEntity.height).toFloat()
            )
            result.originBounds = RectF(rect)
            result.rotateBounds = RectF(rect)
            return result
        }

        /**一些默认初始化*/
        fun defaultPreviewInfo(info: PreviewInfo) {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            //是否要使用4点预览
            val openPointsPreview = HawkEngraveKeys.USE_FOUR_POINTS_PREVIEW //开启了4点预览
                    && !laserPeckerModel.haveExDevice() //没有外置设备连接
            info.isFourPointPreview = openPointsPreview

            if (laserPeckerModel.haveExDevice()) {
                info.zState = PreviewInfo.Z_STATE_PAUSE//有外设的情况下, z轴优先暂停滚动
            } else {
                info.zState = null
            }
        }

        /**渲染初始化*/
        fun updatePreviewInfo(info: PreviewInfo, itemRenderer: IRenderer?) {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            info.apply {
                itemDataBean = null
                if (itemRenderer is DataItemRenderer) {
                    itemDataBean = itemRenderer.rendererItem?.dataBean
                }
                if (itemRenderer == null) {
                    rotate = null
                    val productInfo = laserPeckerModel.productInfoData.value
                    val bounds = if (laserPeckerModel.isCarOpen()) {
                        //小车模式下
                        productInfo?.carPreviewBounds
                    } else if (laserPeckerModel.haveExDevice()) {
                        //有外设的情况下, 使用物理范围
                        //productInfo?.bounds
                        productInfo?.previewBounds
                    } else {
                        productInfo?.previewBounds
                    }
                    bounds?.let {
                        originBounds = RectF(it)
                        rotateBounds = RectF(it)
                    }
                } else if (itemRenderer is BaseItemRenderer<*>) {
                    rotate = itemRenderer.rotate
                    originBounds = RectF(itemRenderer.getBounds())
                    rotateBounds = RectF(itemRenderer.getRotateBounds())
                }
            }
        }
    }

    /**当前正在预览的信息, 退出预览模式时, 需要清空数据
     * [com.angcyo.engrave.EngravePreviewLayoutHelper.bindDeviceState] 清空数据
     * */
    val previewInfoData = vmHoldDataNull<PreviewInfo?>()

    val engraveModel = vmApp<EngraveModel>()
    val laserPeckerModel = vmApp<LaserPeckerModel>()

    init {
        //监听设备状态
        laserPeckerModel.deviceStateData.observe(this) { queryState ->
            if (queryState?.isModeEngravePreview() == true) {
                //no op
            } else {
                //非预览模式, 清空预览数据
                previewInfoData.value = null
            }
        }
    }

    /**开始预览*/
    @AnyThread
    fun startPreview(previewInfo: PreviewInfo?, async: Boolean = true) {
        previewInfoData.updateValue(previewInfo)
        previewInfo?.let {
            val originBounds = previewInfo.originBounds
            val rotateBounds = previewInfo.rotateBounds //旋转后的矩形
            val zPause = previewInfo.zState
            if (zPause == null) {
                //非第三轴预览模式下
                if (previewInfo.isCenterPreview) {
                    //需要中心点预览
                    if (laserPeckerModel.isC1()) {
                        //C1设备显示中心点
                        _previewShowCenter(rotateBounds, async)
                    } else {
                        if (HawkEngraveKeys.enableRectCenterPreview) {
                            //矩形中心点预览
                            rotateBounds?.let {
                                val centerX = it.centerX()
                                val centerY = it.centerY()
                                val bounds = RectF(centerX, centerY, centerX + 1f, centerY + 1f)
                                if (previewInfo.isFourPointPreview) {
                                    _previewRangeRect(bounds, bounds, 0f, async)
                                } else {
                                    _previewRangeRect(bounds, null, null, async)
                                }
                            }
                        } else {
                            //设备中心点
                            _previewShowCenter(rotateBounds, async)
                        }
                    }
                } else {
                    //需要范围预览
                    if (previewInfo.isFourPointPreview && previewInfo.rotate != null) {
                        //4点预览
                        _previewRangeRect(
                            originBounds,
                            previewInfo.rotateBounds,
                            previewInfo.rotate,
                            async
                        )
                    } else {
                        _previewRangeRect(
                            originBounds,
                            previewInfo.rotateBounds,
                            null,//关闭4点预览
                            async
                        )
                    }
                }
            } else {
                //第三轴预览模式
                if (previewInfo.isStartPreview) {
                    if (zPause == PreviewInfo.Z_STATE_PAUSE) {
                        //第三轴需要处于暂停状态
                        _previewRangeRect(
                            originBounds,
                            previewInfo.rotateBounds,
                            null,
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
                    _previewRangeRect(
                        originBounds,
                        previewInfo.rotateBounds,
                        null,//关闭4点预览
                        async
                    )
                    previewInfo.isStartPreview = true
                    previewInfo.zState = PreviewInfo.Z_STATE_PAUSE
                }
            }
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
        rotateBounds: RectF?,
        rotate: Float?,
        async: Boolean,
        zPause: Boolean = false,
    ) {
        bounds ?: return
        laserPeckerModel.sendUpdatePreviewRange(
            bounds,
            rotateBounds ?: bounds,
            rotate,
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
    fun refreshPreview(async: Boolean = true) {
        previewInfoData.value?.let {
            startPreview(it, async)
        }
    }

    /**更新预览的操作, 并且重新发送预览指定*/
    @AnyThread
    fun updatePreview(
        async: Boolean = true,
        sendCmd: Boolean = true,
        action: PreviewInfo.() -> Unit
    ) {
        var previewInfo = previewInfoData.value
        if (previewInfo == null && sendCmd) {
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

    /**使用[itemRenderer]更新预览操作
     * [updatePreview]*/
    @AnyThread
    fun updatePreview(itemRenderer: IRenderer?, async: Boolean = true, sendCmd: Boolean = true) {
        updatePreview(async, sendCmd) {
            updatePreviewInfo(this, itemRenderer)

            //有外设的情况下, z轴优先暂停滚动
            zState = if (laserPeckerModel.haveExDevice()) {
                PreviewInfo.Z_STATE_PAUSE
            } else {
                zState // def
            }
        }
    }
}