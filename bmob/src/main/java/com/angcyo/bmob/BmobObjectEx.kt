package com.angcyo.bmob

import cn.bmob.v3.BmobBatch
import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.datatype.BatchResult
import cn.bmob.v3.exception.BmobException
import com.angcyo.library.L
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */

/**
 * 在数据库中插入一行数据
 * @return 返回数据 objectId
 * */
private fun BmobObject.save(
    onResult: (objectId: String?, error: BmobException?) -> Unit = { objectId, error ->
        L.d(objectId, error)
    }
): Disposable {
    return Disposables.empty()// this.save(SaveListenerHandler<String>(onResult))
}

/**更新[objectId]对应的数据*/
private fun BmobObject.update(
    objectId: String,
    onResult: (error: BmobException?) -> Unit = { error ->
        L.d(error)
    }
): Disposable {
    return Disposables.empty()//this.update(objectId, UpdateListenerHandler(onResult))
}

private fun BmobObject.delete(
    objectId: String,
    onResult: (error: BmobException?) -> Unit = { error ->
        L.d(error)
    }
): Disposable {
    return Disposables.empty()//this.delete(objectId, UpdateListenerHandler(onResult))
}

/**
 * 批量查询
 * http://doc.bmob.cn/data/android/develop_doc/#165
 * */
private fun <Data> bmobQuery(
    config: BmobQuery<Data>.() -> Unit = {},
    onResult: (results: List<Data>?, error: BmobException?) -> Unit = { results, error ->
        L.d(results, error)
    }
): Disposable {
    val bmobQuery: BmobQuery<Data> = BmobQuery()
    bmobQuery.config()
    return Disposables.empty()//bmobQuery.findObjects(FindListenerHandler(onResult))
}

/**查询[objectId]对应的数据*/
private fun <Data> query(
    objectId: String,
    onResult: (obj: Data?, error: BmobException?) -> Unit = { obj, error ->
        L.d(obj, error)
    }
) {
    //查找Person表里面id为6b6c11c537的数据
    val bmobQuery: BmobQuery<Data> = BmobQuery()

    //bmobQuery.getObject(objectId, QueryListenerHandler<Data>(onResult))
}

/**
 * 批量操作
 * http://doc.bmob.cn/data/android/develop_doc/#16
 *1. 批量操作每次只支持最大50条记录的操作。
 *2. 批量操作不支持对User表的操作。
 */
private fun bmobBatch(
    action: BmobBatch.() -> Unit = {},
    onResult: (results: List<BatchResult>?, error: BmobException?) -> Unit = { results, error ->
        L.d(results, error)
    }
) {
    val bmobBatch = BmobBatch()
    /*insertBatch	批量添加数据，并返回所添加数据的objectId字段
    updateBatch	批量更新数据
    deleteBatch	批量删除数据*/
    //bmobBatch.insertBatch()
    //bmobBatch.updateBatch()
    //bmobBatch.deleteBatch()
    bmobBatch.action()
    //bmobBatch.doBatch(QueryListListenerHandler(onResult))
}