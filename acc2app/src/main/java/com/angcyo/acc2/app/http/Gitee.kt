package com.angcyo.acc2.app.http

import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.component.jsonName
import com.angcyo.acc2.app.fillPks
import com.angcyo.acc2.app.http.bean.AdaptiveVersionBean
import com.angcyo.acc2.app.http.bean.FunctionBean
import com.angcyo.acc2.app.http.bean.MemoryConfigBean
import com.angcyo.acc2.app.memoryConfig
import com.angcyo.acc2.app.model.GiteeModel
import com.angcyo.acc2.bean.ActionBean
import com.angcyo.acc2.bean.CheckBean
import com.angcyo.acc2.bean.TaskBean
import com.angcyo.core.vmApp
import com.angcyo.download.version.VersionUpdateBean
import com.angcyo.http.base.bean
import com.angcyo.http.base.fromJson
import com.angcyo.http.base.listType
import com.angcyo.http.interceptor.LogInterceptor
import com.angcyo.http.rx.observer
import com.angcyo.http.toBean
import com.angcyo.library.L
import com.angcyo.library.ex.*
import com.google.gson.JsonElement
import io.reactivex.disposables.Disposable
import retrofit2.Response
import java.lang.reflect.Type

/**
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/01/26
 */
object Gitee {

    //无/结尾
    var BASE = ""

    var _last_fetch_time = 0L

    //首次拉取数据
    var isFirst = true

    /**[com.angcyo.core.CoreApplication.onCreate]*/
    fun init(online: Boolean = !isDebugType()) {
        fetch(online)
    }

    /**
     * ```
     * //获取最新数据
     * if (!isDebugType()) {
     *     Gitee.fetch()
     * }
     * ```
     * [force] 强制拉取
     * */
    fun fetch(online: Boolean = !isDebugType(), force: Boolean = isFirst) {
        if (BASE.isEmpty()) {
            throw IllegalArgumentException("请先配置[BASE]地址.")
        }

        val memoryConfigBean = memoryConfig()
        fetchMemoryConfig(online) { data, error ->
            if (error == null) {
                val newMemoryConfigBean = memoryConfig()

                var pass = false
                val nowTime = nowTime()
                if (online) {
                    if (nowTime - _last_fetch_time >= newMemoryConfigBean.fetchInterval * 1000) {
                        //需要拉取数据
                        isFirst = false
                        pass = false
                    } else {
                        pass = true
                    }
                }

                if (!pass) {
                    if (force || memoryConfigBean.version < newMemoryConfigBean.version) {
                        //需要更新数据
                        if (force || newMemoryConfigBean.updateFunction) {
                            fetchFunctionList(online)
                        }
                        if (force || newMemoryConfigBean.updateCheck) {
                            fetchAllCheck(online)
                        }
                        if (force || newMemoryConfigBean.updateAction) {
                            fetchAllAction(online)
                        }
                        if (force || newMemoryConfigBean.updateBackAction) {
                            fetchAllBackAction(online)
                        }
                        if (force || newMemoryConfigBean.updateTask) {
                            fetchAllTask(online)
                        }
                    }
                }
                _last_fetch_time = nowTime
            }
        }
    }

    //<editor-fold desc="base">

    /**从gitee获取数据*/
    fun get(
        json: String,
        end: (data: Response<JsonElement>?, error: Throwable?) -> Unit
    ): Disposable {
        val url = if (json.isHttpScheme()) {
            json
        } else {
            "$BASE/${json.jsonName()}"
        }
        return com.angcyo.http.get {
            this.url = url
            query = hashMapOf("time" to nowTime()) //带上时间参数, 避免缓存
            header = hashMapOf(
                LogInterceptor.closeLog(false),
                "Host" to (url.toUri()?.host ?: "gitee.com"),
                "Cache-Control" to "no-cache",
                "Cookie" to "gitee-session-n=MEl6NWFQd3RVMjhuL1pjWEJzWWE3MGpXMXFCcXMvRjdEWHl4ZXdQSVhVbFpQdTJtdVdWMVloYmYxWVQ4ZTkvVWFrRnl4WTdtazBpNTZpZkllUnFhTHc9PS0tMUdwM2JSM1R0VnRMNzhmamR6c081dz09--555bc1476152411c5402088a42058307da19dc8e; oschina_new_user=false",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.64"
            )
            isSuccessful = {
                it.isSuccessful
            }
        }.map {
            if (!it.isSuccessful) {
                throw IllegalArgumentException("${url}:${it.message()}")
            }
            it
        }.observer {
            onObserverEnd = { data, error ->
                end(data, error)
            }
        }
    }

