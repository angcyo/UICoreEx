package com.angcyo.objectbox

import android.content.Context
import android.os.Build
import android.text.TextUtils
import androidx.collection.SimpleArrayMap
import com.angcyo.library.L
import com.angcyo.library.annotation.CallPoint
import com.angcyo.library.annotation.DSL
import com.angcyo.library.app
import com.angcyo.library.ex.*
import com.angcyo.library.model.Page
import com.angcyo.objectbox.DslBox.Companion.default_package_name
import com.angcyo.objectbox.DslBox.Companion.getBox
import com.angcyo.objectbox.DslBox.Companion.getBoxStore
import io.objectbox.*
import io.objectbox.android.Admin
import io.objectbox.exception.DbException
import io.objectbox.kotlin.equal
import io.objectbox.kotlin.query
import io.objectbox.query.QueryBuilder
import io.objectbox.query.QueryCondition
import java.io.File
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * Entity 有变化时, 请 执行 Make Project, 否则[Entity]可能不会生效
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/14
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class DslBox {

    companion object {

        private const val ERROR_FILE_NAME = "error.log"

        /**默认的数据库所在的文件夹名字*/
        var DEFAULT_FOLDER_NAME = "objectbox"

        /**将包名和对应的[BoxStore]保存起来.
         * 用来获取[BoxStore]*/
        val boxStoreMap = SimpleArrayMap<String, BoxStore>()

        /**默认的包名*/
        var default_package_name: String? = null

        /**调试特性*/
        var DEBUG_FLAGS = DebugFlags.LOG_QUERIES or
                DebugFlags.LOG_QUERY_PARAMETERS or
                DebugFlags.LOG_TRANSACTIONS_READ or
                DebugFlags.LOG_TRANSACTIONS_WRITE

        /**
         * [packageName] 数据库的包名. [MyObjectBox]所在的路径
         * [dbName] 数据库的名字 [io.objectbox.BoxStoreBuilder.DEFAULT_NAME]
         * */
        @CallPoint
        fun init(
            context: Context = app(),
            packageName: String? = default_package_name,
            dbName: String? = null,
            debug: Boolean = isDebug()
        ) {
            val _pName = packageName ?: context.packageName
            val myObjectBoxClass = Class.forName("$_pName.MyObjectBox")
            init(myObjectBoxClass, context, _pName, dbName, debug)
        }

        fun init(
            myObjectBoxClass: KClass<*>,
            context: Context = app(),
            packageName: String? = default_package_name,
            dbName: String? = null,
            debug: Boolean = isDebug()
        ) {
            init(myObjectBoxClass.java, context, packageName, dbName, debug)
        }

        fun init(
            myObjectBoxClass: Class<*>,
            context: Context = app(),
            packageName: String? = default_package_name,
            dbName: String? = null,
            debug: Boolean = isDebug()
        ) {

            //生成的[MyObjectBox]类, 所在的包名. [PlaceholderEntity]
            val _pName = packageName ?: context.packageName
            //数据库的名字
            val _dbName = checkDbNameWithError(context, dbName ?: BoxStoreBuilder.DEFAULT_NAME)

//            File initFile = new File(boxPath(context), dbName + "db.init");
//            if (initFile.exists()) {
//                //数据库初始化文件存在, 说明之前初始化失败了. 直接删除, 不知道为啥文件删除不掉. 所以只能重命名创建数据库.
//                BoxStore.deleteAllFiles(context, dbName);
//            }

            try {

                //获取[builder]方法
                val builderMethod = myObjectBoxClass.getDeclaredMethod("builder")

                if (default_package_name == null) {
                    default_package_name = _pName
                }

                val baseDirectory = File(boxPath(context))
                val dbDirectory = File(baseDirectory, _dbName)

                //默认路径:/data/user/0/包名/files/objectbox/objectbox 文件夹下
                //storeBuilder.baseDirectory()
                ///data/user/0/com.wayto.wxbic.plugin/files/objectbox

                val storeBuilder = builderMethod.invoke(null) as BoxStoreBuilder
                storeBuilder.androidContext(context.applicationContext)
                storeBuilder.baseDirectory(baseDirectory)
                storeBuilder.name(_dbName)

                if (debug) {
                    storeBuilder.debugFlags(DEBUG_FLAGS)
                    storeBuilder.debugRelations()
                }

                try {
                    val boxStore: BoxStore = buildStore(context, _pName, storeBuilder)
                    boxStoreMap.put(_pName, boxStore)
                    L.w("数据库:${_dbName} ${BoxStore.getVersionNative()} 路径:${_pName} -> ${dbDirectory.absolutePath}")
                } catch (e: DbException) {
                    //e.printStackTrace();
                    //io.objectbox.exception.DbException, 数据库初始化异常.一般是迁移导致的,改变了字段的数据类型
                    //storeBuilder.baseDirectory()
                    val errorFile = File(dbDirectory, ERROR_FILE_NAME)
                    val newFile: Boolean = errorFile.createNewFile()
                    if (newFile) {
                        e.saveTo(errorFile.absolutePath)
                    }

                    L.w("数据库初始化失败:$dbDirectory 即将重试. ${e.message}")

                    //换个数据库名字, 重新初始化
                    init(context, packageName, dbName, debug)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && e is ReflectiveOperationException) {
                    L.e("当前的路径下:$_pName, 未发现[MyObjectBox]类. 可以考虑在当前包名下新建[PlaceholderEntity]类")
                }
            }
        }

        /**
         * 检查数据之前是否有迁移异常
         *
         * @return 返回有效的数据库名
         */
        private fun checkDbNameWithError(context: Context, dbName: String): String {
            var result: String = dbName
            val boxFolder = File(boxPath(context))
            if (boxFolder.exists()) {
                //返回所有相同命名的数据库文件列表
                val files: Array<File>? = boxFolder.listFiles { pathName ->
                    pathName.isDirectory && pathName.name.startsWith(dbName)
                }
                if (!files.isNullOrEmpty()) {
                    files.sort()
                    val lastDbPath = files[files.size - 1]
                    val errorFile = File(lastDbPath, ERROR_FILE_NAME)
                    if (errorFile.exists()) {
                        //错误文件存在, 说明之前崩溃过
                        //String errorLog = FileExKt.readData(errorFile);
                        //L.v(errorLog);
                        result = dbName + (files.size + 1)
                        result = checkDbNameWithError(context, result)
                    } else {
                        result = lastDbPath.name
                    }
                }
            }
            return result
        }

        fun buildStore(
            context: Context,
            packageName: String,
            storeBuilder: BoxStoreBuilder
        ): BoxStore {
            return if (TextUtils.equals(context.packageName, packageName)) {
                storeBuilder.buildDefault()
            } else {
                storeBuilder.build()
            }
        }

        /** 删除数据库文件 */
        fun deleteBoxDb(context: Context, dbName: String? = BoxStoreBuilder.DEFAULT_NAME): Boolean {
            return boxDbPath(context, dbName).file().deleteRecursively()
        }

        /** 数据库的文件夹路径
         *  [/data/user/0/com.angcyo.uicore.demo/files/objectbox/LaserPecker/data.mdb]
         *  [/data/user/0/com.angcyo.uicore.demo/files/objectbox/LaserPecker/lock.mdb]
         *
         *  [/data/user/0/com.angcyo.uicore.demo/files/objectbox/objectbox/data.mdb]
         *  [/data/user/0/com.angcyo.uicore.demo/files/objectbox/objectbox/lock.mdb]
         * */
        fun boxDbPath(context: Context, dbName: String?): String {
            return boxPath(context) + File.separator + dbName
        }

        /**获取数据库默认路径文件夹
         * [/data/user/0/com.angcyo.uicore.demo/files/]
         * [/storage/emulated/0/Android/data/com.angcyo.uicore.demo/files/]
         * [android.content.Context.getFilesDir]
         * [android.content.Context.getExternalFilesDir]
         * */
        fun boxPath(context: Context): String {
            return context.filesDir.absolutePath + File.separator + DEFAULT_FOLDER_NAME
            //return libFilePath(DEFAULT_FOLDER_NAME, context)
        }

        /**清理缓存*/
        fun clear() {
            BoxStore.clearDefaultStore()
            boxStoreMap.clear()
        }

        /**获取BoxStore*/
        fun getBoxStore(packageName: String): BoxStore {
            return boxStoreMap.get(packageName)
                ?: throw NullPointerException("$packageName 未初始化[ObjectBox], 请先调用[DslBox.init()]")
        }

        //java.lang.IllegalArgumentException: class com.angcyo.objectbox.laser.pecker.entity.CommandEntity
        // is not a known entity. Please add it and trigger generation again.
        fun <T> getBox(packageName: String, entityClass: Class<T>): Box<T> {
            return getBoxStore(packageName).boxFor(entityClass)
        }

        /**启动所有数据服务*/
        fun startObjectBrowserAll() {
            boxStoreMap.each { key, boxStore ->
                boxStore?.let {
                    startObjectBrowser(it)
                }
            }
        }

        /**启动数据浏览服务*/
        fun startObjectBrowser(boxStore: BoxStore, context: Context = app()) {
            val started = Admin(boxStore).start(context)
            //val started: Boolean = AndroidObjectBrowser(boxStore).start(app())
            L._tempTag = "ObjectBrowser"
            if (started) {
                L.i("数据库浏览服务启动成功: http://${getWifiIP() ?: "localhost"}:${boxStore.objectBrowserPort}/index.html")
            } else {
                L.w("数据库浏览服务启动失败, 请检查是否使用了[objectbox-android-objectbrowser]")
            }
        }
    }
}

