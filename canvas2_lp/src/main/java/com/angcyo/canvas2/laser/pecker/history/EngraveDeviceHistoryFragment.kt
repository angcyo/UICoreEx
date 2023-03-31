package com.angcyo.canvas2.laser.pecker.history

import android.os.Bundle
import com.angcyo.bluetooth.fsc.enqueue
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.bluetooth.fsc.laserpacker.command.QueryCmd
import com.angcyo.bluetooth.fsc.laserpacker.parse.QueryEngraveFileParser
import com.angcyo.bluetooth.fsc.parse
import com.angcyo.canvas2.laser.pecker.history.dslitem.EngraveIndexHistoryItem
import com.angcyo.dsladapter.toEmpty
import com.angcyo.dsladapter.toError
import com.angcyo.engrave2.EngraveFlowDataHelper
import com.angcyo.objectbox.laser.pecker.entity.EngraveDataEntity

/**
 * 设备历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-1-5
 */
class EngraveDeviceHistoryFragment : BaseHistoryFragment() {

    init {
        //关闭分页
        page.singlePage()
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)

        //product _vh.group(R.id.lib_content_wrap_layout) ?:
        //lib_content_overlay_wrap_layout
        //val group = _vh.group(R.id.lib_content_overlay_wrap_layout) ?: _vh.itemView as ViewGroup
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
    }

    /**加载设备雕刻记录*/
    override fun loadHistoryList() {
        QueryCmd.fileList.enqueue { bean, error ->
            finishRefresh()
            if (error == null) {
                val indexList = bean?.parse<QueryEngraveFileParser>()?.indexList
                if (indexList.isNullOrEmpty()) {
                    _adapter.toEmpty()
                } else {
                    //每个索引的文件, 只显示一次
                    val resultList = mutableListOf<EngraveDataEntity>()
                    for (index in indexList) {
                        val engraveData = EngraveFlowDataHelper.getEngraveData(
                            index,
                            false,
                            LaserPeckerHelper.lastDeviceAddress()
                        )
                        if (engraveData != null) {
                            resultList.add(engraveData)
                        }
                    }
                    resultList.sortByDescending { it.startTime } //排序, 根据雕刻开始的时间逆序排列
                    loadDataEnd(resultList)
                }
            } else {
                _adapter.toError(error)
            }
        }
    }

    /**加载结束, 渲染界面*/
    fun loadDataEnd(list: List<EngraveDataEntity>) {
        loadDataEnd(EngraveIndexHistoryItem::class.java, list) { bean ->
            itemEngraveDataEntity = bean
            initItemClickEvent()
        }
    }
}