    fun <T> assets(json: String, typeOfT: Type, end: (T) -> Unit) {
        val name = if (json.isHttpScheme()) {
            json.subEnd("/", true)!!
        } else {
            json
        }
        app().readAssets(name.jsonName())
            ?.fromJson<T>(typeOfT)
            ?.let {
                end(it)
            }
    }

    //</editor-fold desc="base">

    //<editor-fold desc="task">

    fun getCheck(json: String, end: (list: List<CheckBean>?, error: Throwable?) -> Unit) {
        get(json) { data, error ->
            val list = data?.toBean<List<CheckBean>>(listType(CheckBean::class.java))
            end(list, error)
        }
    }

    fun getAction(json: String, end: (list: List<ActionBean>?, error: Throwable?) -> Unit) {
        get(json) { data, error ->
            val list = data?.toBean<List<ActionBean>>(listType(ActionBean::class.java))
            end(list, error)
        }
    }

    fun getTask(json: String, end: (data: TaskBean?, error: Throwable?) -> Unit) {
        get(json) { data, error ->
            val bean = data?.toBean<TaskBean>(TaskBean::class.java)
            end(bean, error)
        }
    }

    /**获取所有[CheckBean]*/
    fun fetchAllCheck(online: Boolean) {
        val result = mutableListOf<CheckBean>()
        val list = memoryConfig().file?.check //?: mutableListOf("check_common")
        val giteeModel = vmApp<GiteeModel>()

        if (list.isNullOrEmpty()) {
            L.e("请先配置[check]json文件")
            giteeModel.allCheckData.value = null
            return
        }

        //back
        giteeModel.allCheckDataBack.resetAll(giteeModel.allCheckData.value)

        //保存一份Asset check
        if (giteeModel.allAssetCheckData.value.isNullOrEmpty() && !giteeModel.allCheckData.value.isNullOrEmpty()) {
            giteeModel.allAssetCheckData.value = giteeModel.allCheckData.value
        }

        if (online) {
            list.forEach {
                getCheck(it) { list, error ->
                    list?.let {
                        result.addAll(it)
                        giteeModel.allCheckData.value = result
                    }
                }
            }
        } else {
            list.forEach {
                if (it.isHttpScheme()) {
                    getCheck(it) { list, error ->
                        list?.let {
                            result.addAll(it)
                            giteeModel.allCheckData.value = result
                        }
                    }
                } else {
                    assets<List<CheckBean>>(it, listType(CheckBean::class.java)) {
                        result.addAll(it)
                        giteeModel.allCheckData.value = result
                    }
                }
            }
        }
    }

    /**获取所有[ActionBean]*/
    fun fetchAllAction(online: Boolean) {
        val result = mutableListOf<ActionBean>()
        val list = memoryConfig().file?.action //?: mutableListOf("all_actions")
        val giteeModel = vmApp<GiteeModel>()
        if (list.isNullOrEmpty()) {
            L.e("请先配置[action]json文件")
            giteeModel.allActionData.value = null
            return
        }

        giteeModel.allActionDataBack.resetAll(giteeModel.allActionData.value)

        if (online) {
            list.forEach {
                getAction(it) { list, error ->
                    list?.let {
                        result.addAll(it)
                        giteeModel.allActionData.value = result
                    }
                }
            }
        } else {
            list.forEach {
                if (it.isHttpScheme()) {
                    getAction(it) { list, error ->
                        list?.let {
                            result.addAll(it)
                            giteeModel.allActionData.value = result
                        }
                    }
                } else {
                    assets<List<ActionBean>>(it, listType(ActionBean::class.java)) {
                        result.addAll(it)
                        giteeModel.allActionData.value = result
                    }
                }
            }
        }
    }

    /**获取所有回退[ActionBean]*/
    fun fetchAllBackAction(online: Boolean) {
        val result = mutableListOf<ActionBean>()
        val list = memoryConfig().file?.backAction //?: mutableListOf("back_actions")
        val giteeModel = vmApp<GiteeModel>()
        if (list.isNullOrEmpty()) {
            L.e("请先配置[backAction]json文件")
            giteeModel.allBackActionData.value = null
            return
        }

        giteeModel.allBackActionDataBack.resetAll(giteeModel.allBackActionData.value)

        if (online) {
            list.forEach {
                getAction(it) { list, error ->
                    list?.let {
                        result.addAll(it)
                        giteeModel.allBackActionData.value = result
                    }
                }
            }
        } else {
            list.forEach {
                if (it.isHttpScheme()) {
                    getAction(it) { list, error ->
                        list?.let {
                            result.addAll(it)
                            giteeModel.allBackActionData.value = result
                        }
                    }
                } else {
                    assets<List<ActionBean>>(it, listType(ActionBean::class.java)) {
                        result.addAll(it)
                        giteeModel.allBackActionData.value = result
                    }
                }
            }
        }
    }