fun defaultBoxStore() = default_package_name ?: BuildConfig.LIBRARY_PACKAGE_NAME

//region ---Box BoxStore---

/**快速获取[BoxStore]*/
fun boxStoreOf(
    packageName: String = defaultBoxStore(),
    action: BoxStore.() -> Unit = {}
): BoxStore {
    val boxStore = getBoxStore(packageName)
    boxStore.action()
    return boxStore
}

/**
 * 快速获取[Box]
 * boxOf(MessageEntity::class.java) {
 *   val list = query().build().find()
 *   val list2 = query().equal(MessageEntity_.isSkip, false).build().find()
 *   val list3 = query().equal(MessageEntity_.isSkip, false).order(MessageEntity_.timestamp).build().find(10, 20)
 *
 *   query(FriendEntity_.state.equal("已是好友").or(FriendEntity_.state.equal("已发送申请")))
 *   query(FriendEntity_.state.equal("已发送申请"))
 * }
 * */
fun <T> boxOf(
    entityClass: Class<T>,
    packageName: String = defaultBoxStore(),
    action: Box<T>.() -> Unit = {}
): Box<T> {
    val box = getBox(packageName, entityClass)
    box.action()
    return box
}

fun <T : Any> boxOf(
    entityClass: KClass<T>,
    packageName: String = defaultBoxStore(),
    action: Box<T>.() -> Unit = {}
): Box<T> {
    return boxOf(entityClass.java, packageName, action)
}

