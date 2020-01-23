package com.angcyo.pager

import android.os.Bundle
import android.view.ViewGroup
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.loader.loadPath
import com.angcyo.pager.dslitem.DslPhotoViewItem
import com.angcyo.widget._vp
import com.angcyo.widget.pager.DslPagerAdapter
import com.angcyo.widget.pager.getPrimaryViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/22
 */

open class PagerTransitionFragment : ViewTransitionFragment() {

    val pagerTransitionCallback get() = transitionCallback as PagerTransitionCallback

    init {
        fragmentLayoutId = R.layout.lib_pager_transition_fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (transitionCallback !is PagerTransitionCallback) {
            throw IllegalArgumentException("transitionCallback not PagerTransitionCallback.")
        }
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        _vh._vp(R.id.lib_view_pager)?.apply {
            val items = mutableListOf<DslAdapterItem>()

            pagerTransitionCallback.loaderMedia.forEach {
                items.add(DslPhotoViewItem().apply {
                    itemData = it
                    imageUrl = it.loadPath()
                })
            }

            adapter = DslPagerAdapter(items)

            setCurrentItem(pagerTransitionCallback.startPosition, false)

            //在后面添加事件, 那么第一次就不会触发[onPageSelected]
            addOnPageChangeListener(pagerTransitionCallback)
        }
    }

    override fun onTransitionShowStart() {
        super.onTransitionShowStart()
    }

    override fun onTransitionShowEnd() {
        super.onTransitionShowEnd()
    }

    override fun startTransition(start: Boolean) {
        dslTransition.apply {
            sceneRoot = _vh.itemView as? ViewGroup

            transitionCallback.sceneRoot = sceneRoot
            val vh = _vh._vp(R.id.lib_view_pager)?.getPrimaryViewHolder() ?: _vh
            _configTransition(start, vh)
        }
    }
}