package com.angcyo.viewmodel

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/16
 */

/**单独获取[Fragment]对应的[ViewModelProvider]*/
fun Fragment.of(factory: ViewModelProvider.Factory? = null) =
    ViewModelProviders.of(this, factory)

/**获取[Fragment]所在[FragmentActivity]中包含的[ViewModelProvider]*/
fun Fragment.ofa(factory: ViewModelProvider.Factory? = null) =
    ViewModelProviders.of(this.requireActivity(), factory)

/**单独获取[FragmentActivity]对应的[ViewModelProvider]*/
fun FragmentActivity.of(factory: ViewModelProvider.Factory? = null) =
    ViewModelProviders.of(this, factory)

/**获取[ViewModelStore]中的[ViewModel], 默认的[key]是[DEFAULT_KEY + ":" + modelClass.getCanonicalName()]*/
inline fun <reified VM : ViewModel> Fragment.vm(factory: ViewModelProvider.Factory? = null) =
    of(factory).get(VM::class.java)

inline fun <reified VM : ViewModel> Fragment.vma(factory: ViewModelProvider.Factory? = null) =
    ofa(factory).get(VM::class.java)

inline fun <reified VM : ViewModel> FragmentActivity.vm(factory: ViewModelProvider.Factory? = null) =
    of(factory).get(VM::class.java)