inline fun <reified T : Any> KClass<T>.box(
    packageName: String = defaultBoxStore(),
    action: Box<T>.() -> Unit = {}
): Box<T> {
    val cls = this.java
    val box = boxOf(cls, packageName)
    box.action()
    return box
}

//endregion ---Box BoxStore---

//region ---query find---

/**返回所有记录的数量*/
inline fun <reified T : Any> KClass<T>.count(packageName: String = defaultBoxStore()): Long {
    val cls = this.java
    return boxOf(cls, packageName).count()
}

/**返回查询的数量*/
inline fun <reified T : Any> KClass<T>.countBy(
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): Long {
    val cls = this.java
    return boxOf(cls, packageName).query(block).count()
}

/**获取所有记录
 * [KClass.page] 分页获取
 * */
fun <T> Box<T>.findAll(block: QueryBuilder<T>.() -> Unit = {}): List<T> {
    return query(block).find()
}

/**
 * [QueryBuilder.StringOrder.CASE_SENSITIVE] //区分大小写
 * [QueryBuilder.StringOrder.CASE_INSENSITIVE] //不区分大小写
 * */
fun <T> QueryBuilder<T>.equalString(
    property: Property<T>,
    value: Any?,
    order: QueryBuilder.StringOrder = QueryBuilder.StringOrder.CASE_INSENSITIVE
): QueryBuilder<T> = equal(property, "${value ?: ""}", order)

