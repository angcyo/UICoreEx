package com.angcyo.github.dslitem

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.library.app
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.dslitem.DslNestedRecyclerItem
import com.angcyo.widget.pager.DrawableIndicator
import com.leochuan.ScaleLayoutManager
import com.leochuan.ViewPagerLayoutManager

/**
 * 轮播图切换item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/18
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslBannerItem : DslNestedRecyclerItem() {

    init {
        itemLayoutId = R.layout.dsl_banner_item

        itemNestedLayoutManager = ScaleLayoutManager(app(), 0).apply {
            recycleChildrenOnDetach = true
            isFullItem = true
            minScale = 1f
            maxScale = 1f
            maxAlpha = 1f
            minAlpha = 1f
            itemSpace = 0
        }
    }

    val pagerLayoutManager: ViewPagerLayoutManager?
        get() = itemNestedLayoutManager as? ViewPagerLayoutManager

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        val drawableIndicator: DrawableIndicator? = itemHolder.v(R.id.lib_drawable_indicator)

        //page切换监听
        pagerLayoutManager?.setOnPageChangeListener(object :
            ViewPagerLayoutManager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                //L.v(state)
            }

            override fun onPageSelected(position: Int) {
                //L.v(position), 相当页面滑动也会通知.
                _scrollPositionConfig?.adapterPosition = position
                drawableIndicator?.animatorToIndex(position)
            }
        })

        //列表
        itemHolder.rv(R.id.lib_nested_recycler_view)?.apply {
            drawableIndicator?.indicatorCount = itemNestedAdapter.itemCount

            itemNestedAdapter.onDispatchUpdatesOnce {
                drawableIndicator?.indicatorCount = it.itemCount
            }

            if (itemKeepScrollPosition) {
                _scrollPositionConfig?.run { scrollToPosition(adapterPosition) }
            }
        }
    }
}