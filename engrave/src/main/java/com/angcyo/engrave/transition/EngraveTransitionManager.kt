package com.angcyo.engrave.transition

import android.graphics.RectF
import androidx.annotation.WorkerThread
import com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.canvas.data.CanvasProjectItemBean
import com.angcyo.canvas.graphics.GraphicsHelper
import com.angcyo.canvas.graphics.IEngraveProvider
import com.angcyo.canvas.items.data.DataItemRenderer
import com.angcyo.canvas.items.renderer.BaseItemRenderer
import com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_BLACK_WHITE
import com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_DITHERING
import com.angcyo.canvas.utils.CanvasConstant.DATA_MODE_GCODE
import com.angcyo.canvas.utils.sort
import com.angcyo.engrave.R
import com.angcyo.engrave.data.*
import com.angcyo.engrave.toEngraveDataTypeStr
import com.angcyo.engrave.toEngraveTypeOfDataMode
import com.angcyo.library.L
import com.angcyo.library.LTime
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex._string
import com.angcyo.library.ex.file
import com.angcyo.library.ex.size
import com.angcyo.library.utils.filePath
import com.angcyo.library.utils.writeToFile
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.objectbox.saveAllEntity
import kotlin.math.max
import kotlin.math.min

/**
 * 雕刻数据相关处理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/22
 */
class EngraveTransitionManager {

