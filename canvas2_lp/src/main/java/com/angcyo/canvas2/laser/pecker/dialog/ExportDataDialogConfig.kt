package com.angcyo.canvas2.laser.pecker.dialog

import android.app.Dialog
import android.content.Context
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker._deviceConfigBean
import com.angcyo.bluetooth.fsc.laserpacker._deviceSettingBean
import com.angcyo.bluetooth.fsc.laserpacker.command.EngraveCmd
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper
import com.angcyo.canvas2.laser.pecker.engrave.LPTransferHelper
import com.angcyo.canvas2.laser.pecker.engrave.config.EngraveConfigProvider
import com.angcyo.canvas2.laser.pecker.engrave.config.IEngraveConfigProvider
import com.angcyo.canvas2.laser.pecker.engrave.config.IEngraveConfigTaskProvider
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.EngraveSegmentScrollItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveLayerConfigItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveMaterialWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngraveOptionWheelItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravePropertyItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.EngravePumpItem
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.engrave.appendDrawable
import com.angcyo.canvas2.laser.pecker.engrave.dslitem.transfer.TransferDataNameItem
import com.angcyo.core.component.file.writeToLog
import com.angcyo.core.tgStrokeLoadingCaller
import com.angcyo.core.vmApp
import com.angcyo.dialog.BaseRecyclerDialogConfig
import com.angcyo.dialog.configBottomDialog
import com.angcyo.dialog2.dslitem.itemSelectedIndex
import com.angcyo.dialog2.dslitem.itemWheelList
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.RecyclerItemFlowAnimator
import com.angcyo.dsladapter.itemIndexPosition
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.engrave2.model.TransferModel
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.item.style.itemCurrentIndex
import com.angcyo.item.style.itemLabelText
import com.angcyo.laserpacker.LPDataConstant
import com.angcyo.laserpacker.bean.LPProjectBean
import com.angcyo.laserpacker.device.EngraveHelper
import com.angcyo.laserpacker.device.LayerHelper
import com.angcyo.laserpacker.device.MaterialHelper
import com.angcyo.laserpacker.device.data.EngraveLayerInfo
import com.angcyo.library.L
import com.angcyo.library.annotation.DSL
import com.angcyo.library.component.VersionMatcher
import com.angcyo.library.component.byteWriter
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._string
import com.angcyo.library.ex.ceil
import com.angcyo.library.ex.shareFile
import com.angcyo.library.ex.update
import com.angcyo.library.ex.uuid
import com.angcyo.library.libCacheFile
import com.angcyo.library.unit.IValueUnit
import com.angcyo.library.utils.writeToFile
import com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.MaterialEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.lpSaveEntity
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 导出U盘数据lpb
 *
 * [com.angcyo.laserpacker.LPDataConstant.LPB_EXT]
 *
 * [com.angcyo.canvas2.laser.pecker.engrave.LPEngraveHelper.generateTransferConfig]
 * [com.angcyo.canvas2.laser.pecker.engrave.LPTransferHelper.transitionTransferData]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/09/18
 */
