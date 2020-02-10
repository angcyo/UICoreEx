package com.angcyo.bmob

import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.UpdateListener
import com.angcyo.library.L

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */
private class UpdateListenerHandler(
    val onResult: (error: BmobException?) -> Unit = { error ->
        L.d(error)
    }
) : UpdateListener() {
    override fun done(error: BmobException?) {
        onResult(error)
    }
}