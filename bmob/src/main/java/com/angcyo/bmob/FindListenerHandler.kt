package com.angcyo.bmob

import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.FindListener
import com.angcyo.library.L

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */
private class FindListenerHandler<Data>(
    val onResult: (results: List<Data>?, error: BmobException?) -> Unit = { results, error ->
        L.d(results, error)
    }
) : FindListener<Data>() {
    override fun done(results: List<Data>?, error: BmobException?) {
        onResult(results, error)
    }
}
