package com.angcyo.pager

import android.graphics.Color
import androidx.transition.TransitionSet
import com.angcyo.transition.ColorTransition
import com.angcyo.widget.DslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/22
 */

open class ViewTransitionCallback {

    //<editor-fold desc="show过渡">

    /**界面显示时, 动画开始的值设置*/
    open fun onCaptureShowStartValues(viewHolder: DslViewHolder) {
        //背景颜色动画
        viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
    }

    /**界面显示时, 动画结束后的值设置*/
    open fun onCaptureShowEndValues(viewHolder: DslViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.BLACK)
    }

    /**开始show的转场动画, 返回true, 拦截过渡*/
    open fun onStartShowTransition(
        fragment: ViewTransitionFragment,
        viewHolder: DslViewHolder
    ): Boolean {
        return false
    }

    open fun onSetShowTransitionSet(
        viewHolder: DslViewHolder,
        transitionSet: TransitionSet
    ): TransitionSet {
        transitionSet.apply {
            addTransition(ColorTransition().addTarget(viewHolder.itemView))
//            addTransition(Fade(Fade.OUT))
//            addTransition(ChangeBounds())
//            addTransition(ChangeTransform())
//            addTransition(ChangeClipBounds())
//            addTransition(ChangeImageTransform())
//            addTransition(Fade(Fade.IN))
        }
        return transitionSet
    }

    //</editor-fold desc="show过渡">

    //<editor-fold desc="hide过渡">

    /**界面关闭, 动画开始时的值(通过可以不设置此处)*/
    open fun onCaptureHideStartValues(viewHolder: DslViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.BLACK)
    }

    /**界面关闭, 动画需要结束的值*/
    open fun onCaptureHideEndValues(viewHolder: DslViewHolder) {
        viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT)
    }

    /**开始hide的转场动画, 返回true, 拦截过渡*/
    open fun onStartHideTransition(
        fragment: ViewTransitionFragment,
        viewHolder: DslViewHolder
    ): Boolean {
        return false
    }

    open fun onSetHideTransitionSet(
        viewHolder: DslViewHolder,
        transitionSet: TransitionSet
    ): TransitionSet {
        return onSetShowTransitionSet(viewHolder, transitionSet)
    }
    //</editor-fold desc="hide过渡">

}
