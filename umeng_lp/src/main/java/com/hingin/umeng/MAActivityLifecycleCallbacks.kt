package com.hingin.umeng

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.umeng.analytics.MobclickAgent

/**
 * 页面监听上报
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/02/20
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
class MAActivityLifecycleCallbacks : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {
        MobclickAgent.onResume(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        MobclickAgent.onPause(activity)
    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {

    }
}