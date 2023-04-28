package com.angcyo.github.finger.lifecycle

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment

/**
 * Created by Administrator on 2018\2\5 0005.
 */

@SuppressLint("ValidFragment")
class SupportFingerPrinterManagerFragment(val fragmentLifecycle: ActivityFragmentLifecycle = ActivityFragmentLifecycle()) :
    Fragment() {

    override fun onStart() {
        super.onStart()
        fragmentLifecycle.onStart()
    }

    override fun onResume() {
        super.onResume()
        fragmentLifecycle.onResume()
    }

    override fun onPause() {
        super.onPause()
        fragmentLifecycle.onPause()
    }

    override fun onStop() {
        super.onStop()
        fragmentLifecycle.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentLifecycle.onDestroy()
    }
}