package com.angcyo.github.dslitem

import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.recycler.clearItemDecoration
import com.angcyo.widget.recycler.initDsl
import com.leochuan.ScaleLayoutManager
import com.leochuan.ViewPagerLayoutManager

/**
 * 轮播图切换item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/18
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslBannerItem : DslAdapterItem() {

    var itemBannerAdapter: DslAdapter = DslAdapter()

    var itemBannerLayoutManager: ViewPagerLayoutManager = ScaleLayoutManager(app(), 0).apply {
        minScale = 1f
        maxAlpha = 1f
        minAlpha = 1f
    }

    //记录当前滚动的页码
    var _itemPagePosition = RecyclerView.NO_POSITION

    init {
        itemLayoutId = R.layout.dsl_banner_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemBannerLayoutManager.setOnPageChangeListener(object :
            ViewPagerLayoutManager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                L.v(state)
            }

            override fun onPageSelected(position: Int) {
                L.v(position)
                _itemPagePosition = position
            }
        })

        itemHolder.rv(R.id.lib_recycler_view)?.apply {
            clearItemDecoration()
            initDsl()
            layoutManager = itemBannerLayoutManager
            adapter = itemBannerAdapter

            if (_itemPagePosition in 0 until itemBannerAdapter.itemCount) {
                scrollToPosition(_itemPagePosition)
            } else {
                _itemPagePosition = RecyclerView.NO_POSITION
            }
        }
    }
}