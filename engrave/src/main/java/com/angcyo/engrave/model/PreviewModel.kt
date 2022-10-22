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
import com.angcyo.http.rx.doMain
import com.angcyo.library.annotation.Private
import com.angcyo.viewmodel.vmDataNull

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

        /**一些默认初始化*/
        fun defaultPreviewInfo(info: PreviewInfo) {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            //是否要使用4点预览
            val openPointsPreview = HawkEngraveKeys.USE_FOUR_POINTS_PREVIEW //开启了4点预览
                    && !laserPeckerModel.haveExDevice() //没有外置设备连接
            info.isFourPointPreview = openPointsPreview

            if (laserPeckerModel.haveExDevice()) {
                info.isZPause = true//有外设的情况下, z轴优先暂停滚动
            } else {
                info.isZPause = null
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
                    val bounds = if (laserPeckerModel.haveExDevice()) {
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
    val previewInfoData = vmDataNull<PreviewInfo?>()

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
        doMain {
            previewInfoData.setValue(previewInfo)
        }
        previewInfo?.let {
            val originBounds = previewInfo.originBounds
            val zPause = previewInfo.isZPause
            if (zPause == null) {
                //非第三轴预览模式下
                if (previewInfo.isCenterPreview) {
                    //需要中心点预览
                    if (laserPeckerModel.productInfoData.value?.isCI() == true) {
                        //C1设备显示中心点
                        _previewShowCenter(originBounds, async)
                    } else {
                        if (HawkEngraveKeys.enableRectCenterPreview) {
                            //矩形中心点预览
                            originBounds?.let {
                                val centerX = originBounds.centerX()
                                val centerY = originBounds.centerY()
                                val bounds = RectF(centerX, centerY, centerX + 1f, centerY + 1f)
                                if (previewInfo.isFourPointPreview) {
                                    _previewRangeRect(bounds, bounds, 0f, async)
                                } else {
                                    _previewRangeRect(bounds, null, null, async)
                                }
                            }
                        } else {
                            //设备中心点
                            _previewShowCenter(originBounds, async)
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
                    if (zPause) {
                        //第三轴需要处于暂停状态
                        _previewRangeRect(
                            originBounds,
                            previewInfo.rotateBounds,
                            null,
                            async,
                            true
                        )
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
                    previewInfo.isZPause = true
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

    /**z轴滚动预览*/
    @Private
    fun _zContinuePreview(async: Boolean = true) {
        val cmd = EngravePreviewCmd.previewZContinueCmd()
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
    fun updatePreview(async: Boolean = true, action: PreviewInfo.() -> Unit) {
        previewInfoData.value?.let {
            defaultPreviewInfo(it)
            it.action()
            startPreview(it, async)
        }
    }

    /**使用[itemRenderer]更新预览操作
     * [updatePreview]*/
    @AnyThread
    fun updatePreview(itemRenderer: IRenderer?, async: Boolean = true) {
        updatePreview(async) {
            updatePreviewInfo(this, itemRenderer)
        }
    }
}