package com.angcyo.canvas.laser.pecker.mode

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.angcyo.base.dslAHelper
import com.angcyo.canvas.data.CanvasOpenDataType
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.ex.*
import com.angcyo.viewmodel.vmDataOnce

/**
 * 用来实现[Canvas]的打开文件和导入文件数据转发
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/15
 */
class CanvasOpenModel : ViewModel() {

    companion object {

        /**需要打开处理的[Activity]*/
        var OPEN_ACTIVITY_CLASS: Class<out Activity>? = null

        /** [OPEN_ACTIVITY_CLASS]内部的[Fragment], 实际上只需要[OPEN_ACTIVITY_CLASS]就可以完成处理*/
        var OPEN_ACTIVITY_FRAGMENT_CLASS: Class<out Fragment>? = null

        /**启动主页*/
        fun openCanvasActivity(context: Context) {
            context.dslAHelper {
                if (OPEN_ACTIVITY_FRAGMENT_CLASS == null) {
                    //只有Activity
                    start(OPEN_ACTIVITY_CLASS!!)
                } else {
                    //有Activity又有Fragment
                    startFragment(
                        OPEN_ACTIVITY_CLASS!!,
                        OPEN_ACTIVITY_FRAGMENT_CLASS!!
                    )
                }
            }
        }
    }

    /**需要打开的数据
     * 支持[com.angcyo.canvas.data.CanvasProjectItemBean]
     * 支持[com.angcyo.canvas.data.CanvasProjectBean]
     * */
    val openPendingData = vmDataOnce<CanvasOpenDataType?>()

    /**使用创作打开一个图片/GCode/SVG*/
    @CallPoint
    fun open(context: Context, bean: CanvasOpenDataType?): Boolean {
        bean ?: return false
        openPendingData.postValue(bean)
        return if (openPendingData.hasObservers()) {
            //有监听者, 需要弹出界面
            context.openApp()
            true
        } else if (OPEN_ACTIVITY_CLASS != null) {
            //无监听者
            openCanvasActivity(context)
            true
        } else {
            false
        }
    }
}