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
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex._string
import com.angcyo.library.ex.size
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.entity.toTransferData
import com.angcyo.objectbox.saveAllEntity
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random.Default.nextInt

/**
 * 雕刻数据相关处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class EngraveTransitionManager {

    companion object {

        /**生成一个雕刻需要用到的文件索引
         * 4个字节 最大 4_294_967_295
         * */
        fun generateEngraveIndex(): Int {
            val millis = System.currentTimeMillis() //13位毫秒
            val s = millis / 1000 //10位秒
            val m = millis % 1000 //毫秒
            val r = nextInt(0, m.toInt()) //随机数
            return (s + m + r).toInt()
        }

        /**生成一个雕刻的文件名*/
        fun generateEngraveName(): String {
            return "filename-${HawkEngraveKeys.lastEngraveCount + 1}"
        }

        /**图层, 以及图层数据*/
        val engraveLayerList = listOf(
            EngraveLayerInfo(DATA_MODE_DITHERING, _string(R.string.engrave_layer_bitmap)),
            EngraveLayerInfo(DATA_MODE_BLACK_WHITE, _string(R.string.engrave_layer_fill)),
            EngraveLayerInfo(DATA_MODE_GCODE, _string(R.string.engrave_layer_line))
        )

        /**获取图层*/
        fun getEngraveLayer(mode: Int?) = engraveLayerList.find { it.mode == mode }

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

    /**相同类型的[TransferDataInfo]会合并在一起, 会根据列表顺序, 生成对应顺序的数据
     * [com.angcyo.engrave.transition.EngraveTransitionManager.engraveLayerList]*/
    @CallPoint
    @WorkerThread
    fun transitionTransferData(
        canvasDelegate: CanvasDelegate,
        transferConfigEntity: TransferConfigEntity
    ): List<TransferDataEntity> {
        val resultDataList = mutableListOf<TransferDataEntity>()
        engraveLayerList.forEach { engraveLayerInfo ->
            val dataList = mutableListOf<TransferDataEntity>()
            getRendererList(canvasDelegate, engraveLayerInfo).forEach { renderer ->
                //开始将[renderer]转换成数据
                transitionTransferData(renderer, transferConfigEntity)?.let { transferDataEntity ->
                    transferDataEntity.layerMode = engraveLayerInfo.mode
                    dataList.add(transferDataEntity)
                }
            }
            if (dataList.isNotEmpty()) {
                if (!transferConfigEntity.mergeData || dataList.size() == 1) {
                    //只有1条数据, 不需要合并
                    resultDataList.addAll(dataList)
                } else {
                    //多条数据
                    when (engraveLayerInfo.mode) {
                        DATA_MODE_GCODE -> {
                            //GCode 数据合并
                            val gcodeTransferDataInfo = _mergeTransferData(dataList)
                            resultDataList.add(gcodeTransferDataInfo)
                        }
                        DATA_MODE_BLACK_WHITE -> {
                            //线段数据合并
                            val bitmapPathTransferDataInfo = _mergeTransferData(dataList)
                            resultDataList.add(bitmapPathTransferDataInfo)
                        }
                        else -> {
                            //抖动数据不需要合并
                            resultDataList.addAll(dataList)
                        }
                    }
                }
            }
        }
        resultDataList.saveAllEntity(LPBox.PACKAGE_NAME)//入库
        return resultDataList
    }

    /**将[renderer]转换成传输给机器的数据,
     * 内部有耗时转换操作, 建议在子线程处理*/
    @CallPoint
    @WorkerThread
    fun transitionTransferData(
        renderer: BaseItemRenderer<*>,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        var result: TransferDataEntity? = null

        for (transition in transitionList) {
            result = transition.doTransitionTransferData(renderer, transferConfigEntity)
            if (result != null) {
                break
            }
        }

        return result
    }

    //

    /**合并数据*/
    fun _mergeTransferData(dataList: List<TransferDataEntity>): TransferDataEntity {
        val resultTransferDataInfo = TransferDataEntity(index = generateEngraveIndex())

        resultTransferDataInfo.data = byteWriter {
            dataList.forEach {
                write(it.bytes())
            }
        }.toTransferData()
        val first = dataList.first()

        resultTransferDataInfo.taskId = first.taskId
        resultTransferDataInfo.layerMode = first.layerMode

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