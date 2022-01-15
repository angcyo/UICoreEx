package com.angcyo.github.finger.lifecycle

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment

/**
 * Created by Administrator on 2018\2\5 0005.
 */

@SuppressLint("ValidFragment")
class SupportFingerPrinterManagerFragment(val lifecycle: ActivityFragmentLifecycle = ActivityFragmentLifecycle()) :
    Fragment() {

    override fun onStart() {
        super.onStart()
        lifecycle.onStart()
    }

    override fun onResume() {
        super.onResume()
        lifecycle.onResume()
    }

    override fun onPause() {
        super.onPause()
        lifecycle.onPause()
    }

    override fun onStop() {
        super.onStop()
        lifecycle.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.onDestroy()
    }
}