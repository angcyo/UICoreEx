package com.angcyo.github.dslitem

import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.github.R
import com.angcyo.item.DslNestedRecyclerItem
import com.angcyo.library.app
import com.angcyo.widget.DslViewHolder
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

        nestedRecyclerItemConfig.itemNestedLayoutManager = ScaleLayoutManager(app(), 0).apply {
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
        get() = nestedRecyclerItemConfig.itemNestedLayoutManager as? ViewPagerLayoutManager

    override fun onBindNestedRecyclerView(
        recyclerView: RecyclerView,
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onBindNestedRecyclerView(
            recyclerView,
            itemHolder,
            itemPosition,
            adapterItem,
            payloads
        )

        val drawableIndicator: DrawableIndicator? = itemHolder.v(R.id.lib_drawable_indicator)

        //page切换监听
        pagerLayoutManager?.setOnPageChangeListener(object :
            ViewPagerLayoutManager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                //L.v(state)
            }

            override fun onPageSelected(position: Int) {
                //L.v(position), 相当页面滑动也会通知.
                nestedRecyclerItemConfig._scrollPositionConfig?.adapterPosition = position
                drawableIndicator?.animatorToIndex(position)
            }
        })

        //列表
        recyclerView.apply {
            drawableIndicator?.indicatorCount = nestedRecyclerItemConfig.itemNestedAdapter.itemCount

            nestedRecyclerItemConfig.itemNestedAdapter.onDispatchUpdatesOnce {
                drawableIndicator?.indicatorCount = it.itemCount
            }

            if (nestedRecyclerItemConfig.itemKeepScrollPosition) {
                nestedRecyclerItemConfig._scrollPositionConfig?.run {
                    scrollToPosition(
                        adapterPosition
                    )
                }
            }
        }
    }
}