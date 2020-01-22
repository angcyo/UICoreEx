package com.angcyo.pager

import android.os.Bundle
import android.view.ViewGroup
import androidx.transition.TransitionSet
import com.angcyo.base.dslFHelper
import com.angcyo.fragment.AbsLifecycleFragment
import com.angcyo.transition.DslTransition

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/22
 */

open class ViewTransitionFragment : AbsLifecycleFragment() {

    /**过渡回调*/
    var transitionCallback: ViewTransitionCallback = ViewTransitionCallback()

    /**过渡执行协调*/
    val dslTransition = DslTransition()

    init {
        fragmentLayoutId = R.layout.lib_pager_transition_fragment
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        //防止事件穿透
        _vh.itemView.isClickable = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dslTransition.apply {
            sceneRoot = _vh.itemView as? ViewGroup

            //Capture
            onCaptureStartValues = {
                transitionCallback.onCaptureShowStartValues(_vh)
            }

            //Capture
            onCaptureEndValues = {
                transitionCallback.onCaptureShowEndValues(_vh)
            }

            //anim
            onSetTransition = {
                transitionCallback.onSetShowTransitionSet(_vh, TransitionSet())
            }

            onTransitionEnd = {
                onTransitionShowEnd()
            }

            //transition
            if (!transitionCallback.onStartShowTransition(this@ViewTransitionFragment, _vh)) {
                //不拦截, 执行默认的过渡动画
                transition()
            }
        }
    }

    /**显示过渡动画结束*/
    open fun onTransitionShowEnd() {

    }

    /**隐藏过渡动画结束*/
    open fun onTransitionHideEnd() {
        //真正移除界面
        dslFHelper {
            noAnim()
            remove(this@ViewTransitionFragment)
        }
    }

    //拦截默认的返回处理
    override fun onBackPressed(): Boolean {
        if (super.onBackPressed()) {
            backTransition()
        }
        return false
    }

    /**转场动画关闭界面*/
    open fun backTransition() {
        dslTransition.apply {
            //Capture
            onCaptureStartValues = {
                transitionCallback.onCaptureHideStartValues(_vh)
            }

            //Capture
            onCaptureEndValues = {
                transitionCallback.onCaptureHideEndValues(_vh)
            }

            //anim
            onSetTransition = {
                transitionCallback.onSetHideTransitionSet(_vh, TransitionSet())
            }

            onTransitionEnd = {
                onTransitionHideEnd()
            }

            //transition
            if (!transitionCallback.onStartHideTransition(this@ViewTransitionFragment, _vh)) {
                //不拦截, 执行默认的过渡动画
                transition()
            }
        }
    }
}