/**
 * box.query(User_.name.equal("Jane") and (User_.age.less(12) or User_.status.equal("child")))
 * [io.objectbox.query.QueryBuilder.apply] //新方式
 * [io.objectbox.query.QueryBuilder.equal] //旧方式
 * */
fun <T> Box<T>.findAll(
    queryCondition: QueryCondition<T>? = null,
    block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val builder = if (queryCondition == null) query() else query(queryCondition)
    return builder.apply(block).build().find()
}

/**获取所有记录*/
inline fun <reified T : Any> KClass<T>.findAll(
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val cls = this.java
    return boxOf(cls, packageName).findAll(null, block)
}

/**查找第一个*/
inline fun <reified T : Any> KClass<T>.findFirst(
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): T? {
    val cls = this.java
    val box = boxOf(cls, packageName)
    return box.findFirst(block)
}

/**获取第一条记录*/
fun <T> Box<T>.findFirst(block: QueryBuilder<T>.() -> Unit = {}): T? {
    return query(block).findFirst()
}

fun <T> Box<T>.findFirst(
    queryCondition: QueryCondition<T>? = null,
    block: QueryBuilder<T>.() -> Unit = {}
): T? {
    val builder = if (queryCondition == null) query() else query(queryCondition)
    return builder.apply(block).build().findFirst()
}

/**获取最后[limit]条记录*/
fun <T> Box<T>.findLastList(limit: Long = 1, block: QueryBuilder<T>.() -> Unit = {}): List<T> {
    val count = query(block).count()
    if (count <= 0) {
        //Invalid offset (-1): must be zero or positive
        return emptyList()
    }
    return query(block).find(max(count - limit, 0), limit)
}

fun <T> Box<T>.findLastList(
    limit: Long = 1,
    queryCondition: QueryCondition<T>? = null,
    block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val countBuilder = if (queryCondition == null) query() else query(queryCondition)
    val count = countBuilder.apply(block).build().count() //先查数量
    if (count <= 0) {
        //Invalid offset (-1): must be zero or positive
        return emptyList()
    }
    val builder = if (queryCondition == null) query() else query(queryCondition)
    return builder.apply(block).build().find(max(count - limit, 0), limit)
}

/**查找最后一个元素
 * ```
 * apply(
 *          EngraveConfigEntity_.taskId.equal("$taskId")
 *              .and(EngraveConfigEntity_.layerId.equal(engraveLayerInfo?.layerId ?: ""))
 *      )
 * ```
 * */
inline fun <reified T : Any> KClass<T>.findLast(
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): T? {
    val cls = this.java
    val box = boxOf(cls, packageName)
    return box.findLastList(1, null, block).lastOrNull()
}

inline fun <reified T : Any> KClass<T>.findLastList(
    limit: Long = 1,
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val cls = this.java
    val box = boxOf(cls, packageName)
    return box.findLastList(limit, null, block)
}

/**获取所有记录*/
inline fun <reified T : Any> KClass<T>.allEntity(packageName: String = defaultBoxStore()): List<T> {
    val cls = this.java
    return boxOf(cls, packageName).all
}

/**分页查找
 * page(page) {
 *    //降序排列
 *    orderDesc(EngraveHistoryEntity_.entityId)
 * }
 * */
fun <T> Box<T>.page(
    page: Page,
    queryCondition: QueryCondition<T>? = null,
    block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val offset = (page.requestPageIndex - page.firstPageIndex) * page.requestPageSize
    val limit = page.requestPageSize
    val builder = if (queryCondition == null) query() else query(queryCondition)
    return builder.apply(block).build().find(offset.toLong(), limit.toLong())
}

inline fun <reified T : Any> KClass<T>.page(
    page: Page,
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val cls = this.java
    return boxOf(cls, packageName).page(page, null, block)
}

//endregion ---query find---

//region ---remove---

/**删除所有*/
fun <T> Box<T>.removeAll(block: QueryBuilder<T>.() -> Unit = {}): List<T> {
    return findAll(block).apply { this@removeAll.remove(this) }
}

