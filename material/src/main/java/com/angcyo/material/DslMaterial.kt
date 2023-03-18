package com.angcyo.material

import android.content.Context
import androidx.annotation.IdRes
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.base.setInputHint
import com.angcyo.widget.base.setTextViewHintAction
import com.google.android.material.textfield.TextInputLayout

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/16
 */
object DslMaterial {

    /**初始化*/
    fun init(context: Context?) {
        setTextViewHintAction = { textView, hint ->
            //兼容
            val parent = textView.parent
            if (parent is TextInputLayout) {
                parent.hint = hint
            } else if (parent?.parent is TextInputLayout) {
                (parent.parent as TextInputLayout).hint = hint
            } else {
                textView.hint = hint
            }
        }
    }
}

/**[com.angcyo.widget.base.TextView.setInputHint]*/
fun DslViewHolder.setInputHint(@IdRes id: Int, hint: CharSequence?) {
    tv(id)?.setInputHint(hint)
}