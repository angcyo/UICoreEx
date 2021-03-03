package com.angcyo.acc2.app

import android.os.Bundle
import com.angcyo.acc2.app.component.AccWindow
import com.angcyo.acc2.app.http.Gitee
import com.angcyo.acc2.app.http.Message
import com.angcyo.acc2.app.model.AdaptiveModel
import com.angcyo.core.activity.BaseCoreAppCompatActivity
import com.angcyo.core.component.IObserver
import com.angcyo.core.component.VolumeObserver
import com.angcyo.core.vmApp
import com.angcyo.download.version.versionUpdate
import com.angcyo.library.ex.isDebug

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/04
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
open class Acc2MainActivity : BaseCoreAppCompatActivity() {

    val volumeObserver = object : IObserver {
        override fun onChange(type: Int, from: Int, value: Int) {
            if (isDebug()) {
                //AccessibilityWindow.onCatchAction?.invoke()
            }
        }
    }

    init {
        doubleBackTime = 1_000
    }

    override fun onCreateAfter(savedInstanceState: Bundle?) {
        super.onCreateAfter(savedInstanceState)

        VolumeObserver.init(this)
        VolumeObserver.observe(volumeObserver)

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
    }

    override fun onPostResume() {
        super.onPostResume()

        Message.fetchMessage()
        //load
        vmApp<AdaptiveModel>().updateOnResume()
        Gitee.fetchVersion { data, error ->
            data.let {
                versionUpdate(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AccWindow.hide()
        VolumeObserver.removeObserve(volumeObserver)
    }
}