fun <T> Box<T>.removeFirst(block: QueryBuilder<T>.() -> Unit = {}): T? {
    return findFirst(block)?.apply { this@removeFirst.remove(this) }
}

/**删除一条记录*/
inline fun <reified T> T.deleteEntity(packageName: String = defaultBoxStore()): Boolean {
    return boxOf(T::class.java, packageName).remove(this)
}

/**[deleteEntity]*/
inline fun <reified T> T.removeEntity(packageName: String = defaultBoxStore()) =
    deleteEntity(packageName)

/**删除所有*/
inline fun <reified T> Collection<T>.deleteAllEntity(packageName: String = defaultBoxStore()) {
    return boxOf(T::class.java, packageName).remove(this)
}

/**[deleteAllEntity]*/
inline fun <reified T> Collection<T>.removeAllEntity(packageName: String = defaultBoxStore()) =
    deleteAllEntity(packageName)

/**获取所有记录*/
inline fun <reified T : Any> KClass<T>.removeAll(
    packageName: String = defaultBoxStore(),
    noinline block: QueryBuilder<T>.() -> Unit = {}
): List<T> {
    val cls = this.java
    val boxOf = boxOf(cls, packageName)
    return boxOf.removeAll(block)
}

//endregion ---remove---

//region ---save---

/**保存实体, 保存成功之后, [entityId]会自动赋值
 * id不为0时, 就是更新
 * 返回Entity的id*/
inline fun <reified T> T.saveEntity(packageName: String = defaultBoxStore()): Long {
    if (this == null) {
        return -1
    }
    return boxOf(T::class.java, packageName).put(this)
}

/**保存实例*/
@DSL
inline fun <reified T : Any> KClass<T>.saveEntity(
    packageName: String = defaultBoxStore(),
    update: T.() -> Unit
): Long {
    val cls = this.java
    val box = boxOf(cls, packageName)
    val entity = cls.newInstance()
    entity.update()
    return box.put(entity)
}

/**查询一个实体, 如果不存在, 则创建一个实体
 * [init] 创建实体时的初始化回调
 * [query] 查询条件
 * */
@DSL
inline fun <reified T : Any> KClass<T>.queryOrCreateEntity(
    packageName: String = defaultBoxStore(),
    init: T.() -> Unit = {},
    noinline query: QueryBuilder<T>.() -> Unit,
): T {
    val cls = this.java
    val box = boxOf(cls, packageName)
    val find = box.query(query).findFirst()
    if (find == null) {
        val entity = cls.newInstance()
        entity.init()
        box.put(entity)
        return entity
    }
    return find
}

/**保证一定得到一个实体, 并且执行初始化方法*/
@DSL
inline fun <reified T : Any> KClass<T>.ensureEntity(
    packageName: String = defaultBoxStore(),
    noinline query: QueryBuilder<T>.() -> Unit,
    block: T.() -> Unit = {},
): T {
    val cls = this.java
    val box = boxOf(cls, packageName)
    val find = box.query(query).findFirst()
    if (find == null) {
        val entity = cls.newInstance()
        entity.block()
        box.put(entity)
        return entity
    } else {
        find.block()
        find.saveEntity(packageName)
    }
    return find
}

/**批量保存或者更新
 * id不为0时, 就是更新*/
inline fun <reified T> Collection<T>.saveAllEntity(packageName: String = defaultBoxStore()) {
    boxOf(T::class.java, packageName).put(this)
}

/**先通过查询条件查找满足条件的实体, 如果不存在则创建新的*/
inline fun <reified T : Any> KClass<T>.updateOrCreateEntity(
    packageName: String = defaultBoxStore(),
    noinline query: QueryBuilder<T>.() -> Unit,
    update: T.() -> Unit
): Long {
    val cls = this.java
    val box = boxOf(cls, packageName)
    val entity = box.query(query).findFirst() ?: cls.newInstance()
    entity.update()
    return box.put(entity)
}

//endregion ---save---

/**
 * [io.objectbox.android.ObjectBoxLiveData] 使用
 * val operateData: ObjectBoxLiveData<OperateEntity> = ObjectBoxLiveData(boxOf(OperateEntity::class.java).query().build())
 * */