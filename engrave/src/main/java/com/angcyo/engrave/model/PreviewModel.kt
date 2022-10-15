package com.angcyo.engrave.model

import android.graphics.RectF
import androidx.annotation.AnyThread
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.lifecycle.LifecycleViewModel
import com.angcyo.core.vmApp
import com.angcyo.engrave.EngraveHelper
import com.angcyo.engrave.data.HawkEngraveKeys
import com.angcyo.engrave.data.PreviewInfo
import com.angcyo.viewmodel.vmDataNull

/**
 * 预览数据存储
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
class PreviewModel : LifecycleViewModel() {

    companion object {

        /**创建一个预览信息*/
        fun createPreviewInfo(canvasDelegate: CanvasDelegate): PreviewInfo? {
            val laserPeckerModel = vmApp<LaserPeckerModel>()
            val selectedRenderer = canvasDelegate.getSelectedRenderer()

            //是否要使用4点预览
            val openPointsPreview = HawkEngraveKeys.USE_FOUR_POINTS_PREVIEW //开启了4点预览
                    && !laserPeckerModel.haveExDevice() //没有外置设备连接

            if (selectedRenderer == null) {
                //没有选中的元素, 则考虑预览设备的最佳尺寸范围
                laserPeckerModel.productInfoData.value?.previewBounds?.let {
                    return PreviewInfo(RectF(it), RectF(it))
                }
            } else {
                val itemUuid = selectedRenderer.getRendererRenderItem()?.uuid

                //先执行, 设置预览的信息
                val info = PreviewInfo(
                    selectedRenderer.getBounds(),
                    selectedRenderer.getRotateBounds(),
                    if (openPointsPreview) {
                        selectedRenderer.rotate
                    } else {
                        null
                    },
                    itemUuid
                )
                return info
            }
            return null
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
                previewInfoData.postValue(null)
            }
        }
    }

    /**刷新预览, 根据当前的状态, 择优发送指令
     * 在改变激光强度之后调用*/
    @AnyThread
    fun refreshPreview(async: Boolean, zPause: Boolean): Boolean {
        if (HawkEngraveKeys.isRectCenterPreview) {
            //矩形中心点预览
            rectCenterPreview(async, zPause)
            return true
        } else if (laserPeckerModel.isEngravePreviewShowCenterMode()) {
            previewShowCenter(async)
            return true
        } else if (laserPeckerModel.isEngravePreviewMode()) {
            refreshPreviewBounds(async, zPause)
            return true
        }
        return false
    }

    /**开始或者刷新预览, 并且发送通知
     * 保持当前的预览模式不变, 更新预览的bounds等信息
     * 保持: 中心点预览 or 范围预览
     * */
    @AnyThread
    fun startOrRefreshPreview(previewInfo: PreviewInfo?, async: Boolean, zPause: Boolean) {
        previewInfoData.postValue(previewInfo)
        if (HawkEngraveKeys.isRectCenterPreview) {
            //矩形中心点预览
            rectCenterPreview(async, zPause)
        } else if (laserPeckerModel.isEngravePreviewMode() || !refreshPreview(async, zPause)) {
            //开始预览
            previewBounds(previewInfo, async, zPause)
        }
    }

    /**刷新预览范围*/
    @AnyThread
    fun refreshPreviewBounds(async: Boolean, zPause: Boolean) {
        previewInfoData.value?.let {
            previewBounds(it, async, zPause)
        }
    }

    /**矩形中心点预览*/
    @AnyThread
    fun rectCenterPreview(async: Boolean, zPause: Boolean) {
        previewInfoData.value?.let {
            val centerX = it.originBounds.centerX()
            val centerY = it.originBounds.centerY()
            val bounds = RectF(centerX, centerY, centerX + 1f, centerY + 1f)
            val diameter = EngraveHelper.getDiameter()
            laserPeckerModel.sendUpdatePreviewRange(
                bounds,
                bounds,
                null,
                HawkEngraveKeys.lastPwrProgress,
                async,
                zPause,
                diameter
            )
        }
    }

    /**中心点预览指令*/
    @AnyThread
    fun previewShowCenter(async: Boolean) {
        previewInfoData.value?.let {
            laserPeckerModel.previewShowCenter(
                it.originBounds,
                HawkEngraveKeys.lastPwrProgress,
                async
            )
        }
    }

    /**更新机器的预览范围
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.sendUpdatePreviewRange]
     * [zPause] 是否需要第三轴暂停预览
     * [async] 是否是异步指令
     *
     * 发送指令之后, 可能需要重新发送查询指令
     * [com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel.queryDeviceState]
     * */
    @AnyThread
    fun previewBounds(info: PreviewInfo?, async: Boolean, zPause: Boolean) {
        previewInfoData.postValue(info)

        //发送指令
        if (info == null) {
            //是否需要停止预览?
        } else {
            val diameter = EngraveHelper.getDiameter()
            laserPeckerModel.sendUpdatePreviewRange(
                info.originBounds,
                info.rotateBounds,
                info.rotate,
                HawkEngraveKeys.lastPwrProgress,
                async,
                zPause,
                diameter
            )
        }

        //后执行, 通知预览
        //engraveModel.updateEngravePreviewUuid(info?.itemUuid)
    }
}