package com.angcyo.bmob

import cn.bmob.v3.BmobBatch
import cn.bmob.v3.BmobObject
import cn.bmob.v3.BmobQuery
import cn.bmob.v3.datatype.BatchResult
import com.angcyo.http.rx.BaseObserver
import com.angcyo.http.rx.observableToMain
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

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
    saveObservable()
        .compose(observableToMain())
        .subscribe(observer)
    return observer
}

/**更新指定[objectId]的对象, 似乎不会触发[onComplete]回调*/
fun BmobObject.update(
    objectId: String,
    action: BaseObserver<Exception>.() -> Unit = {}
): Disposable {
    val observer = BaseObserver<Exception>().apply(action)
    updateObservable(objectId)
        .compose(observableToMain())
        .subscribe(observer)
    return observer
}

/**如果已存在, 则更新. 否则新增*/
inline fun <reified T : BmobObject> T.updateOrSave(
    queryAction: BmobQuery<T>.() -> Unit,
    noinline action: BaseObserver<String?>.() -> Unit = {}
): Disposable {
    val bmob = this
    val query = BmobQuery<T>().apply(queryAction)
    val queryObserver = BaseObserver<String?>().apply(action)
    query.findObjectsObservable(T::class.java)
        .observeOn(Schedulers.io())
        .map { list ->
            val first = list.firstOrNull()
            if (first == null) {
                //save
                bmob.saveSync()
            } else {
                bmob.updateSync(first.objectId)
                first.objectId
            }
        }
        .compose(observableToMain())
        .subscribe(queryObserver)
    return queryObserver
}

/**查询bmob对象*/
inline fun <reified T : BmobObject> bmobQuery(
    queryAction: BmobQuery<T>.() -> Unit = {},
    action: BaseObserver<List<T>>.() -> Unit
): Disposable {
    val query = BmobQuery<T>().apply(queryAction)
    val observer = BaseObserver<List<T>>().apply(action)
    query.findObjectsObservable(T::class.java)
        .compose(observableToMain())
        .subscribe(observer)
    return observer
}

/**删除bmob对象*/
inline fun <reified T : BmobObject> bmobDelete(
    queryAction: BmobQuery<T>.() -> Unit = {},
    noinline action: BaseObserver<List<BatchResult>>.() -> Unit = {}
): Disposable {
    val query = BmobQuery<T>().apply(queryAction)
    val observer = BaseObserver<List<BatchResult>>().apply(action)
    query.findObjectsObservable(T::class.java)
        .observeOn(Schedulers.io())
        .flatMap { list ->
            val bmobBatch = BmobBatch()
            bmobBatch.deleteBatch(list as List<BmobObject>?)
            bmobBatch.doBatchObservable()
        }
        .compose(observableToMain())
        .subscribe(observer)
    return observer
}

//</editor-fold desc="bmob 3.7.9 版本二次封装">

//<editor-fold desc="低版本的bmob很难进行二次封装">

///**
// * 在数据库中插入一行数据
// * @return 返回数据 objectId
// * */
//private fun BmobObject.save(
//    onResult: (objectId: String?, error: Exception?) -> Unit = { objectId, error ->
//        L.d(objectId, error)
//    }
//): Disposable {
//    return Disposables.empty()// this.save(SaveListenerHandler<String>(onResult))
//}
//
///**更新[objectId]对应的数据*/
//private fun BmobObject.update(
//    objectId: String,
//    onResult: (error: Exception?) -> Unit = { error ->
//        L.d(error)
//    }
//): Disposable {
//    return Disposables.empty()//this.update(objectId, UpdateListenerHandler(onResult))
//}
//
//private fun BmobObject.delete(
//    objectId: String,
//    onResult: (error: Exception?) -> Unit = { error ->
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
//    onResult: (results: List<Data>?, error: Exception?) -> Unit = { results, error ->
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
//    onResult: (obj: Data?, error: Exception?) -> Unit = { obj, error ->
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
//    onResult: (results: List<BatchResult>?, error: Exception?) -> Unit = { results, error ->
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