    companion object {

        /**雕刻传输数据缓存文件的文件夹*/
        const val ENGRAVE_TRANSFER_FILE_FOLDER = "transfer"

        fun String.toTransferData() = toByteArray(Charsets.ISO_8859_1)

        fun ByteArray.toTransferData() = toString(Charsets.ISO_8859_1)

        /**将字节数据写入到文件*/
        fun ByteArray.writeTransferDataPath(fileName: String) =
            writeToFile(filePath(ENGRAVE_TRANSFER_FILE_FOLDER, fileName).file())

        /**生成一个雕刻需要用到的文件索引
         * 4个字节 最大 4_294_967_295
         * */
        fun generateEngraveIndex(): Int {
            val millis = System.currentTimeMillis() //13位毫秒
            /*val s = millis / 1000 //10位秒
            val m = millis % 1000 //毫秒
            val r = nextInt(0, m.toInt()) //随机数
            return (s + m + r).toInt()*/
            return (millis and 0xfff_ffff).toInt()
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

        /**获取一个转换需要的额外参数, 在需要合并数据时需要
         * [rendererList] 需要是一个图层下的所有渲染器, 不能混合
         * */
        fun getTransitionParam(
            rendererList: List<BaseItemRenderer<*>>,
            transferConfigEntity: TransferConfigEntity
        ): TransitionParam {
            var dataEngraveType = DataCmd.ENGRAVE_TYPE_BITMAP
            val lastIndex = rendererList.lastIndex

            var bpMinLeft: Float? = null
            var bpMinTop: Float? = null

            var gCodeStartRenderer: BaseItemRenderer<*>? = null
            var gCodeEndRenderer: BaseItemRenderer<*>? = null

            rendererList.forEachIndexed { index, baseItemRenderer ->
                dataEngraveType =
                    IEngraveTransition.getDataMode(baseItemRenderer, transferConfigEntity)
                        .toEngraveTypeOfDataMode()
                if (dataEngraveType == DataCmd.ENGRAVE_TYPE_BITMAP_PATH) {
                    //线段数据, 需要计算出数据左上角的坐标
                    val rotateBounds = baseItemRenderer.getRotateBounds()
                    bpMinLeft = min(bpMinLeft ?: rotateBounds.left, rotateBounds.left)
                    bpMinTop = min(bpMinTop ?: rotateBounds.top, rotateBounds.top)
                } else if (dataEngraveType == DataCmd.ENGRAVE_TYPE_GCODE) {
                    //GCode数据需要计算出首尾是哪个渲染器
                    if (index == 0) {
                        gCodeStartRenderer = baseItemRenderer
                    }
                    if (index == lastIndex) {
                        gCodeEndRenderer = baseItemRenderer
                    }
                }
            }
            val param = TransitionParam(
                RectF(bpMinLeft ?: 0f, bpMinTop ?: 0f, 0f, 0f),
                gCodeStartRenderer,
                gCodeEndRenderer
            )
            return param
        }

        /**根据雕刻图层, 获取对应选中的渲染器
         * [layerInfo] 为空时, 表示所有
         * [sort] 是否要排序, 排序规则离左上角越近的优先
         * */
        fun getRendererList(
            canvasDelegate: CanvasDelegate,
            layerInfo: EngraveLayerInfo? = null,
            sort: Boolean = false
        ): List<BaseItemRenderer<*>> {
            val rendererList = mutableListOf<BaseItemRenderer<*>>()
            val selectList = canvasDelegate.getSelectedRendererList()
            if (selectList.isEmpty()) {
                rendererList.addAll(canvasDelegate.itemsRendererList)
            } else {
                rendererList.addAll(selectList)
            }

            val resultList = rendererList.filter { it.isVisible() }.filter {
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

            return if (sort) {
                resultList.sort()
            } else {
                resultList
            }
        }

        /**获取不是指定雕刻图层的渲染器*/
        fun getRendererListNot(
            canvasDelegate: CanvasDelegate,
            layerInfo: EngraveLayerInfo?,
            sort: Boolean = false
        ): List<BaseItemRenderer<*>> {
            val rendererList = mutableListOf<BaseItemRenderer<*>>()
            val selectList = canvasDelegate.getSelectedRendererList()
            if (selectList.isEmpty()) {
                rendererList.addAll(canvasDelegate.itemsRendererList)
            } else {
                rendererList.addAll(selectList)
            }

            val layerList = rendererList.filter { it.isVisible() }.filter {
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

            rendererList.removeAll(layerList)

            return if (sort) {
                rendererList.sort()
            } else {
                rendererList
            }
        }
    }

    //---

    /**数据转换器*/
    private val transitionList = mutableListOf<IEngraveTransition>()

    init {
        transitionList.add(RawTransition())
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
            val rendererList = getRendererList(canvasDelegate, engraveLayerInfo, true)
            val param = getTransitionParam(rendererList, transferConfigEntity)
            var dataEngraveType = DataCmd.ENGRAVE_TYPE_BITMAP
            rendererList.forEach { renderer ->
                //开始将[renderer]转换成数据
                LTime.tick()
                L.i("开始转换数据->${transferConfigEntity.name}")
                doTransitionTransferData(
                    renderer,
                    transferConfigEntity,
                    param
                )?.let { transferDataEntity ->
                    transferDataEntity.layerMode = engraveLayerInfo.mode
                    dataEngraveType = transferDataEntity.engraveDataType
                    dataList.add(transferDataEntity)
                }
                L.i("转换耗时->${LTime.time()} ${dataEngraveType.toEngraveDataTypeStr()}")
            }
            if (dataList.isNotEmpty()) {
                if (!transferConfigEntity.mergeData || dataList.size() == 1) {
                    //只有1条数据, 不需要合并
                    resultDataList.addAll(dataList)
                } else {
                    //多条数据
                    when (dataEngraveType) {
                        DataCmd.ENGRAVE_TYPE_GCODE -> {
                            //GCode 数据合并
                            val gcodeTransferDataInfo = _mergeTransferData(dataList)
                            resultDataList.add(gcodeTransferDataInfo)
                        }
                        DataCmd.ENGRAVE_TYPE_BITMAP_PATH -> {
                            //线段数据合并
                            if (transferConfigEntity.mergeBpData) {
                                val bitmapPathTransferDataInfo = _mergeTransferData(dataList)
                                resultDataList.add(bitmapPathTransferDataInfo)
                            } else {
                                //不合并线段数据
                                resultDataList.addAll(dataList)
                            }
                        }
                        else -> {
                            DataCmd.ENGRAVE_TYPE_BITMAP_DITHERING
                            //抖动数据不需要合并
                            resultDataList.addAll(dataList)
                        }
                    }
                }
            }
        }
        //入库, 然后就可以通过, 通过任务id获取任务需要传输的数据列表了
        //[com.angcyo.engrave.EngraveFlowDataHelper.getTransferDataList]
        resultDataList.saveAllEntity(LPBox.PACKAGE_NAME)
        return resultDataList
    }

    /**将[itemDataBean]转换成传输给机器的数据,
     * 内部有耗时转换操作, 建议在子线程处理
     * [doTransitionTransferData]
     * */
    @CallPoint
    @WorkerThread
    fun transitionTransferData(
        itemDataBean: CanvasProjectItemBean,
        transferConfigEntity: TransferConfigEntity
    ): TransferDataEntity? {
        val renderItem = GraphicsHelper.parseRenderItemFrom(itemDataBean, null) ?: return null
        val transferDataEntity =
            doTransitionTransferData(renderItem, transferConfigEntity, null)
        //入库, 然后就可以通过, 通过任务id获取任务需要传输的数据列表了
        transferDataEntity?.lpSaveEntity()
        return transferDataEntity
    }

    /**将[renderer]转换成传输给机器的数据,
     * 内部有耗时转换操作, 建议在子线程处理*/
    @CallPoint
    @WorkerThread
    fun doTransitionTransferData(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        param: TransitionParam? = null
    ): TransferDataEntity? {
        var result: TransferDataEntity? = null

        for (transition in transitionList) {
            result =
                transition.doTransitionTransferData(engraveProvider, transferConfigEntity, param)
            if (result != null) {
                break
            }
        }

        //result?.layerMode //?

        return result
    }

    //---

    /**合并数据*/
    fun _mergeTransferData(dataList: List<TransferDataEntity>): TransferDataEntity {
        val resultTransferDataInfo = TransferDataEntity(index = generateEngraveIndex())

        resultTransferDataInfo.dataPath = byteWriter {
            dataList.forEach {
                write(it.bytes())
            }
        }.writeTransferDataPath("${resultTransferDataInfo.index}")
        val first = dataList.first()

        resultTransferDataInfo.taskId = first.taskId
        resultTransferDataInfo.layerMode = first.layerMode

        resultTransferDataInfo.engraveDataType = first.engraveDataType
        resultTransferDataInfo.dpi = first.dpi
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