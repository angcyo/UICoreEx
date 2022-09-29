package com.angcyo.engrave.transition

import androidx.annotation.WorkerThread
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE
import com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING
import com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE
import com.angcyo.engrave.R
import com.angcyo.engrave.data.*
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex._string
import com.angcyo.library.ex.size
import kotlin.math.max
import kotlin.math.min

/**
 * 雕刻数据相关处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class EngraveTransitionManager {

    companion object {
        /**生成一个雕刻需要用到的文件索引*/
        fun generateEngraveIndex(): Int {
            return (System.currentTimeMillis() / 1000).toInt()
        }

        /**图层, 以及图层数据*/
        val engraveLayerList = listOf(
            EngraveLayerInfo(DATA_MODE_DITHERING, _string(R.string.engrave_layer_bitmap)),
            EngraveLayerInfo(DATA_MODE_BLACK_WHITE, _string(R.string.engrave_layer_fill)),
            EngraveLayerInfo(DATA_MODE_GCODE, _string(R.string.engrave_layer_line))
        )

        /**根据雕刻图层, 获取对应选中的渲染器
         * [layerInfo] 为空时, 表示所有*/
        fun getRendererList(
            canvasDelegate: CanvasDelegate,
            layerInfo: EngraveLayerInfo?
        ): List<BaseItemRenderer<*>> {
            val rendererList = mutableListOf<BaseItemRenderer<*>>()
            val selectList = canvasDelegate.getSelectedRendererList()
            if (selectList.isEmpty()) {
                rendererList.addAll(canvasDelegate.itemsRendererList)
            } else {
                rendererList.addAll(selectList)
            }
            return rendererList.filter { it.isVisible() }.filter {
                if (it is DataItemRenderer) {
                    if (layerInfo == null) {
                        true
                    } else {
                        it.dataItem?.dataBean?._dataMode == layerInfo.mode
                    }
                } else {
                    false
                }
            }
        }
    }

    /**数据转换器*/
    private val transitionList = mutableListOf<IEngraveTransition>()

    init {
        transitionList.add(GCodeTransition())
        transitionList.add(BitmapTransition())
    }

/*
    */
    /**将[renderer]转换成雕刻预备的数据 *//*
    @CallPoint
    fun transitionReadyData(renderer: BaseItemRenderer<*>?): EngraveReadyInfo? {
        val itemRenderer = renderer ?: return null
        var result: EngraveReadyInfo? = null

        for (transition in transitionList) {
            result = transition.doTransitionReadyData(itemRenderer)
            if (result != null) {
                break
            }
        }

        if (result?.engraveData == null) {
            result?.engraveData = EngraveDataInfo()
        }
        if (result == null) {
            L.w("无法处理的Item:${renderer}")
        } else {
            L.i(
                "预处理Item数据->",
                result.dataType.toDataTypeStr(),
                result.dataMode.toDataModeStr()
            )
        }
        return result
    }
    */
/*
    */
    /**真正的雕刻数据处理*//*
    @CallPoint
    fun transitionEngraveData(
        renderer: BaseItemRenderer<*>?,
        engraveReadyInfo: EngraveReadyInfo
    ) {
        val itemRenderer = renderer ?: return

        for (transition in transitionList) {
            val result = transition.doTransitionEngraveData(itemRenderer, engraveReadyInfo)
            if (result) {
                break
            }
        }

        engraveReadyInfo.engraveData?.let {
            L.i(
                "处理Item数据->",
                engraveReadyInfo.dataType.toDataTypeStr(),
                engraveReadyInfo.dataMode.toDataModeStr(),
                engraveReadyInfo.engraveData?.engraveDataType?.toEngraveTypeStr()
            )
        }
    }*/

    /**相同类型的[TransferDataInfo]会合并在一起*/
    @CallPoint
    @WorkerThread
    fun transitionTransferData(
        canvasDelegate: CanvasDelegate,
        transferDataConfigInfo: TransferDataConfigInfo
    ): List<TransferTaskData> {
        val result = mutableListOf<TransferTaskData>()
        engraveLayerList.forEach { engraveLayerInfo ->
            val dataList = mutableListOf<TransferDataInfo>()
            getRendererList(canvasDelegate, engraveLayerInfo).forEach { renderer ->
                transitionTransferData(renderer, transferDataConfigInfo)?.let { transferDataInfo ->
                    dataList.add(transferDataInfo)
                }
            }
            if (dataList.isNotEmpty()) {
                if (dataList.size() == 1) {
                    //只有1条数据, 不需要合并
                    result.add(TransferTaskData(engraveLayerInfo, dataList))
                } else {
                    //多条数据
                    if (engraveLayerInfo.mode == DATA_MODE_GCODE) {
                        //GCode 数据合并
                        val gcodeTransferDataInfo = _mergeTransferData(dataList)
                        result.add(
                            TransferTaskData(engraveLayerInfo, listOf(gcodeTransferDataInfo))
                        )
                    } else if (engraveLayerInfo.mode == DATA_MODE_BLACK_WHITE) {
                        //线段数据合并
                        val bitmapPathTransferDataInfo = _mergeTransferData(dataList)
                        result.add(
                            TransferTaskData(engraveLayerInfo, listOf(bitmapPathTransferDataInfo))
                        )
                    } else {
                        //抖动数据不需要合并
                        result.add(TransferTaskData(engraveLayerInfo, dataList))
                    }
                }
            }
        }
        return result
    }

    /**将[renderer]转换成传输给机器的数据,
     * 内部有耗时转换操作, 建议在子线程处理*/
    @CallPoint
    @WorkerThread
    fun transitionTransferData(
        renderer: BaseItemRenderer<*>,
        transferDataConfigInfo: TransferDataConfigInfo
    ): TransferDataInfo? {
        var result: TransferDataInfo? = null

        for (transition in transitionList) {
            result = transition.doTransitionTransferData(renderer, transferDataConfigInfo)
            if (result != null) {
                break
            }
        }

        return result
    }

    //

    /**合并数据*/
    fun _mergeTransferData(dataList: List<TransferDataInfo>): TransferDataInfo {
        val resultTransferDataInfo = TransferDataInfo(index = generateEngraveIndex())
        resultTransferDataInfo.data = byteWriter {
            dataList.forEach {
                write(it.data)
            }
        }
        val first = dataList.first()
        resultTransferDataInfo.engraveDataType = first.engraveDataType
        resultTransferDataInfo.px = first.px
        resultTransferDataInfo.name = first.name
        resultTransferDataInfo.x = first.x
        resultTransferDataInfo.y = first.y

        var right = first.x + first.width
        var bottom = first.y + first.height
        var lines = 0
        dataList.forEach {
            resultTransferDataInfo.x = min(resultTransferDataInfo.x, it.x)
            resultTransferDataInfo.y = min(resultTransferDataInfo.y, it.y)
            right = max(right, it.x + it.width)
            bottom = max(bottom, it.y + it.height)
            lines += it.lines
        }

        resultTransferDataInfo.lines = lines
        resultTransferDataInfo.width = right - resultTransferDataInfo.x
        resultTransferDataInfo.height = bottom - resultTransferDataInfo.y

        return resultTransferDataInfo
    }

}