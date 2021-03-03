package com.angcyo.acc2.app.http

import com.angcyo.acc2.app.app
import com.angcyo.acc2.app.component.jsonName
import com.angcyo.acc2.app.http.bean.FunctionBean
import com.angcyo.acc2.app.http.bean.MemoryConfigBean
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
import com.angcyo.library.ex.isDebugType
import com.angcyo.library.ex.isHttpScheme
import com.angcyo.library.ex.nowTime
import com.angcyo.library.ex.readAssets
import com.google.gson.JsonElement
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

    fun init(online: Boolean = !isDebugType()) {
        fetch(online)
    }

    fun fetch(online: Boolean = true) {
        if (online && BASE.isEmpty()) {
            L.e("请先配置[BASE]地址.")
            return
        }
        fetchMemoryConfig(online) { data, error ->
            if (error == null) {
                fetchFunctionList(online)
                fetchAllCheck(online)
                fetchAllAction(online)
                fetchAllBackAction(online)
                fetchAllTask(online)
            }
        }
    }

    //<editor-fold desc="base">

    /**从gitee获取数据*/
    fun get(json: String, end: (data: Response<JsonElement>?, error: Throwable?) -> Unit) {
        com.angcyo.http.get {
            url = if (json.isHttpScheme()) {
                json
            } else {
                "$BASE/${json.jsonName()}"
            }
            query = hashMapOf("time" to nowTime()) //带上时间参数, 避免缓存
            header = hashMapOf(LogInterceptor.closeLog(true))
            isSuccessful = {
                it.isSuccessful
            }
        }.map {
            if (!it.isSuccessful) {
                throw IllegalArgumentException(it.message())
            }
            it
        }.observer {
            onObserverEnd = { data, error ->
                end(data, error)
            }
        }
    }

    fun <T> assets(json: String, typeOfT: Type, end: (T) -> Unit) {
        app().readAssets(json.jsonName())
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
            val list = data?.toBean<TaskBean>(TaskBean::class.java)
            end(list, error)
        }
    }

    /**获取所有[CheckBean]*/
    fun fetchAllCheck(online: Boolean) {
        val result = mutableListOf<CheckBean>()
        val list = app().memoryConfigBean.file?.check ?: mutableListOf("check_common")
        if (list.isNullOrEmpty()) {
            L.e("请先配置[check]json文件")
            return
        }
        if (online) {
            list.forEach {
                getCheck(it) { list, error ->
                    list?.let {
                        result.addAll(it)
                        vmApp<GiteeModel>().allCheckData.value = result
                    }
                }
            }
        } else {
            list.forEach {
                assets<List<CheckBean>>(it, listType(CheckBean::class.java)) {
                    result.addAll(it)
                    vmApp<GiteeModel>().allCheckData.value = result
                }
            }
        }
    }

    /**获取所有[ActionBean]*/
    fun fetchAllAction(online: Boolean) {
        val result = mutableListOf<ActionBean>()
        val list = app().memoryConfigBean.file?.action ?: mutableListOf("all_actions")
        if (list.isNullOrEmpty()) {
            L.e("请先配置[action]json文件")
            return
        }
        if (online) {
            list.forEach {
                getAction(it) { list, error ->
                    list?.let {
                        result.addAll(it)
                        vmApp<GiteeModel>().allActionData.value = result
                    }
                }
            }
        } else {
            list.forEach {
                assets<List<ActionBean>>(it, listType(ActionBean::class.java)) {
                    result.addAll(it)
                    vmApp<GiteeModel>().allActionData.value = result
                }
            }
        }
    }

    /**获取所有回退[ActionBean]*/
    fun fetchAllBackAction(online: Boolean) {
        val result = mutableListOf<ActionBean>()
        val list = app().memoryConfigBean.file?.backAction ?: mutableListOf("back_actions")
        if (list.isNullOrEmpty()) {
            L.e("请先配置[backAction]json文件")
            return
        }
        if (online) {
            list.forEach {
                getAction(it) { list, error ->
                    list?.let {
                        result.addAll(it)
                        vmApp<GiteeModel>().allBackActionData.value = result
                    }
                }
            }
        } else {
            list.forEach {
                assets<List<ActionBean>>(it, listType(ActionBean::class.java)) {
                    result.addAll(it)
                    vmApp<GiteeModel>().allBackActionData.value = result
                }
            }
        }
    }

    /**获取所有[TaskBean]*/
    fun fetchAllTask(online: Boolean) {
        val result = mutableListOf<TaskBean>()
        val list = app().memoryConfigBean.file?.task
        if (list.isNullOrEmpty()) {
            L.e("请先配置[task]json文件")
            return
        }
        if (online) {
            list.forEach {
                getTask(it) { data, error ->
                    data?.let {
                        result.add(it)
                        vmApp<GiteeModel>().apply {
                            allTaskData.value = null
                            _addTasks(result)
                        }
                    }
                }
            }
        } else {
            list.forEach {
                assets<TaskBean>(it, TaskBean::class.java) {
                    result.add(it)
                    vmApp<GiteeModel>().apply {
                        allTaskData.value = null
                        _addTasks(result)
                    }
                }
            }
        }
    }

    //</editor-fold desc="task">

    //<editor-fold desc="fetch">

    fun fetchMemoryConfig(
        online: Boolean = isDebugType(),
        end: (data: MemoryConfigBean?, error: Throwable?) -> Unit = { _, _ -> }
    ) {
        val json = app().memoryConfigBean.file?.memoryConfig ?: "memory_config"

        if (online) {
            get(json) { data, error ->
                data?.toBean(MemoryConfigBean::class.java)?.let {
                    app().memoryConfigBean = it
                    end(it, error)
                }
                error?.let {
                    end(null, it)
                }
            }
        } else {
            assets<MemoryConfigBean>(json, MemoryConfigBean::class.java) {
                app().memoryConfigBean = it
                end(it, null)
            }
        }
    }

    /**功能列表*/
    fun fetchFunctionList(
        online: Boolean = isDebugType(),
        end: (list: List<FunctionBean>?, error: Throwable?) -> Unit = { _, _ -> }
    ) {
        val result = mutableListOf<FunctionBean>()
        val json = app().memoryConfigBean.file?.function ?: "function_list"
        if (online) {
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
        get(app().memoryConfigBean.file?.version ?: "version") { data, error ->
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