class ExportDataDialogConfig(context: Context? = null) : BaseRecyclerDialogConfig(context),
    IEngraveConfigTaskProvider {

    /**需要导出的数据*/
    var renderDelegate: CanvasRenderDelegate? = null

    /**雕刻配置提供者*/
    var engraveConfigProvider: IEngraveConfigProvider = EngraveConfigProvider()

    private var outputFile: File? = null
    private lateinit var taskId: String
    private lateinit var transferConfigEntity: TransferConfigEntity

    private val deviceStateModel = vmApp<DeviceStateModel>()
    private val laserPeckerModel = vmApp<LaserPeckerModel>()

    override var engraveConfigTaskId: String?
        get() = taskId
        set(value) {
            taskId = value ?: "null"
        }

    override var engraveConfigProjectBean: LPProjectBean? = null

    init {
        dialogTitle = _string(R.string.canvas_export_data)

        positiveButton { dialog, dialogViewHolder ->
            if (outputFile == null) {
                createLpbData {
                    outputFile?.shareFile()
                }
            } else {
                outputFile?.shareFile()
            }
            //dialog.dismiss()
        }
    }

    override fun onDialogDestroy(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        super.onDialogDestroy(dialog, dialogViewHolder)
        EngraveFlowDataHelper.clearTask(taskId)
    }

    override fun initDialogView(dialog: Dialog, dialogViewHolder: DslViewHolder) {
        taskId = uuid()
        transferConfigEntity = LPEngraveHelper.generateTransferConfig(taskId, renderDelegate)

        super.initDialogView(dialog, dialogViewHolder)
    }

    /**创建lpb数据
     * [com.angcyo.laserpacker.LPDataConstant.LPB_EXT]*/
    private fun createLpbData(action: Action) {
        val delegate = renderDelegate ?: return
        tgStrokeLoadingCaller { isCancel, loadEnd ->
            doBack {
                val list =
                    LPTransferHelper.transitionTransferData(delegate, transferConfigEntity, false)

                //有效图片数据个数
                var itemCount = 0

                val bytes = byteWriter {
                    write("LPDT")
                    write(6, 2)//目前固定是6
                    write(1) //数据版本

                    val width = ((transferConfigEntity.originWidth ?: 0f) * 10).ceil().toInt()
                    val height = ((transferConfigEntity.originHeight ?: 0f) * 10).ceil().toInt()
                    write(width, 2)
                    write(height, 2)

                    write(0) //图片数量, 先占位,后面赋值

                    //图片数据
                    for (transferDataEntity in list) {
                        val dataCmd = TransferModel.getTransferDataCmd(transferDataEntity)
                        dataCmd?.let {
                            val engraveConfigEntity = EngraveFlowDataHelper.getEngraveConfig(
                                taskId,
                                transferDataEntity.layerId
                            )
                            engraveConfigEntity?.let {
                                val itemHeaderBytes = byteWriter {
                                    //write(0, 2) //图片数据头部字节长度, 先占位,后面赋值
                                    write(engraveConfigEntity.power)
                                    write(EngraveCmd.depthToSpeed(engraveConfigEntity.depth))
                                    write(engraveConfigEntity.type)
                                    write(engraveConfigEntity.time)
                                    //HawkEngraveKeys.lastDiameterPixel
                                    val diameter =
                                        (IValueUnit.MM_UNIT.convertPixelToValue(engraveConfigEntity.diameterPixel) * 100).roundToInt()
                                    write(diameter)
                                    write(engraveConfigEntity.precision)
                                }
                                write(itemHeaderBytes.size, 2)
                                write(itemHeaderBytes)
                                write(dataCmd.toByteArray())
                                itemCount++
                            }
                        }
                    }
                }
                bytes.update(11, itemCount)

                val file = libCacheFile(transferConfigEntity.name + LPDataConstant.LPB_EXT)
                outputFile = file
                bytes.writeToFile(file)

                loadEnd(true, null)
                doMain {
                    action()
                }
            }
        }
    }

    //---

    /**选过哪些图层, 用来标识对应的图层配置过参数*/
    val selectLayerList = mutableSetOf<String>()

    /**当前选中的图层id
     * [EngraveLayerConfigItem]*/
    var selectLayerId: String =
        LayerHelper.getEngraveLayerList().firstOrNull()?.layerId ?: HawkEngraveKeys.lastLayerId

    /**
     * 主流程参数配置
     * [com.angcyo.canvas2.laser.pecker.engrave.BaseEngraveLayoutHelper.renderEngraveConfig]
     * */
    override fun DslAdapter.onSelfRenderAdapter() {
        outputFile = null //clear

        TransferDataNameItem()() {
            itemTransferConfigEntity = transferConfigEntity
            observeItemChange {
                outputFile = null //clear
            }
        }
        val layerList = LPEngraveHelper.getSelectElementLayerInfoList(renderDelegate)
        val findLayer = layerList.find { it.layerId == selectLayerId }
        if (findLayer == null) {
            //选中的图层不存在, 则使用第一个
            selectLayerId = layerList.firstOrNull()?.layerId ?: selectLayerId
        }

        //图层添加选中后的图标
        selectLayerList.add(selectLayerId)
        val layerIconList = layerList.map {
            EngraveLayerInfo(
                it.layerId,
                span {
                    if (selectLayerList.contains(it.layerId)) {
                        appendDrawable(R.drawable.canvas_layer_selected)
                    }
                    append(it.label)
                },
                it.isGroupExtend,
                it.showDpiConfig
            )
        }

        val materialEntity =
            engraveConfigProvider.getEngraveMaterial(this@ExportDataDialogConfig, selectLayerId)
        "任务:${taskId} [$selectLayerId]已选材质:$materialEntity".writeToLog(logLevel = L.INFO)

        //当前选中图层的雕刻配置
        var engraveConfigEntity: EngraveConfigEntity? =
            engraveConfigProvider.getEngraveConfigList(this@ExportDataDialogConfig).find {
                it.layerId == selectLayerId
            }

        if (engraveConfigEntity == null) {
            //如果没有材质中没有找到对应图层的配置, 则构建一个
            engraveConfigEntity =
                engraveConfigProvider.getEngraveConfig(this@ExportDataDialogConfig, selectLayerId)
        }

        /*"任务:${taskId} 图层[$selectLayerId] 材质:${materialEntity} 参数:${engraveConfigEntity}".writeToLog(
                   logLevel = L.INFO
               )*/

        //雕刻图层切换
        if (layerIconList.isNotEmpty()) {
            EngraveLayerConfigItem()() {
                itemSegmentList = layerIconList
                itemCurrentIndex = max(
                    0,
                    layerIconList.indexOf(layerIconList.find { it.layerId == selectLayerId })
                )
                observeItemChange {
                    selectLayerId = layerIconList[itemCurrentIndex].layerId
                    val itemIndexPosition = it.itemIndexPosition()
                    if (itemIndexPosition != -1) {
                        //图层改变后, 动画提示参数变化
                        RecyclerItemFlowAnimator(
                            itemIndexPosition + 1,
                            -1
                        ).start(it.itemDslAdapter?._recyclerView)
                    }
                    refreshDslAdapter()
                }
            }
        }

        if (!deviceStateModel.isPenMode()) { //握笔模块, 不需要材质
            //材质选择
            EngraveMaterialWheelItem()() {
                itemTag = MaterialEntity::name.name
                itemLabelText = _string(R.string.custom_material)
                itemWheelList = MaterialHelper.getLayerMaterialList(selectLayerId)
                itemSelectedIndex = MaterialHelper.indexOfMaterial(
                    itemWheelList as List<MaterialEntity>,
                    materialEntity
                )
                itemEngraveConfigEntity = engraveConfigEntity

                //刷新界面
                observeItemChange {
                    refreshDslAdapter()
                }
            }
        }

        // 激光光源选择
        val typeList = LaserPeckerHelper.findProductSupportLaserTypeList()
        if (laserPeckerModel.productInfoData.value?.isCSeries() != true && typeList.isNotEmpty()) {
            EngraveSegmentScrollItem()() {
                itemText = _string(R.string.laser_type)
                itemSegmentList = typeList
                itemCurrentIndex =
                    typeList.indexOfFirst { it.type == engraveConfigEntity.type }
                observeItemChange {
                    val type = typeList[itemCurrentIndex].type
                    HawkEngraveKeys.lastType = type.toInt()
                    engraveConfigEntity.type = type
                    engraveConfigEntity.lpSaveEntity()
                    refreshDslAdapter()
                }
                observeMaterialChange()
            }
        }

        if (laserPeckerModel.isCSeries()) {
            //C1 加速级别选择 加速级别
            if (engraveConfigEntity.precision < 0) {
                engraveConfigEntity.precision = 1
                engraveConfigEntity.lpSaveEntity()
            }
            EngraveOptionWheelItem()() {
                itemTag = EngraveConfigEntity::precision.name
                itemLabelText = _string(R.string.engrave_precision)
                itemWheelList = EngraveHelper.percentList(5)
                itemSelectedIndex = EngraveHelper.findOptionIndex(
                    itemWheelList,
                    engraveConfigEntity.precision
                )
                itemEngraveConfigEntity = engraveConfigEntity
                observeMaterialChange()
            }
        }

        //风速等级
        if (VersionMatcher.matches(
                laserPeckerModel.productInfoData.value?.version,
                _deviceSettingBean?.showPumpRange,
                false,
                true
            )
        ) {
            val pumpList = _deviceConfigBean?.pumpMap?.get(engraveConfigEntity.layerId)
            if (!pumpList.isNullOrEmpty()) {
                EngravePumpItem()() {
                    itemEngraveConfigEntity = engraveConfigEntity
                    initPumpIfNeed()
                    itemSegmentList = pumpList
                    itemCurrentIndex = max(
                        0,
                        pumpList.indexOf(pumpList.find { it.value == engraveConfigEntity.pump })
                    )
                }
            }
        }

        //雕刻参数
        if (deviceStateModel.isPenMode()) {
            //握笔模块, 雕刻速度, 非雕刻深度
            engraveConfigEntity.power = 100 //功率必须100%
            engraveConfigEntity.lpSaveEntity()
            //雕刻速度
            EngraveOptionWheelItem()() {
                itemTag = MaterialEntity.SPEED
                itemLabelText = _string(R.string.engrave_speed)
                itemWheelList = EngraveHelper.percentList()
                itemEngraveConfigEntity = engraveConfigEntity
                itemSelectedIndex = EngraveHelper.findOptionIndex(
                    itemWheelList,
                    EngraveCmd.depthToSpeed(engraveConfigEntity.depth)
                )
                observeMaterialChange()
            }
        } else {
            //功率/深度/次数
            EngravePropertyItem()() {
                itemEngraveConfigEntity = engraveConfigEntity
                observeMaterialChange()
            }
        }
    }

    /**监听改变之后, 显示材质保存按钮*/
    fun DslAdapterItem.observeMaterialChange() {
        observeItemChange {
            outputFile = null //clear
            /* 2023-9-18 这里暂不开放
                itemDslAdapter?.find<EngraveMaterialWheelItem>()?.let {
                    it._materialEntity?.isChanged = true
                    it.updateAdapterItem()
                }*/
        }
    }
}

/**数据导出对话框*/
@DSL
fun Context.exportDataDialogConfig(
    delegate: CanvasRenderDelegate?,
    config: ExportDataDialogConfig.() -> Unit = {}
) {
    return ExportDataDialogConfig(this).run {
        renderDelegate = delegate
        configBottomDialog(this@exportDataDialogConfig)
        config()
        show()
    }
}