    /**获取所有[TaskBean]*/
    fun fetchAllTask(online: Boolean) {
        val result = mutableListOf<TaskBean>()
        val list = memoryConfig().file?.task
        val giteeModel = vmApp<GiteeModel>()
        if (list.isNullOrEmpty()) {
            L.e("请先配置[task]json文件")
            giteeModel.allTaskData.value = null
            return
        }
        if (online) {
            list.forEach {
                getTask(it) { data, error ->
                    data?.let {
                        result.add(it)
                        giteeModel.apply {
                            allTaskData.value = null
                            _addTasks(result)
                        }
                    }
                }
            }
        } else {
            list.forEach {
                if (it.isHttpScheme()) {
                    getTask(it) { data, error ->
                        data?.let {
                            result.add(it)
                            giteeModel.apply {
                                allTaskData.value = null
                                _addTasks(result)
                            }
                        }
                    }
                } else {
                    assets<TaskBean>(it, TaskBean::class.java) {
                        result.add(it)
                        giteeModel.apply {
                            allTaskData.value = null
                            _addTasks(result)
                        }
                    }
                }
            }
        }
    }

    //</editor-fold desc="task">

    //<editor-fold desc="fetch">

    /**拉取内存配置信息*/
    fun fetchMemoryConfig(
        online: Boolean = !isDebugType(),
        end: (data: MemoryConfigBean?, error: Throwable?) -> Unit = { _, _ -> }
    ) {
        val json = memoryConfig().file?.memoryConfig ?: "memory_config"

        if (json.isEmpty()) {
            L.e("请先配置[memoryConfig]j")
            return
        }

        if (online || json.isHttpScheme()) {
            get(json) { data, error ->
                data?.toBean(MemoryConfigBean::class.java)?.let {
                    it.isOnlineData = true
                    it.pks?.forEach { entry ->
                        entry.value.isOnlineData = true
                    }
                    app().memoryConfigBean = it.fillPks()
                    end(app().memoryConfigBean, error)
                }
                error?.let {
                    end(null, it)
                }
            }
        } else {
            assets<MemoryConfigBean>(json, MemoryConfigBean::class.java) {
                app().memoryConfigBean = it.fillPks()
                end(app().memoryConfigBean, null)
            }
        }
    }

    fun fetchAdaptiveConfig(
        online: Boolean = !isDebugType(),
        end: (data: AdaptiveVersionBean?, error: Throwable?) -> Unit = { _, _ -> }
    ) {
        val json = memoryConfig().file?.adaptive ?: "adaptive_version"

        if (json.isEmpty()) {
            L.e("请先配置[adaptive]")
            return
        }

        if (online || json.isHttpScheme()) {
            get(json) { data, error ->
                data?.toBean(AdaptiveVersionBean::class.java)?.let {
                    end(it, error)
                }
                error?.let {
                    end(null, it)
                }
            }
        } else {
            assets<AdaptiveVersionBean>(json, AdaptiveVersionBean::class.java) {
                end(it, null)
            }
        }
    }

    /**功能列表*/
    fun fetchFunctionList(
        online: Boolean = !isDebugType(),
        end: (list: List<FunctionBean>?, error: Throwable?) -> Unit = { _, _ -> }
    ) {
        val result = mutableListOf<FunctionBean>()
        val json = memoryConfig().file?.function //?: "function_list"

        if (json.isNullOrEmpty()) {
            end(null, IllegalArgumentException("请先配置[function]json文件"))
            return
        }

        if (online || json.isHttpScheme()) {
            get(json) { data, error ->
                data?.toBean<List<FunctionBean>>(listType(FunctionBean::class.java))?.let {
                    result.addAll(it)
                    vmApp<GiteeModel>().apply {
                        allFunctionData.value = result
                    }
                    end(it, error)
                }
                error?.let {
                    end(null, it)
                }
            }
        } else {
            assets<List<FunctionBean>>(json, listType(FunctionBean::class.java)) {
                result.addAll(it)
                vmApp<GiteeModel>().apply {
                    allFunctionData.postValue(result)
                }
                end(it, null)
            }
        }
    }

    var lastVersionUpdateBean: VersionUpdateBean? = null

    fun fetchVersion(end: (data: VersionUpdateBean?, error: Throwable?) -> Unit) {
        val json = memoryConfig().file?.version ?: "version"
        get(json) { data, error ->
            data?.toBean<VersionUpdateBean>(bean(VersionUpdateBean::class.java))?.let {
                lastVersionUpdateBean = it
                end(it, error)
            }
            error?.let {
                end(null, it)
            }
        }
    }

    //</editor-fold desc="fetch">

}