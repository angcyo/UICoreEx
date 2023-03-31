package com.angcyo.canvas2.laser.pecker.history

import com.angcyo.canvas2.laser.pecker.history.dslitem.EngraveTaskHistoryItem
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity_
import com.angcyo.objectbox.page

/**
 * App历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-1-5
 */
class EngraveAppHistoryFragment : BaseHistoryFragment() {

    /**加载App雕刻记录*/
    override fun loadHistoryList() {
        val list = EngraveTaskEntity::class.page(page, LPBox.PACKAGE_NAME) {
            orderDesc(EngraveTaskEntity_.startTime)
        }
        loadDataEnd(list)
    }

    /**加载结束, 渲染界面*/
    fun loadDataEnd(list: List<EngraveTaskEntity>) {
        loadDataEnd(EngraveTaskHistoryItem::class.java, list) { bean ->
            itemEngraveTaskEntity = bean
            initItemClickEvent()
        }
    }
}