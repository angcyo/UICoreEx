package com.angcyo.canvas2.laser.pecker.manager

import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.core.vmApp
import com.angcyo.library.L
import com.angcyo.library.component.hawk.HawkPropertyValue
import com.angcyo.library.ex._string
import com.angcyo.library.ex.animationOf
import com.angcyo.library.ex.infinite
import com.angcyo.library.ex.inflate
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.isNoSize
import com.angcyo.library.ex.postDelay
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toDpi
import com.angcyo.library.getAppVersionCode
import com.angcyo.library.utils.RUtils
import com.angcyo.widget.base.clickIt
import com.angcyo.widget.base.screenRect
import com.angcyo.widget.layout.GuideFrameLayout
import com.angcyo.widget.span.span

/**
 * 新手指引管理
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/11/27
 */
object GuideManager {

    /**当前完成了引导的版本*/
    var guideVersion: Long by HawkPropertyValue<Any, Long>(0)

    val guideList = mutableListOf<GuideFrameLayout>()

    /**引导布局*/
    val guideLayoutList = mutableListOf(
        R.layout.layout_guide_clipart,
        R.layout.layout_guide_preview,
        R.layout.layout_guide_next,
        R.layout.layout_guide_send,
        R.layout.layout_guide_engrave,
        R.layout.layout_guide_finish
    )

    /**是否需要暂停当前的引导索引*/
    var pauseGuideIndex: Int = -1

    /**当前指引到了哪一步, 从1开始*/
    var guideIndex: Int = 0

    /**正在延迟等待的引导索引*/
    private var delayIndex: Int = -1

    /**检查或者显示引导*/
    fun checkOrShowGuide(container: ViewGroup?, anchor: View?, index: Int, delay: Long = 0) {
        if (container == null) {
            return
        }
        if (index - guideIndex != 1) {
            //不是下一步
            return
        }
        if (isDebugType()) {
            resetGuide()
        }
        if (guideVersion == getAppVersionCode()) {
            //已经完成了引导
            return
        }
        if (!vmApp<DeviceStateModel>().isDeviceConnect()) {
            if (delayIndex == index) {
                //已经在延迟
            } else if (delayIndex == -1) {
                delayIndex = index
                showGuide(container, anchor, index, delay)
            }
            return
        }
        if (anchor != null) {
            val screenRect = anchor.screenRect()
            if (screenRect.isNoSize()) {
                if (delayIndex == index) {
                    //已经在延迟
                } else if (delayIndex == -1) {
                    delayIndex = index
                    showGuide(container, anchor, index, delay)
                }
                return
            }
        }
        delayIndex = -1
        showGuide(container, anchor, index, delay)
    }

    private fun showGuide(container: ViewGroup, anchor: View?, index: Int, delay: Long = 0) {
        if (!vmApp<DeviceStateModel>().isDeviceConnect()) {
            //设备未连接
            anchor?.let {
                it.postDelay(160) {
                    showGuide(container, anchor, index, delay)
                }
            }
            return
        }
        if (anchor != null) {
            val screenRect = anchor.screenRect()
            if (screenRect.isNoSize() || pauseGuideIndex == index) {
                anchor.postDelay(160) {
                    showGuide(container, anchor, index, delay)
                }
                return
            }
            L.d("显示引导[$index]$screenRect")
        }
        guideIndex = index
        (anchor ?: container).postDelay(delay) {
            showGuideOf(container, anchor, index)
        }
    }

    /**从指定的控件显示一个指定的引导界面
     * [index] 从1开始
     * */
    private fun showGuideOf(container: ViewGroup, anchor: View?, index: Int) {
        delayIndex = -1
        val guidLayoutId = guideLayoutList.getOrNull(index - 1) ?: 0
        if (index == guideLayoutList.size()) {
            finishGuide()
        }
        if (guidLayoutId == 0) {
            return
        }
        val screenRect = anchor?.screenRect()
        if (anchor != null && screenRect != null) {
            if (screenRect.left < 0 || screenRect.right > RUtils.getDeviceWidth() ||
                screenRect.top < 0 || screenRect.bottom > RUtils.getDeviceHeight()
            ) {
                container.postDelay(160) {
                    showGuideOf(container, anchor, index)
                }
                return
            }
        }
        val rootView: GuideFrameLayout = container.inflate(guidLayoutId) as GuideFrameLayout
        rootView.onAnchorClick = {
            removeLastGuide()
        }
        screenRect?.let { rootView.addAnchor(it) }
        rootView.findViewById<View>(R.id.skip_guide_view)?.clickIt {
            backGuid()
        }
        rootView.findViewById<TextView>(R.id.lib_text_view)?.text = span {
            append("$index") {
                fontSize = 24.toDpi()
            }
            append("/${guideLayoutList.size()}")
            appendLine()
            append(
                when (index) {
                    1 -> _string(R.string.canvas_guid_clipart)
                    2 -> _string(R.string.canvas_guid_preview)
                    3 -> _string(R.string.canvas_guid_next)
                    4 -> _string(R.string.canvas_guid_send)
                    5 -> _string(R.string.canvas_guid_engrave)
                    else -> ""
                }
            )
        }
        //45°平移动画
        animationOf(id = R.anim.guide_cursor_animation)?.let {
            it.duration = 200
            it.infinite(ValueAnimator.REVERSE)
            rootView.findViewById<View>(R.id.guide_cursor_view)?.startAnimation(it)
        }
        guideList.add(rootView)
    }

    fun removeLastGuide(): Boolean {
        guideList.removeLastOrNull()?.let {
            it.removeIt()
            return true
        }
        return false
    }

    /**完成引导*/
    fun finishGuide() {
        guideVersion = getAppVersionCode()
    }

    /**重置引导*/
    fun resetGuide() {
        guideVersion = 0
    }

    /**移除最后一个引导, 如果成功返回true*/
    fun backGuid(): Boolean {
        finishGuide()
        return removeLastGuide()
    }

}