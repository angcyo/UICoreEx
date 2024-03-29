package com.angcyo.canvas2.laser.pecker.engrave

import android.graphics.Paint
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.bean._useCutData
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas.render.renderer.BaseRenderer
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.preview.GCodeDataOffsetItem
import com.angcyo.canvas2.laser.pecker.util.lpElement
import com.angcyo.canvas2.laser.pecker.util.lpElementBean
import com.angcyo.core.component.file.writeErrorLog
import com.angcyo.core.component.file.writePerfLog
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.vmApp
import com.angcyo.coroutine.sleep
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.data.TransferState
import com.angcyo.engrave2.data.TransitionParam
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.engrave2.transition.EngraveTransitionHelper
import com.angcyo.http.rx.doBack
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.exception.TransferException
import com.angcyo.laserpacker.toEngraveDataTypeStr
import com.angcyo.laserpacker.toPaintStyleInt
import com.angcyo.library.L
import com.angcyo.library.LTime
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.MM
import com.angcyo.library.component.hawk.LibHawkKeys
import com.angcyo.library.ex.classHash
import com.angcyo.library.ex.postDelay
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toStr
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity
import com.angcyo.objectbox.saveAllEntity

/**
 * 业务相关的数据传输助手工具类
 *
 * [BaseRenderer] -> [TransferDataEntity]
 *
 * [com.angcyo.engrave2.transition.EngraveTransitionHelper] 核心转换助手
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/29
 */
object LPTransferHelper {

    /**开始创建机器需要的传输数据*/
    @CallPoint
    @AnyThread
    fun startCreateTransferData(
        transferModel: TransferModel,
        taskId: String?,
        canvasDelegate: CanvasRenderDelegate?
    ) {
        canvasDelegate ?: return
        //清空之前之前的所有传输数据
        "开始创建传输数据[$taskId]".writeToLog()
        EngraveFlowDataHelper.removeTransferDataState(taskId)
        transferModel.stopTransfer()

        canvasDelegate.refresh()
        canvasDelegate.view.postDelay(LibHawkKeys.minPostDelay) {//2023-5-13 漂移, 漏打, 神一样的BUG, 需要鬼一样的代码
            doBack {
                //传输状态, 开始创建数据
                "即将创建传输数据[$taskId]".writeToLog()
                val transferState = TransferState(taskId, progress = -1)
                try {
                    EngraveFlowDataHelper.onStartCreateTransferData(taskId)
                    transferModel.transferStateOnceData.postValue(transferState)
                    val transferConfigEntity =
                        EngraveFlowDataHelper.getOrGenerateTransferConfig(taskId)
                    //数据已入库, 可以直接在数据库中查询
                    val entityList = transitionTransferData(canvasDelegate, transferConfigEntity)
                    val size = entityList.size()
                    if (size < 5) {
                        "已创建传输数据[$taskId]:$entityList".writeToLog()
                    } else {
                        "已创建传输数据[$taskId]:[$size]个".writeToLog()
                    }
                    EngraveFlowDataHelper.onFinishCreateTransferData(taskId)
                    transferModel.startTransferData(transferState.taskId)

                    transferModel.updateTransferMessage(null)
                } catch (e: Exception) {
                    transferModel.updateTransferMessage(e.toStr())
                    e.printStackTrace()
                    e.toStr().writeErrorLog()
                    transferModel.errorTransfer(transferState, TransferException())
                }
            }
            "请等待数据创建完成[$taskId]".writeToLog()
        }
    }

    /**相同类型的[TransferDataInfo]会合并在一起, 会根据列表顺序, 生成对应顺序的数据
     * [com.angcyo.engrave.transition.EngraveTransitionManager.engraveLayerList]*/
    @CallPoint
    @WorkerThread
    fun transitionTransferData(
        delegate: CanvasRenderDelegate,
        transferConfigEntity: TransferConfigEntity,
        useItemEngraveParams: Boolean = HawkEngraveKeys.enableItemEngraveParams
    ): List<TransferDataEntity> {
        val resultDataList = mutableListOf<TransferDataEntity>()

        if (HawkEngraveKeys.enableItemTopOrder) {
            //从上往下的雕刻顺序
            val rendererList = LPEngraveHelper.getLayerRendererList(delegate, null, true)
            if (rendererList.isNotEmpty()) {
                resultDataList.addAll(
                    transitionTransferData(
                        rendererList,
                        transferConfigEntity,
                        null,
                        useItemEngraveParams
                    )
                )
            }
        } else {
            //规定的图层雕刻顺序
            for (engraveLayerInfo in LayerHelper.getEngraveLayerList()) {
                val rendererList =
                    LPEngraveHelper.getLayerRendererList(delegate, engraveLayerInfo, false)
                if (rendererList.isNotEmpty()) {
                    resultDataList.addAll(
                        transitionTransferData(
                            rendererList,
                            transferConfigEntity,
                            engraveLayerInfo.layerId,
                            useItemEngraveParams
                        )
                    )
                }
            }
        }
        //入库, 然后就可以通过, 通过任务id获取任务需要传输的数据列表了
        //[com.angcyo.engrave.EngraveFlowDataHelper.getTransferDataList]
        resultDataList.saveAllEntity(LPBox.PACKAGE_NAME)
        return resultDataList
    }

