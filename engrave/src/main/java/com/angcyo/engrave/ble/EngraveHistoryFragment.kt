package com.angcyo.engrave.ble

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.engrave.R
import com.angcyo.engrave.ble.dslitem.EngraveHistoryItem
import com.angcyo.library.ex._string
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity
import com.angcyo.objectbox.laser.pecker.entity.EngraveHistoryEntity_
import com.angcyo.objectbox.laser.pecker.lpBoxOf
import com.angcyo.objectbox.page

/**
 * 历史文档界面
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/05
 */
class EngraveHistoryFragment : BaseDslFragment() {

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
    }

    override fun onLoadData() {
        super.onLoadData()

        lpBoxOf(EngraveHistoryEntity::class) {
            val list = page(page) {
                //降序排列
                orderDesc(EngraveHistoryEntity_.entityId)
            }
            loadDataEnd(EngraveHistoryItem::class.java, list) { bean ->
                engraveHistoryEntity = bean
            }
        }
    }

}