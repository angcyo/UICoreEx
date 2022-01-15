package com.angcyo.github.finger.lifecycle

import java.util.*

/**
 * Created by Administrator on 2018\2\5 0005.
 */
class ActivityFragmentLifecycle : Lifecycle {

    private val lifecycleListeners =
        Collections.newSetFromMap(WeakHashMap<LifecycleListener, Boolean>())

    private var isStarted = false
    private var isDestroyed = false
    private var isResumed = false
    private var isPaused = false

    companion object {
        fun <T> getSnapshot(other: Collection<T>): List<T> {
            val result: MutableList<T> = ArrayList(other.size)
            for (item in other) {
                if (item != null) {
                    result.add(item)
                }
            }
            return result
        }
    }

    override fun addListener(listener: LifecycleListener) {
        lifecycleListeners.add(listener)
        when {
            isDestroyed -> {
                listener.onDestroy()
            }
            isStarted -> {
                listener.onStart()
            }
            isResumed -> {
                listener.onResume()
            }
            isPaused -> {
                listener.onPause()
            }
            else -> {
                listener.onStop()
            }
        }
    }

    override fun removeListener(listener: LifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    fun onPause() {
        isPaused = true
        isResumed = false
        for (lifecycleListener in getSnapshot(lifecycleListeners)) {
            lifecycleListener.onPause()
        }
    }

    fun onResume() {
        isResumed = true
        isPaused = false
        for (lifecycleListener in getSnapshot(lifecycleListeners)) {
            lifecycleListener.onResume()
        }
    }

    fun onStart() {
        isStarted = true
        for (lifecycleListener in getSnapshot(lifecycleListeners)) {
            lifecycleListener.onStart()
        }
    }

    fun onStop() {
        isStarted = false
        for (lifecycleListener in getSnapshot(lifecycleListeners)) {
            lifecycleListener.onStop()
        }
    }

    fun onDestroy() {
        isDestroyed = true
        for (lifecycleListener in getSnapshot(lifecycleListeners)) {
            lifecycleListener.onDestroy()
        }
    }
}