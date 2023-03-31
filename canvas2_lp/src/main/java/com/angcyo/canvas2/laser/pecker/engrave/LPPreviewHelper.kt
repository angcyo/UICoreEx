package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.RectF
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas.render.renderer.CanvasGroupRenderer
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.vmApp
import com.angcyo.engrave2.data.PreviewInfo
import com.angcyo.engrave2.model.PreviewModel
import com.angcyo.library.ex.size

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

    /**渲染初始化*/
    private fun updatePreviewInfo(info: PreviewInfo, rendererList: List<BaseRenderer>?) {
        val laserPeckerModel = vmApp<LaserPeckerModel>()
        info.apply {
            data = null
            if (rendererList.size() == 1) {
                data = rendererList?.firstOrNull()?.lpElementBean()
            }
            if (rendererList.isNullOrEmpty()) {
                //未选中元素的情况下预览
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
                }
            } else {
                //选中元素的情况下预览
                originBounds = CanvasGroupRenderer.getRendererListRenderProperty(rendererList)
                    .getRenderBounds(RectF())
            }
        }
    }
}