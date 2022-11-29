package com.angcyo.engrave.ble

import android.graphics.Color
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
import com.angcyo.canvas.CanvasDelegate
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.showIn
import com.angcyo.core.vmApp
import com.angcyo.dialog.itemsDialog
import com.angcyo.dsladapter.toEmpty
import com.angcyo.dsladapter.toError
import com.angcyo.engrave.*
import com.angcyo.engrave.ble.dslitem.EngraveHistoryItem
import com.angcyo.fragment.AbsFragment
import com.angcyo.library.ex.Action
import com.angcyo.library.ex._string
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.toast
import com.angcyo.library.toastQQ
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity_
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.page

/**
 * 历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryFragment : BaseDslFragment(), IEngraveCanvasFragment {

    init {
        fragmentTitle = _string(R.string.ui_slip_menu_history)
        fragmentConfig.isLightStyle = true
        fragmentConfig.showTitleLineView = true

        enableAdapterRefresh = true

        //关闭分页
        page.singlePage()
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
        //engraveProductLayoutHelper.bindCanvasView(_vh, group, null)

        //开始预览
        /*engraveBeforeLayoutHelper.onPreviewAction = {
            toPreview()
        }
        //开始雕刻
        engraveBeforeLayoutHelper.onNextAction = {
            //更新雕刻参数
            toEngrave()
        }*/
        //监听雕刻状态, 结束后刷新数据
        val peckerModel = vmApp<LaserPeckerModel>()
        peckerModel.deviceStateData.observe {
            if (peckerModel.deviceStateData.beforeValue?.isEngraveStop() != true && it?.isEngraveStop() == true) {
                _adapter.updateAllItem()
            }
        }
    }


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
                val indexList = bean?.parse<QueryEngraveFileParser>()?.indexList
                if (indexList.isNullOrEmpty()) {
                    _adapter.toEmpty()
                } else {
                    val allList = EngraveFlowDataHelper.getEngraveData(indexList)
                    //每个索引的文件, 只显示一次
                    val resultList = mutableListOf<EngraveDataEntity>()
                    allList.filterTo(resultList) { entity ->
                        resultList.find { it.index == entity.index } == null
                    }
                    loadDataEnd(resultList)
                }
            } else {
                _adapter.toError(error)
            }
        }
    }

    /**加载app历史记录*/
    fun loadEntityHistoryList() {
        lpBoxOf(EngraveDataEntity::class) {
            val list = page(page) {
                //降序排列
                orderDesc(EngraveDataEntity_.startTime)
            }
            loadDataEnd(list)
        }
    }

    /**加载结束, 渲染界面*/
    fun loadDataEnd(list: List<EngraveDataEntity>) {
        loadDataEnd(EngraveHistoryItem::class.java, list) { bean ->
            val item = this
            itemEngraveDataEntity = bean

            itemLongClick = {
                fContext().itemsDialog {
                    addDialogItem {
                        itemText = _string(R.string.delete_history)
                        itemClick = {
                            //删除机器记录
                            FileModeCmd.deleteHistory(bean.index).enqueue { bean, error ->
                                if (bean?.parse<FileTransferParser>()
                                        ?.isFileDeleteSuccess() == true
                                ) {
                                    toastQQ(_string(R.string.delete_history_succeed))

                                    _adapter.render {
                                        item.removeAdapterItem()
                                    }
                                }
                                error?.let { toast(it.message) }
                            }
                        }
                    }
                    addDialogItem {
                        itemText = _string(R.string.start_preview)
                        itemClick = {
                            toPreview(bean)
                        }
                    }
                    addDialogItem {
                        itemText = _string(R.string.start_engrave)
                        itemClick = {
                            toEngrave(bean)
                        }
                    }
                }
                true
            }

            itemClick = {
                toPreview(bean)
            }
        }
    }

    fun check(action: Action) {
        if (engraveFlowLayoutHelper.isAttach()) {
            return
        }
        if (engraveFlowLayoutHelper.checkRestoreEngrave(this)) {
            return
        }
        if (!engraveFlowLayoutHelper.checkStartPreview()) {
            return
        }
        //安全提示弹窗
        engraveFlowLayoutHelper.showSafetyTips(fContext()) {
            //如果有第三轴, 还需要检查对应的配置
            action()
        }
    }

    /**开始预览*/
    fun toPreview(engraveDataEntity: EngraveDataEntity) {
        check {
            _engraveFlowLayoutHelper.historyEngraveDataEntity = engraveDataEntity
            engraveFlowLayoutHelper.startPreview()
            engraveFlowLayoutHelper.showIn(this)
        }
    }

    /**开始雕刻*/
    fun toEngrave(engraveDataEntity: EngraveDataEntity) {
        check {
            _engraveFlowLayoutHelper.historyEngraveDataEntity = engraveDataEntity
            engraveFlowLayoutHelper.engraveFlow = BaseFlowLayoutHelper.ENGRAVE_FLOW_BEFORE_CONFIG
            engraveFlowLayoutHelper.showIn(this)
        }
    }

    /**雕刻布局*/
    val _engraveFlowLayoutHelper = HistoryEngraveFlowLayoutHelper().apply {
        backPressedDispatcherOwner = this@EngraveHistoryFragment
        flowTaskId = null
    }

    override val fragment: AbsFragment
        get() = this
    override val canvasDelegate: CanvasDelegate?
        get() = null
    override val engraveFlowLayoutHelper: EngraveFlowLayoutHelper
        get() = _engraveFlowLayoutHelper

}