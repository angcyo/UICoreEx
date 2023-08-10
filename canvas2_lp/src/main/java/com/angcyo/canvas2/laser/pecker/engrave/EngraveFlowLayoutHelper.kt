package com.angcyo.canvas2.laser.pecker.engrave

import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.library.ex._string
import com.angcyo.library.toastQQ

/**
 * 雕刻布局相关操作
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/30
 */
open class EngraveFlowLayoutHelper : BaseEngraveLayoutHelper() {

    override fun renderPreviewBeforeItems() {
        super.renderPreviewBeforeItems()
    }

    override fun renderPreviewItems() {
        super.renderPreviewItems()
    }

    override fun renderTransferConfig() {
        if (_isSingleItemFlow) {
            //单元素参数使用自己的传输配置
            if (checkCanNext()) {
                //下一步, 数据传输界面

                //退出打印模式, 进入空闲模式
                asyncTimeoutExitCmd { bean, error ->
                    if (error == null) {
                        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
                        engraveFlow = ENGRAVE_FLOW_TRANSMITTING
                        val flowId = generateFlowId("准备发送文件")

                        engraveConfigProvider.onSaveTransferConfig(
                            this@EngraveFlowLayoutHelper,
                            engraveConfigProvider.getTransferConfig(this)
                        )
                        onStartEngraveTransferData(flowId)
                        val delegate = engraveCanvasFragment?.renderDelegate
                        LPTransferHelper.startCreateTransferData(transferModel, flowId, delegate)

                        //last
                        renderFlowItems()
                    } else {
                        toastQQ(error.message)
                    }
                }
            }
        } else {
            super.renderTransferConfig()
        }
    }

    override fun changeToTransferConfig() {
        if (_isSingleItemFlow) {
            val noConfigEngraveParamsRenderer =
                LPEngraveHelper.getNoConfigEngraveParamsRenderer(engraveCanvasFragment?.renderDelegate)
            if (noConfigEngraveParamsRenderer != null) {
                //有元素没有配置参数
                val label = noConfigEngraveParamsRenderer.lpElementBean()?.name ?: "Element"
                toastQQ(_string(R.string.no_config_params_tip, label))
            } else {
                super.changeToTransferConfig()
            }
        } else {
            super.changeToTransferConfig()
        }
    }

    override fun renderEngraveItemParamsConfig() {
        super.renderEngraveItemParamsConfig()
    }

    override fun renderTransmitting() {
        super.renderTransmitting()
    }

    override fun renderEngraveConfig() {
        super.renderEngraveConfig()
    }

    override fun renderEngraving() {
        super.renderEngraving()
    }

    override fun renderEngraveFinish() {
        super.renderEngraveFinish()
    }

}