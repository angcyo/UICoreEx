package com.angcyo.engrave.ble.history

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.engrave.*
import com.angcyo.engrave.ble.dslitem.EngraveTaskHistoryItem
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveTaskEntity_
import com.angcyo.objectbox.page

/**
 * App历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-1-5
 */
class EngraveAppHistoryFragment : BaseDslFragment() {

    init {
        enableRefresh = true
    }

    override fun onInitFragment(savedInstanceState: Bundle?) {
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(Color.WHITE)
        super.onInitFragment(savedInstanceState)
    }

    override fun onLoadData() {
        super.onLoadData()
        loadAppHistoryList()
    }

    /**加载App雕刻记录*/
    fun loadAppHistoryList() {
        val list = EngraveTaskEntity::class.page(page, LPBox.PACKAGE_NAME) {
            orderDesc(EngraveTaskEntity_.startTime)
        }
        loadDataEnd(list)
    }

    /**加载结束, 渲染界面*/
    fun loadDataEnd(list: List<EngraveTaskEntity>) {
        loadDataEnd(EngraveTaskHistoryItem::class.java, list) { bean ->
            itemEngraveTaskEntity = bean
        }
    }
}