package com.angcyo.pager

import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.angcyo.base.dslFHelper

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/16
 */

/**[Fragment]中*/
fun Fragment.dslPager(imageView: ImageView?, url: String?) {
    dslFHelper {
        noAnim()
        show(ViewTransitionFragment().apply {
            transitionCallback = object : ViewTransitionCallback() {

            }
        })
    }
}