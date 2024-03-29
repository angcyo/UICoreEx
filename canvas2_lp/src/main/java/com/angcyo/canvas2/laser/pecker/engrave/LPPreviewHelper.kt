package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.RectF
import androidx.annotation.AnyThread
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.bean._multiElementRange
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.vmApp
import com.angcyo.engrave2.data.PreviewInfo
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.library.component._debounce
import com.angcyo.library.ex.size
import com.angcyo.library.unit.toMm

/**
 * 业务相关的预览助手工具类
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
object LPPreviewHelper {

    /**创建一个预览信息*/
    fun createPreviewInfo(canvasDelegate: CanvasRenderDelegate?): PreviewInfo? {
        canvasDelegate ?: return null
        val rendererList = canvasDelegate.getSelectorOrAllElementRendererList(true, false)

        val result = PreviewInfo()
        //result.rotate = selectedRenderer?.rotate //2023-3-29 不需要

        PreviewModel.defaultPreviewInfo(result)
        updatePreviewInfo(result, rendererList)
        return result
    }

    /**使用[itemRenderer], 更新预览信息*/
    fun updatePreviewByRenderer(
        renderFragment: IEngraveRenderFragment,
        rendererList: List<BaseRenderer>?,
    ) {
        val flowLayoutHelper = renderFragment.flowLayoutHelper
        if (!flowLayoutHelper.isInitialize) {
            return
        }
        if (rendererList.isNullOrEmpty()) {
            return
        }
        if (renderFragment.flowLayoutHelper.isMinimumPreview) {
            //最小化了
        } else if (!flowLayoutHelper.isAttach()) {
            //未附着
        }
        //flowLayoutHelper.startPreview(renderFragment)
        val laserPeckerModel = flowLayoutHelper.laserPeckerModel
        val enablePreviewDebounce =
            laserPeckerModel.productInfoData.value?.deviceConfigBean?.enablePreviewDebounce == true
        //是否要抖动发送预览指令
        val debounce = HawkEngraveKeys.enablePreviewDebounce && enablePreviewDebounce
        if (debounce || laserPeckerModel.haveExDevice()) { //有外设的情况下, 需要抖动发送预览指令
            updatePreview(rendererList, sendCmd = false)
            _debounce {
                updatePreview(rendererList)
            }
        } else {
            updatePreview(rendererList)
        }
    }

    /**渲染初始化*/
    private fun updatePreviewInfo(info: PreviewInfo, rendererList: List<BaseRenderer>?) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        info.apply {
            rendererUuid = null
            if (rendererList.size() == 1) {
                val renderer = rendererList?.firstOrNull()
                if (renderer?.lpElementBean()?._layerMode == LPDataConstant.DATA_MODE_GCODE) {
                    rendererUuid = renderer.uuid
                }
            }
            if (rendererList.isNullOrEmpty()) {
                //未选中元素的情况下预览
                val productInfo = laserPeckerModel.productInfoData.value
                val bounds = if (laserPeckerModel.isCarConnect()) {
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
                }
                elementBoundsList = null
            } else {
                //选中元素的情况下预览
                val boundsList = mutableListOf<RectF>()
                originBounds = CanvasGroupRenderer.getRendererListRenderProperty(
                    rendererList,
                    resultBoundsList = boundsList
                ).getRenderBounds(RectF())
                if (_multiElementRange) {
                    info.boundsList = boundsList
                }
                elementBoundsList = boundsList
            }

            //约束最小宽高
            originBounds?.let {
                if (it.width() <= 0) {
                    it.right = it.left + 1
                }
                if (it.height() <= 0) {
                    it.bottom = it.top + 1
                }

                //2023-7-19
                HawkEngraveKeys.lastPreviewWidth = it.width().toMm()
                HawkEngraveKeys.lastPreviewHeight = it.height().toMm()
            }
        }
    }

    /**使用[rendererList]更新预览操作
     * [updatePreview]*/
    @AnyThread
    private fun updatePreview(
        rendererList: List<BaseRenderer>?,
        async: Boolean = true,
        sendCmd: Boolean = true
    ) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        val previewModel = vmApp<PreviewModel>()
        previewModel.updatePreview(async, sendCmd) {
            updatePreviewInfo(this, rendererList)

            //有外设的情况下, z轴优先暂停滚动
            zState = if (laserPeckerModel.haveExDevice()) {
                PreviewInfo.Z_STATE_PAUSE
            } else {
                zState // def
            }
        }
    }
}