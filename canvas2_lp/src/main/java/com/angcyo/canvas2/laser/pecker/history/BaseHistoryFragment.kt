package com.angcyo.canvas2.laser.pecker.history

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.command.FileModeCmd
import com.angcyo.canvas.render.core.CanvasRenderDelegate
import com.angcyo.canvas2.laser.pecker.IEngraveRenderFragment
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.engrave.BaseFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.engrave.EngraveFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.engrave.HistoryEngraveFlowLayoutHelper
import com.angcyo.canvas2.laser.pecker.history.dslitem.EngraveHistoryItem
import com.angcyo.canvas2.laser.pecker.history.dslitem.EngraveIndexHistoryItem
import com.angcyo.canvas2.laser.pecker.history.dslitem.EngraveTaskHistoryItem
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.core.showIn
import com.angcyo.core.vmApp
import com.angcyo.dialog.itemsDialog
import com.angcyo.dialog.loadLoadingCaller
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.http.rx.doBack
import com.angcyo.http.rx.doMain
import com.angcyo.library.component.batchHandle
import com.angcyo.library.ex._color
import com.angcyo.library.ex._string
import com.angcyo.library.toastQQ
import com.angcyo.objectbox.laser.pecker.lpRemoveAllEntity

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/06
 */
abstract class BaseHistoryFragment : BaseDslFragment(), IEngraveRenderFragment {

    init {
        enableRefresh = true
    }

    override fun onInitFragment(savedInstanceState: Bundle?) {
        fragmentConfig.fragmentBackgroundDrawable =
            ColorDrawable(_color(R.color.lib_theme_white_color))
        super.onInitFragment(savedInstanceState)
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        //监听雕刻状态, 结束后刷新数据
        val deviceStateModel = vmApp<DeviceStateModel>()
        deviceStateModel.deviceStateData.observe {
            if (deviceStateModel.deviceStateData.beforeValue?.isEngraveStop() != true && it?.isEngraveStop() == true) {
                _adapter.updateAllItem()
            }
        }
    }

    override fun onLoadData() {
        super.onLoadData()
        loadHistoryList()
    }

    /**加载历史列表*/
    open fun loadHistoryList() {

    }

    /**事件初始化
     * [onDeleteAction] 删除成功的回调*/
    fun EngraveHistoryItem.initItemClickEvent(onDeleteAction: () -> Unit) {
        val item = this
        itemClick = {
            toPreview(item)
        }
        itemLongClick = {
            fContext().itemsDialog {
                addDialogItem {
                    itemText = _string(R.string.delete_history)
                    itemClick = {
                        //删除机器记录
                        batchDeleteIndex(itemTransferDataEntityList?.map { it.index }) {
                            if (it == null) {
                                //指令删除成功, 清空本地数据库
                                itemTransferDataEntityList?.lpRemoveAllEntity()
                                onDeleteAction()
                                _adapter.render {
                                    item.removeAdapterItem()
                                }
                            }
                        }
                    }
                }
                addDialogItem {
                    itemText = _string(R.string.start_preview)
                    itemClick = {
                        toPreview(item)
                    }
                }
                addDialogItem {
                    itemText = _string(R.string.start_engrave)
                    itemClick = {
                        toEngrave(item)
                    }
                }
            }
            true
        }
    }

    /**初始化相应的数据*/
    fun initEngraveHistoryFlowLayout(item: EngraveHistoryItem) {
        if (item is EngraveTaskHistoryItem) {
            _engraveFlowLayoutHelper.appHistoryEngraveTaskEntity = item.itemEngraveTaskEntity
        }
        if (item is EngraveIndexHistoryItem) {
            _engraveFlowLayoutHelper.deviceHistoryEngraveDataEntity = item.itemEngraveDataEntity
        }
        _engraveFlowLayoutHelper.transferDataEntityList = item.itemTransferDataEntityList
    }

    /**批量删除索引*/
    fun batchDeleteIndex(list: List<Int>?, onEnd: (Throwable?) -> Unit) {
        if (list.isNullOrEmpty()) return
        loadLoadingCaller { cancel, loadEnd ->
            doBack {
                list.batchHandle({ index ->
                    FileModeCmd.deleteHistory(index).enqueue { bean, error ->
                        error?.let { this.error = it }
                        next()
                    }
                }) {
                    doMain {
                        loadEnd(true, error)
                        onEnd(error)
                        if (error == null) {
                            toastQQ(_string(R.string.delete_history_succeed))
                        } else {
                            toastQQ(_string(R.string.delete_history_failed))
                        }
                    }
                }
            }
        }
    }

    /**开始预览*/
    open fun toPreview(item: EngraveHistoryItem) {
        initEngraveHistoryFlowLayout(item)
        flowLayoutHelper.startPreview(this)
    }

    /**开始雕刻*/
    open fun toEngrave(item: EngraveHistoryItem) {
        if (flowLayoutHelper.checkCanStartPreview(this)) {
            initEngraveHistoryFlowLayout(item)
            flowLayoutHelper.engraveFlow = if (item is EngraveIndexHistoryItem) {
                //机器数据, 数据一定存在
                BaseFlowLayoutHelper.ENGRAVE_FLOW_BEFORE_CONFIG
            } else {
                //app的历史记录, 有可能没有传输数据
                BaseFlowLayoutHelper.ENGRAVE_FLOW_AUTO_TRANSFER
            }
            flowLayoutHelper.showIn(this)
        }
    }

    /**雕刻布局*/
    val _engraveFlowLayoutHelper = HistoryEngraveFlowLayoutHelper().apply {
        backPressedDispatcherOwner = this@BaseHistoryFragment
        clearFlowId("初始化历史雕刻流程")
    }

    override val fragment: AbsLifecycleFragment
        get() = this

    override val renderDelegate: CanvasRenderDelegate?
        get() = null

    override val flowLayoutHelper: EngraveFlowLayoutHelper
        get() = _engraveFlowLayoutHelper.apply {
            engraveCanvasFragment = this@BaseHistoryFragment
        }

    override val flowLayoutContainer: ViewGroup?
        get() = _vh.group(R.id.lib_content_overlay_wrap_layout) ?: _vh.itemView as ViewGroup

    override val dangerLayoutContainer: ViewGroup?
        get() = null
}