    /**将渲染的item[BaseRenderer], 转换成[TransferDataEntity]
     *
     * [layerId] 指定图层的id, 不指定则从元素中获取
     * [useItemEngraveParams] 是否要使用元素自己的雕刻参数
     * */
    private fun transitionTransferData(
        rendererList: List<BaseRenderer>,
        transferConfigEntity: TransferConfigEntity,
        layerId: String?,
        useItemEngraveParams: Boolean
    ): List<TransferDataEntity> {
        val resultDataList = mutableListOf<TransferDataEntity>()
        val taskId = transferConfigEntity.taskId
        for (renderer in rendererList) {
            //开始将[renderer]转换成数据
            sleep(HawkEngraveKeys.transferIndexSleep)
            LTime.tick()
            val elementBean = renderer.lpElementBean()
            val elementLayerId = layerId ?: elementBean?._layerId

            val transferConfig = if (useItemEngraveParams) {
                //单元素雕刻参数, 使用元素自己的参数
                val index = elementBean?.index ?: EngraveHelper.generateEngraveIndex()
                elementBean?.index = index
                LPEngraveHelper.getTransferConfig(taskId, "$index")!! // ?: transferConfigEntity
            } else {
                transferConfigEntity
            }

            vmApp<TransferModel>().updateTransferMessage("开始转换数据->${transferConfig.name} ${elementBean?.index}")

            "开始转换数据->${transferConfig.name} ${elementBean?.index} 元素名:${elementBean?.name} $elementLayerId".writePerfLog()
            val transferDataEntity = transitionRenderer(renderer, transferConfig)
            if (transferDataEntity == null) {
                "转换传输数据失败->${transferConfig.name} ${elementBean?.index} 元素名:${elementBean?.name}".writeErrorLog()
            } else {
                transferDataEntity.layerId = elementLayerId
                resultDataList.add(transferDataEntity)
                "转换传输数据耗时[${transferDataEntity.index}]->${LTime.time()} ${transferDataEntity.name} ${transferDataEntity.engraveDataType.toEngraveDataTypeStr()}".writePerfLog()
            }
        }
        if (resultDataList.size() > 1 && HawkEngraveKeys.engraveDataLogLevel >= L.INFO) {
            //数据个数大于1, 生成坐标的鸟瞰图
            EngraveTransitionHelper.saveTaskAerialView(taskId, resultDataList)
        }
        return resultDataList
    }

    /**将[renderer]转换成对应的传输数据
     * [com.angcyo.canvas2.laser.pecker.element.ILaserPeckerElement]
     * [com.angcyo.engrave2.transition.IEngraveDataProvider]
     * */
    fun transitionRenderer(
        renderer: BaseRenderer?,
        transferConfigEntity: TransferConfigEntity,
        @MM
        pathStep: Float? = null
    ): TransferDataEntity? {
        renderer ?: return null
        val element = renderer.lpElement() ?: return null
        val bean = renderer.lpElementBean() ?: return null
        val result: TransferDataEntity? = when (bean._layerMode) {
            LPDataConstant.DATA_MODE_GCODE -> {
                //线条图层, 发送GCode数据

                val transitionParam = TransitionParam(
                    isSingleLine = bean.isLineShape,
                    useOpenCvHandleGCode = true,
                    gcodeOffsetLeft = GCodeDataOffsetItem.offsetLeft,
                    gcodeOffsetTop = GCodeDataOffsetItem.offsetTop,
                    enableGCodeCutData = bean._layerId == LaserPeckerHelper.LAYER_CUT && _useCutData,
                    enableSlice = bean.isNeedSlice,
                    sliceCount = bean.sliceCount,
                    sliceHeight = bean.sliceHeight,
                    sliceGranularity = bean.sliceGranularity,
                    pathStep = pathStep
                )

                if (bean.isLineShape || bean.paintStyle != Paint.Style.STROKE.toPaintStyleInt()) {
                    //虚线,实线,或者填充的图片, 都是GCode数据, 使用pixel转GCode
                    if (bean.isNeedSlice) {
                        //切片的情况下, 使用OpenCV转GCode
                    } else {
                        transitionParam.useOpenCvHandleGCode = false
                    }
                    transitionParam.onlyUseBitmapToGCode =
                        bean.isLineShape && bean.paintStyle == Paint.Style.STROKE.toPaintStyleInt()
                } else {
                    //其他情况下, 转GCode优先使用Path, 再使用OpenCV
                }
                EngraveTransitionHelper.transitionToGCode(
                    element,
                    transferConfigEntity,
                    transitionParam
                )
            }

            LPDataConstant.DATA_MODE_BLACK_WHITE -> {
                //填充图层, 发送图片线段数据
                EngraveTransitionHelper.transitionToBitmapPath(element, transferConfigEntity)
            }

            LPDataConstant.DATA_MODE_DITHERING -> {
                //图片图层, 发送抖动线段数据
                EngraveTransitionHelper.transitionToBitmapDithering(
                    element,
                    transferConfigEntity,
                    TransitionParam(
                        bean.imageFilter == LPDataConstant.DATA_MODE_DITHERING,
                        bean.inverse,
                        bean.contrast,
                        bean.brightness
                    )
                )
            }

            LPDataConstant.DATA_MODE_GREY -> {
                //旧的图片图层, 发送图片数据
                EngraveTransitionHelper.transitionToBitmap(element, transferConfigEntity)
            }

            else -> {
                "无法处理的元素[${element.classHash()}]:${bean._layerMode}".writeErrorLog(logLevel = L.WARN)
                null
            }
        }

        //图层模式
        result?.apply {
            layerId = bean._layerId
        }

        return result
    }

}