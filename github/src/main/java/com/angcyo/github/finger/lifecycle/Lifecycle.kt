package com.angcyo.github.finger.lifecycle

/**
 * Created by Administrator on 2018\2\5 0005.
 */
interface Lifecycle {
    /**
     * Adds the given listener to the set of listeners managed by this Lifecycle implementation.
     */
    fun addListener(listener: LifecycleListener)

    /**
     * Removes the given listener from the set of listeners managed by this Lifecycle implementation,
     * returning `true` if the listener was removed successfully, and `false` otherwise.
     *
     *
     * This is an optimization only, there is no guarantee that every added listener will
     * eventually be removed.
     */
    fun removeListener(listener: LifecycleListener)
}