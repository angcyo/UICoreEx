package com.angcyo.engrave

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.dslitem.EngraveDividerItem
import com.angcyo.engrave.dslitem.engrave.EngraveDataNameItem
import com.angcyo.engrave.dslitem.engrave.EngraveDataPxItem
import com.angcyo.item.DslBlackButtonItem
import com.angcyo.library.ex._string

/**
 * 雕刻item布局渲染
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/27
 */
abstract class BaseEngraveLayoutHelper : BaseEngravePreviewLayoutHelper() {

    override fun renderFlowItems() {
        when (engraveFlow) {
            ENGRAVE_FLOW_TRANSFER_BEFORE_CONFIG -> renderTransferConfig()
            ENGRAVE_FLOW_TRANSMITTING -> renderTransmitting()
            else -> super.renderFlowItems()
        }
    }

    //

    /**渲染传输数据配置界面*/
    fun renderTransferConfig() {
        updateIViewTitle(_string(R.string.print_setting))
        engraveBackFlow = ENGRAVE_FLOW_PREVIEW
        showCloseView(true, _string(R.string.ui_back))

        renderDslAdapter {
            EngraveDataNameItem()() {
                //itemEngraveReadyInfo = engraveReadyInfo
            }
            EngraveDataPxItem()() {
                //itemEngraveDataInfo = dataInfo
                itemPxList = LaserPeckerHelper.findProductSupportPxList()
            }
            EngraveDividerItem()()
            DslBlackButtonItem()() {
                itemButtonText = _string(R.string.ui_next)
                itemClick = {
                    //下一步, 数据传输界面
                    engraveFlow = ENGRAVE_FLOW_TRANSMITTING
                    renderFlowItems()
                }
            }
        }
    }

    /**渲染传输中的界面*/
    fun renderTransmitting() {
        updateIViewTitle(_string(R.string.transmitting))
        showCloseView(false)
    }

    //

}