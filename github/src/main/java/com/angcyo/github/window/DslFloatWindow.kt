package com.angcyo.github.window

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.angcyo.library.L
import com.angcyo.library._screenHeight
import com.angcyo.library.ex.undefined_res
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.dslViewHolder
import com.angcyo.widget.base.setDslViewHolder
import com.yhao.floatwindow.*

/**
 * 浮窗
 * https://jitpack.io/com/github/XHGInc/FloatWindow
 * https://github.com/yhaolpz/FloatWindow
 * https://github.com/XHGInc/FloatWindow
 *
 * 其它库
 * https://github.com/princekin-f/EasyFloat
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/08
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class DslFloatWindow {

    companion object {

        const val DEFAULT_TAG = "default_float_window_tag"

        fun get(tag: String = DEFAULT_TAG): IFloatWindow? = FloatWindow.get(tag)

        //销毁
        fun destroy(tag: String = DEFAULT_TAG) {
            FloatWindow.destroy(tag)
        }

        fun show(tag: String = DEFAULT_TAG) {
            //手动控制
            get(tag)?.show()

//            //修改显示位置
//            get()?.updateX(100);
//            get()?.updateY(100);
//
//            destroy(tag)
        }

        fun hide(tag: String = DEFAULT_TAG) {
            //手动控制
            get(tag)?.hide()
        }
    }

    var floatLayoutId: Int = undefined_res

    /**悬浮窗宽高*/
    var floatWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    var floatHeight: Int = ViewGroup.LayoutParams.WRAP_CONTENT

    /**偏移*/
    var floatOffsetX = 0
    var floatOffsetY = (_screenHeight * 0.5f).toInt()

    /**在桌面是否显示*/
    var floatDesktopShow: Boolean = true

    /**浮动窗口tag*/
    var floatTag: String = DEFAULT_TAG

    /**布局初始化*/
    var initFloatLayout: (holder: DslViewHolder) -> Unit = {}

    var floatViewStateListener: ViewStateListener = object : ViewStateListenerAdapter() {
        override fun onBackToDesktop() {
            super.onBackToDesktop()
            L.d("Float Window ...")
        }

        override fun onMoveAnimStart() {
            super.onMoveAnimStart()
            L.d("Float Window ...")
        }

        override fun onMoveAnimEnd() {
            super.onMoveAnimEnd()
            L.d("Float Window ...")
        }

        override fun onPositionUpdate(x: Int, y: Int) {
            super.onPositionUpdate(x, y)
            L.d("Float Window ...x:$x y:$y")
        }

        override fun onDismiss() {
            super.onDismiss()
            L.d("Float Window ...")
        }

        override fun onShow() {
            super.onShow()
            L.d("Float Window ...")
        }

        override fun onHide() {
            super.onHide()
            L.d("Float Window ...")
        }
    }

    var floatPermissionListener: PermissionListener = object : PermissionListener {
        override fun onSuccess() {
            L.i("Float Window Permission Success!")
        }

        override fun onFail() {
            L.w("Float Window Permission Fail!")
        }
    }

    /**
     * 共提供 4 种 MoveType :
     * MoveType.slide : 可拖动，释放后自动贴边 （默认）
     * MoveType.back : 可拖动，释放后自动回到原位置
     * MoveType.active : 可拖动
     * MoveType.inactive : 不可拖动
     * */
    var floatMoveType = MoveType.slide

    /**贴边时的距离偏移*/
    var floatSlideLeftMargin = 0
    var floatSlideRightMargin = 0

    /**构建window配置回调*/
    var configFloatWindow: (FloatWindow.B) -> Unit = {}

    fun doIt(context: Context): IFloatWindow {

        val window: IFloatWindow? = get(floatTag)
        if (window != null) {
            //L.d("已经存在相同Tag的浮窗.")

            window.view?.apply {
                initFloatLayout(dslViewHolder())
            }

            return window
        }

        if (floatLayoutId == undefined_res) {
            throw IllegalStateException("请设置[floatLayoutId]")
        }

        val view: View =
            LayoutInflater.from(context).inflate(floatLayoutId, FrameLayout(context), false)

        val holder = DslViewHolder(view)
        view.setDslViewHolder(holder)

        initFloatLayout(holder)

        FloatWindow
            .with(context.applicationContext)
            .setTag(floatTag)
            .setView(view)
            .setWidth(floatWidth)                            //设置控件宽高
            .setHeight(floatHeight)
            .setX(floatOffsetX)                              //设置控件初始位置
            .setY(floatOffsetY)
            .setDesktopShow(floatDesktopShow)                //桌面显示
            .setViewStateListener(floatViewStateListener)    //监听悬浮控件状态改变
            .setPermissionListener(floatPermissionListener)  //监听权限申请结果
            .setMoveType(floatMoveType, floatSlideLeftMargin, floatSlideRightMargin)
            //.setMoveStyle()
            //.setFilter() //设置在哪些界面上要显示
            .apply {
                //slide
                configFloatWindow(this)
            }
            .build()

        return get(floatTag)!!
    }
}

fun Context.dslFloatWindow(action: DslFloatWindow.() -> Unit): IFloatWindow {
    return DslFloatWindow().run {
        action()
        doIt(this@dslFloatWindow)
    }
}

