package com.angcyo.acc2.app.ui

import android.os.Bundle
import com.angcyo.acc2.app.Acc2AppDslFragment
import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.http.UserHelper.exit
import com.angcyo.dsladapter.find
import com.angcyo.item.DslBottomButtonItem
import com.angcyo.library.ex.getCanUsedState
import com.angcyo.library.ex.isDebug
import com.angcyo.library.toastQQ

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/27
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

open class Acc2SettingFragment : Acc2AppDslFragment() {

    init {
        titleLayoutId = R.layout.lib_empty_item
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentConfig.fragmentBackgroundDrawable = null
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
        renderDslAdapter {
            DslBottomButtonItem()() {
                itemTag = "Button"
                itemHidden = true
                itemButtonText = "初始化失败, 点击重试"
                itemClick = {
                    fetchSetting()
                }
            }
        }
    }

    override fun onFragmentFirstShow(bundle: Bundle?) {
        super.onFragmentFirstShow(bundle)

        /*if (isDebug() && !isDebugType()) {
            HttpConfigDialog.showHttpConfig(fContext()) {
                //获取网络配置
                fetchSetting()
            }
        } else {
            //获取网络配置
            fetchSetting()
        }*/

        if (isDebug()) {
            //AccessibilityTouchLayer.show()
        }
    }

    open fun fetchSetting() {

    }

    fun _showButtonItem(error: Throwable) {
        _adapter.apply {
            find<DslBottomButtonItem>("Button", false)?.apply {
                itemHidden = false
                itemButtonText = "初始化失败, 点击重试\n${error.message}"
            }
            updateItemDepend()
        }
    }

    fun checkApp() {
        if (!isDebug() && app().memoryConfigBean.checkApp) {
            val state = getCanUsedState()
            if (state > 0) {
                toastQQ("此设备无法使用[$state]")
                //kill
                exit()
            }
        }
    }
}