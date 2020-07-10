package com.angcyo.bmob

import cn.bmob.v3.BmobBatch
import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.datatype.BatchResult
import cn.bmob.v3.exception.BmobException
import com.angcyo.http.rx.BaseObserver
import com.angcyo.library.ex.elseNull
import io.reactivex.disposables.Disposable

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/07
 */

//<editor-fold desc="bmob 3.7.9 版本二次封装">

/**保存bmob对象*/
fun BmobObject.save(action: BaseObserver<String>.() -> Unit = {}): Disposable {
    val observer = BaseObserver<String>().apply(action)
    saveObservable().subscribe(observer)
    return observer
}

/**更新指定[objectId]的对象, 似乎不会触发[onComplete]回调*/
fun BmobObject.update(
    objectId: String,
    action: BaseObserver<BmobException>.() -> Unit = {}
): Disposable {
    val observer = BaseObserver<BmobException>().apply(action)
    updateObservable(objectId).subscribe(observer)
    return observer
}

/**如果已存在, 则更新. 否则新增*/
inline fun <reified T : BmobObject> T.updateOrSave(
    queryAction: BmobQuery<T>.() -> Unit,
    noinline result: (String?, Throwable?) -> Unit = { _, _ -> }
): Disposable {
    val bmob = this
    val query = BmobQuery<T>().apply(queryAction)
    val queryObserver = BaseObserver<List<T>>().apply {
        onObserverEnd = { data, _ ->
            data?.firstOrNull()?.let {
                bmob.update(it.objectId) {
                    onObserverEnd = { _, error ->
                        if (error == null) {
                            result(it.objectId, error)
                        } else {
                            result(null, error)
                        }
                    }
                }
            }.elseNull {
                bmob.save {
                    onObserverEnd = { data, error ->
                        if (error == null) {
                            result(data, error)
                        } else {
                            result(null, error)
                        }
                    }
                }
            }
        }
    }
    query.findObjectsObservable(T::class.java).subscribe(queryObserver)
    return queryObserver
}

/**查询bmob对象*/
inline fun <reified T : BmobObject> bmobQuery(
    queryAction: BmobQuery<T>.() -> Unit = {},
    action: BaseObserver<List<T>>.() -> Unit
): Disposable {
    val query = BmobQuery<T>().apply(queryAction)
    val observer = BaseObserver<List<T>>().apply(action)
    query.findObjectsObservable(T::class.java).subscribe(observer)
    return observer
}

/**删除bmob对象*/
inline fun <reified T : BmobObject> bmobDelete(
    queryAction: BmobQuery<T>.() -> Unit = {},
    noinline action: BaseObserver<List<BatchResult>>.() -> Unit = {}
): Disposable {
    val query = BmobQuery<T>().apply(queryAction)
    val observer = BaseObserver<List<BatchResult>>().apply(action)
    val queryObserver = BaseObserver<List<T>>().apply {
        onObserverEnd = { data, error ->
            data?.let {
                val bmobBatch = BmobBatch()
                bmobBatch.deleteBatch(it)
                bmobBatch.doBatchObservable().subscribe(observer)//似乎不会触发[onComplete]回调
            }
            error?.let {
                observer.onError(it)
            }
        }
    }
    query.findObjectsObservable(T::class.java).subscribe(queryObserver)
    return queryObserver
}

//</editor-fold desc="bmob 3.7.9 版本二次封装">

//<editor-fold desc="低版本的bmob很难进行二次封装">

///**
// * 在数据库中插入一行数据
// * @return 返回数据 objectId
// * */
//private fun BmobObject.save(
//    onResult: (objectId: String?, error: BmobException?) -> Unit = { objectId, error ->
//        L.d(objectId, error)
//    }
//): Disposable {
//    return Disposables.empty()// this.save(SaveListenerHandler<String>(onResult))
//}
//
///**更新[objectId]对应的数据*/
//private fun BmobObject.update(
//    objectId: String,
//    onResult: (error: BmobException?) -> Unit = { error ->
//        L.d(error)
//    }
//): Disposable {
//    return Disposables.empty()//this.update(objectId, UpdateListenerHandler(onResult))
//}
//
//private fun BmobObject.delete(
//    objectId: String,
//    onResult: (error: BmobException?) -> Unit = { error ->
//        L.d(error)
//    }
//): Disposable {
//    return Disposables.empty()//this.delete(objectId, UpdateListenerHandler(onResult))
//}
//
///**
// * 批量查询
// * http://doc.bmob.cn/data/android/develop_doc/#165
// * */
//private fun <Data> bmobQuery(
//    config: BmobQuery<Data>.() -> Unit = {},
//    onResult: (results: List<Data>?, error: BmobException?) -> Unit = { results, error ->
//        L.d(results, error)
//    }
//): Disposable {
//    val bmobQuery: BmobQuery<Data> = BmobQuery()
//    bmobQuery.config()
//    return Disposables.empty()//bmobQuery.findObjects(FindListenerHandler(onResult))
//}
//
///**查询[objectId]对应的数据*/
//private fun <Data> query(
//    objectId: String,
//    onResult: (obj: Data?, error: BmobException?) -> Unit = { obj, error ->
//        L.d(obj, error)
//    }
//) {
//    //查找Person表里面id为6b6c11c537的数据
//    val bmobQuery: BmobQuery<Data> = BmobQuery()
//
//    //bmobQuery.getObject(objectId, QueryListenerHandler<Data>(onResult))
//}
//
///**
// * 批量操作
// * http://doc.bmob.cn/data/android/develop_doc/#16
// *1. 批量操作每次只支持最大50条记录的操作。
// *2. 批量操作不支持对User表的操作。
// */
//private fun bmobBatch(
//    action: BmobBatch.() -> Unit = {},
//    onResult: (results: List<BatchResult>?, error: BmobException?) -> Unit = { results, error ->
//        L.d(results, error)
//    }
//) {
//    val bmobBatch = BmobBatch()
//    /*insertBatch	批量添加数据，并返回所添加数据的objectId字段
//    updateBatch	批量更新数据
//    deleteBatch	批量删除数据*/
//    //bmobBatch.insertBatch()
//    //bmobBatch.updateBatch()
//    //bmobBatch.deleteBatch()
//    bmobBatch.action()
//    //bmobBatch.doBatch(QueryListListenerHandler(onResult))
//}

//</editor-fold desc="低版本的bmob很难进行二次封装">