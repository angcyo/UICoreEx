package com.angcyo.acc2.app

import android.os.Bundle
import android.view.MotionEvent
import com.angcyo.acc2.app.component.AccWindow
import com.angcyo.acc2.app.http.AccGitee
import com.angcyo.acc2.app.http.Message
import com.angcyo.acc2.app.http.bean.MessageBean
import com.angcyo.acc2.app.model.AdaptiveModel
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.acc2.app.ui.AccTaskTestFragment
import com.angcyo.base.dslFHelper
import com.angcyo.core.activity.BaseCoreAppCompatActivity
import com.angcyo.core.vmApp
import com.angcyo.dialog.normalIosDialog
import com.angcyo.download.version.versionUpdate
import com.angcyo.library.component.MultiFingeredHelper
import com.angcyo.library.ex.isDebug
import com.angcyo.viewmodel.observe

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/04
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class AccMainActivity : BaseCoreAppCompatActivity() {

    init {
        doubleBackTime = 1_000
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        /*dslFHelper {
            if (Acc2App.haveAllPermission(this@Acc2MainActivity)) {
                removeAll()
                restore(Acc2SettingFragment::class.java)
            } else {
                restore(Acc2PermissionFragment::class.java)
            }
        }*/

        /*dslFHelper {
            //restore(MainFragment::class.java)
            removeAll()
            restore(SettingFragment::class.java)
        }*/

        vmApp<GiteeModel>().messageData.observe(this) { bean ->
            if (bean != null) {
                when (bean.type) {
                    MessageBean.TYPE_DIALOG -> {
                        Message.saveReadMessage(bean)
                        normalIosDialog {
                            dialogTitle = bean.title
                            dialogMessage = bean.message
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (mainMemoryConfig().isOnlineData) {
            AccGitee.fetchVersion { data, error ->
                data.let {
                    versionUpdate(it)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //load
        vmApp<AdaptiveModel>().updateOnResume()

        //giteeVersionUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        AccWindow.hide()
    }

    /**4指捏合, 进入任务测试界面*/
    val pinchGestureDetector = MultiFingeredHelper.PinchGestureDetector().apply {
        onPinchAction = {
            dslFHelper {
                show(AccTaskTestFragment::class.java)
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val adaptiveModel = vmApp<AdaptiveModel>()
        if (isDebug() || adaptiveModel.isSelfDevice() ||
            adaptiveModel.isDebugDevice() ||
            adaptiveModel.isVip() ||
            adaptiveModel.isSuperAdmin()
        ) {
            pinchGestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }
}