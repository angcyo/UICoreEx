package com.angcyo.bmob

import cn.bmob.v3.datatype.BatchResult
import cn.bmob.v3.exception.BmobException
import cn.bmob.v3.listener.QueryListListener
import com.angcyo.library.L

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */
private class QueryListListenerHandler(
    val onResult: (results: List<BatchResult>?, error: BmobException?) -> Unit = { results, error ->
        L.d(results, error)
    }
) : QueryListListener<BatchResult>() {
    override fun done(results: List<BatchResult>?, error: BmobException?) {
        onResult(results, error)
    }
}