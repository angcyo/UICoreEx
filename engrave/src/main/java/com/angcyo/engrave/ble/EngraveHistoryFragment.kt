package com.angcyo.engrave.ble

import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.FileTransferParser
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryEngraveFileParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.showIn
import com.angcyo.core.vmApp
import com.angcyo.dialog.itemsDialog
import com.angcyo.dsladapter.toEmpty
import com.angcyo.dsladapter.toError
import com.angcyo.engrave.*
import com.angcyo.engrave.ble.dslitem.EngraveHistoryItem
import com.angcyo.engrave.data.EngraveDataInfo
import com.angcyo.engrave.data.EngraveOptionInfo
import com.angcyo.engrave.data.EngraveReadyDataInfo
import com.angcyo.engrave.data.PreviewBoundsInfo
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.toast
import com.angcyo.objectbox.deleteEntity
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity_
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.page
import com.angcyo.library.ex.*

/**
 * 历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryFragment : BaseDslFragment() {

    /**前布局*/
    val engraveBeforeLayoutHelper = EngraveBeforeLayoutHelper()

    /**雕刻布局*/
    val engraveLayoutHelper = EngraveLayoutHelper()

    /**雕刻预览布局*/
    val engravePreviewLayoutHelper = EngravePreviewLayoutHelper(this).apply {
        //next
        onNextAction = {
            toEngrave()
        }
    }

    /**产品布局*/
    val engraveProductLayoutHelper = EngraveProductLayoutHelper(this)

    init {
        fragmentTitle = _string(R.string.ui_slip_menu_history)

        enableAdapterRefresh = true
    }

    override fun onInitFragment(savedInstanceState: Bundle?) {
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(Color.WHITE)
        super.onInitFragment(savedInstanceState)
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        //product _vh.group(R.id.lib_content_wrap_layout) ?:
        //lib_content_overlay_wrap_layout
        val group = _vh.group(R.id.lib_content_overlay_wrap_layout) ?: _vh.itemView as ViewGroup
        engraveProductLayoutHelper.bindCanvasView(_vh, group, null)

        //开始预览
        engraveBeforeLayoutHelper.onPreviewAction = {
            toPreview()
        }
        //开始雕刻
        engraveBeforeLayoutHelper.onNextAction = {
            //更新雕刻参数
            toEngrave()
        }
        //监听雕刻状态, 结束后刷新数据
        val peckerModel = vmApp<LaserPeckerModel>()
        peckerModel.deviceStateData.observe {
            if (peckerModel.deviceStateData.beforeValue?.isEngraveStop() != true && it?.isEngraveStop() == true) {
                _adapter.updateAllItem()
            }
        }
    }

    var _readyDataInfo: EngraveReadyDataInfo? = null
    var _engraveOption: EngraveOptionInfo? = null

    override fun onLoadData() {
        super.onLoadData()
        if (isDebugType()) {
            //loadEntityHistoryList()
        }
        loadDeviceHistoryList()
    }

    /**加载设备雕刻记录*/
    fun loadDeviceHistoryList() {
        QueryCmd.fileList.enqueue { bean, error ->
            finishRefresh()
            if (error == null) {
                val nameList = bean?.parse<QueryEngraveFileParser>()?.nameList
                if (nameList.isNullOrEmpty()) {
                    _adapter.toEmpty()
                } else {
                    val allList = lpBoxOf(EngraveHistoryEntity::class).all
                    val resultList = allList.filter { nameList.contains(it.index) }
                    loadDataEnd(resultList)
                }
            } else {
                _adapter.toError(error)
            }
        }
    }

    /**加载app历史记录*/
    fun loadEntityHistoryList() {
        lpBoxOf(EngraveHistoryEntity::class) {
            val list = page(page) {
                //降序排列
                orderDesc(EngraveHistoryEntity_.entityId)
            }
            loadDataEnd(list)
        }
    }

    /**加载结束, 渲染界面*/
    fun loadDataEnd(list: List<EngraveHistoryEntity>) {
        loadDataEnd(EngraveHistoryItem::class.java, list) { bean ->
            val item = this
            engraveHistoryEntity = bean

            itemLongClick = {
                selectHistoryEntity(bean)
                fContext().itemsDialog {
                    addDialogItem {
                        itemText = "删除历史"
                        itemClick = {
                            //删除机器记录
                            FileModeCmd.deleteHistory(bean.index ?: 0).enqueue { bean, error ->
                                if (bean?.parse<FileTransferParser>()
                                        ?.isFileDeleteSuccess() == true
                                ) {
                                    toast("删除成功")

                                    _adapter.render {
                                        item.removeAdapterItem()
                                    }
                                    //删除本地记录
                                    engraveHistoryEntity?.deleteEntity(LPBox.PACKAGE_NAME)
                                }
                                error?.let { toast(it.message) }
                            }
                        }
                    }
                    addDialogItem {
                        itemText = _string(R.string.v3_bmp_setting_preview)
                        itemClick = {
                            toPreview()
                        }
                    }
                    addDialogItem {
                        itemText = _string(R.string.print_v2_package_Laser_start)
                        itemClick = {
                            toEngrave()
                        }
                    }
                }
                true
            }

            itemClick = {
                //数据不存在, 需要重新发送数据
                selectHistoryEntity(bean)
                engraveBeforeLayoutHelper.iViewTitle = bean.name
                engraveBeforeLayoutHelper.engraveReadyDataInfo = _readyDataInfo
                engraveBeforeLayoutHelper.showIn(this@EngraveHistoryFragment)
            }
        }
    }

    /**准备数据*/
    fun selectHistoryEntity(entity: EngraveHistoryEntity) {
        //准备雕刻数据
        val readyDataInfo = EngraveReadyDataInfo(entity, EngraveDataInfo().updateFromEntity(entity))
        readyDataInfo.optionMode = entity.optionMode
        readyDataInfo.dataPath = entity.dataPath
        readyDataInfo.previewDataPath = entity.previewDataPath
        _readyDataInfo = readyDataInfo

        //准备雕刻参数
        _engraveOption = EngraveOptionInfo(
            entity.material ?: _string(R.string.material_custom),
            entity.power,
            entity.depth,
            clamp(entity.printTimes, 1, 100).toByte(),//最小打印1次
            entity.x,
            entity.y,
            entity.type
        )
    }

    /**开始预览*/
    fun toPreview() {
        engravePreviewLayoutHelper.showPreviewSafetyTips(fContext()) {
            _readyDataInfo?.historyEntity?.let {
                engravePreviewLayoutHelper.previewBoundsInfo = PreviewBoundsInfo(
                    RectF(
                        it.x.toFloat(),
                        it.y.toFloat(),
                        (it.x + it.width).toFloat(),
                        (it.y + it.height).toFloat()
                    )
                )
                engravePreviewLayoutHelper.showIn(this)
            }
        }
    }

    /**去雕刻*/
    fun toEngrave() {
        vmApp<EngraveModel>().engraveOptionInfoData.value = _engraveOption

        engraveLayoutHelper.engraveReadyDataInfo = _readyDataInfo
        engraveLayoutHelper.showIn(this)
    }

}