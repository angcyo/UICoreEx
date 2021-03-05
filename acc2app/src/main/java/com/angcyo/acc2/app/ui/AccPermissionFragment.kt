package com.angcyo.acc2.app.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.angcyo.acc2.app.AccApp
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.component.AccOpenTip
import com.angcyo.acc2.app.versionTipName
import com.angcyo.acc2.core.AccPermission
import com.angcyo.base.dslFHelper
import com.angcyo.core.fragment.BaseFragment
import com.angcyo.core.fragment.BaseTitleFragment
import com.angcyo.core.toAppPermissionsDetail
import com.angcyo.library.ex.*
import com.angcyo.widget.base.throttleClickIt
import com.angcyo.widget.span.span


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/04
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class AccPermissionFragment : BaseTitleFragment() {

    companion object {
        /**需要启动的目标界面*/
        var TARGET_CLASS: Class<out BaseFragment>? = null
    }

    init {
        fragmentLayoutId = R.layout.fragment_start
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        fragmentConfig.fragmentBackgroundDrawable = ColorDrawable(_color(R.color.bg_color))
        super.onCreate(savedInstanceState)
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        _vh.tv(R.id.top_text_view)?.text = _string(R.string.lib_accessibility_description)
        _vh.tv(R.id.lib_tip_view)?.text = versionTipName()

//        if (RUtils.getMIUIVersion() ?: 0 >= 12) {
        //_vh.visible(R.id.other_tip_view)
        _vh.tv(R.id.other_tip_view)?.apply {
            text = span {
                append("部分手机还需要打开以下权限:\n1:读取应用列表\n2:读取剪切板\n3:后台弹出界面\n")
                append("点击手动开启") {
                    foregroundColor = _color(R.color.colorAccent)
                }
            }
            throttleClickIt {
                fContext().toAppPermissionsDetail()
            }
        }
//        }

        _vh.gone(R.id.start_tip_view)
        _vh.throttleClick(R.id.start_tip_view) {
            /*dslAHelper {
                open(HELP_URL) {
                    title = "使用帮助"
                    showRightMenu = false
                }
            }*/
        }

        _vh.click(R.id.start_wrap_layout) {
            if (!AccPermission.haveDrawOverlays(fContext())) {
                AccPermission.openOverlaysActivity(fContext())
            } else if (!AccPermission.haveAccessibilityService(fContext())) {
                AccPermission.openAccessibilityActivity(fContext())
                AccOpenTip.show()
            }
        }

        if (isDebug()) {
            _vh.longClick(R.id.start_wrap_layout) {
                AccApp.jumpPermission = true
                dslFHelper {
                    remove(this@AccPermissionFragment)
                    restore(TARGET_CLASS!!)
                }
            }
        }
    }

    override fun onFragmentShow(bundle: Bundle?) {
        super.onFragmentShow(bundle)

        _vh.tv(R.id.text_view)?.text = span {
            if (!AccPermission.haveDrawOverlays(fContext())) {
                append("悬浮窗权限")
                append("[未开启]") {
                    foregroundColor = _color(R.color.warning)
                }
                appendln()
            }
            if (!AccPermission.haveAccessibilityService(fContext())) {
                append("无障碍权限")
                append("[未开启]") {
                    foregroundColor = _color(R.color.warning)
                }
                appendln()
            }

            append("开启") {
                foregroundColor = Color.WHITE
                fontSize = 66 * dpi
            }
        }
    }

    override fun canSwipeBack(): Boolean {
        return false
    }

    override fun onBackPressed(): Boolean {
        return false
    }
}
