package com.angcyo.baidu.trace

import android.Manifest
import android.content.Context
import com.angcyo.library.L
import com.angcyo.library.app
import com.angcyo.library.component.DslNotify
import com.angcyo.library.component.dslBuildNotify
import com.angcyo.library.component.isNetworkAvailable
import com.angcyo.library.ex.havePermissions
import com.angcyo.library.getAppName
import com.angcyo.library.utils.Device
import com.angcyo.library.utils.getIMEI
import com.baidu.trace.LBSTraceClient
import com.baidu.trace.Trace
import com.baidu.trace.api.entity.*
import com.baidu.trace.api.track.HistoryTrackRequest
import com.baidu.trace.api.track.HistoryTrackResponse
import com.baidu.trace.api.track.LatestPointRequest
import com.baidu.trace.api.track.OnTrackListener
import com.baidu.trace.model.OnCustomAttributeListener
import com.baidu.trace.model.OnTraceListener
import com.baidu.trace.model.ProcessOption
import com.baidu.trace.model.PushMessage


/**
 * 百度鹰眼轨迹服务
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/07/13
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class DslBaiduTrace {

    //<editor-fold desc="配置项">

    /**
     * 轨迹服务ID
     *
     * http://lbsyun.baidu.com/trace/admin/service
     * */
    var serviceId: Long = -1

    /**
     * 设备标识
     * */
    var entityName: String? = null
        set(value) {
            if (!field.isNullOrEmpty() && field != value) {
                //停止之前的轨迹
                stopTrace()
            }
            field = value
        }

    // 是否需要对象存储服务，默认为：false，关闭对象存储服务。
    // 注：鹰眼 Android SDK v3.0以上版本支持随轨迹上传图像等对象数据，
    // 若需使用此功能，该参数需设为 true，且需导入bos-android-sdk-1.0.2.jar。
    var isNeedObjectStorage: Boolean = false

    // 定位周期(单位:秒) 2~300秒。
    var gatherInterval = 5

    // 打包回传周期(单位:秒) 2~300秒。
    var packInterval = 10

    /**通知栏保活*/
    var showNotify: Boolean = false

    /**通知栏配置*/
    var configNotify: DslNotify.() -> Unit = {

    }

    //</editor-fold desc="配置项">

    //轨迹服务客户端
    var _traceClient: LBSTraceClient? = null

    var _trace: Trace? = null

    //<editor-fold desc="操作项">

    /**
     * 开启轨迹服务
     * http://lbsyun.baidu.com/index.php?title=android-yingyan/guide/hellotrace*/
    fun startTrace(context: Context = app()) {
        if (_traceClient == null) {

            if (!_checkConfig()) {
                L.e("轨迹配置失败, 请检查!")
                return
            }

            // 初始化轨迹服务
            _trace = Trace(serviceId, entityName, isNeedObjectStorage)

            if (showNotify) {
                _trace?.notification = dslBuildNotify {
                    channelName = "BaiduTrace"
                    notifyTitle = getAppName()
                    notifyText = "轨迹服务正在运行..."
                    configNotify()
                }
            }

            // 初始化轨迹服务客户端
            _traceClient = LBSTraceClient(context).apply {

            }

            // 设置定位和打包周期
            _traceClient?.setInterval(gatherInterval, packInterval)

            //自定义属性
            _traceClient?.setOnCustomAttributeListener(customAttributeListener)

            // 开启服务
            _traceClient?.startTrace(_trace, traceListener)
        } else {
            if (isTraceStart) {
                L.w("已有轨迹服务开启")
            } else {
                // 开启服务
                _traceClient?.startTrace(_trace, traceListener)
            }
        }
    }

    /**停止轨迹服务*/
    fun stopTrace() {
        // 停止服务
        _traceClient?.stopTrace(_trace, traceListener)
        _traceClient = null
    }

    /**开始服务轨迹采集*/
    fun startGather() {
        // 开启采集
        if (isTraceStart) {
            _traceClient?.startGather(traceListener)
        } else {
            L.w("请先开始轨迹服务.")
        }
    }

    /**停止轨迹采集*/
    fun stopGather() {
        // 停止采集
        _traceClient?.stopGather(traceListener)
    }

    //</editor-fold desc="操作项">

    fun _checkConfig(): Boolean {
        if (serviceId < 0) {
            L.w("未指定[serviceId]")
            return false
        }

        if (entityName.isNullOrEmpty()) {
            L.w("未指定[entityNameId]")
            return false
        }
        return true
    }

    /**开启服务后, 是否自动开启采集*/
    var autoTraceStart: Boolean = false

    //轨迹服务是否开始了
    var isTraceStart: Boolean = false

    //采集是否开始了
    var isGatherStart: Boolean = false

    // 初始化轨迹服务监听器
    //http://mapopen-pub-yingyan.cdn.bcebos.com/androidsdk/doc/v3.1.7/index.html
    var traceListener: OnTraceListener? = object : OnTraceListener {

        /**
         * 开启服务回调接口
         * @param errorNo 状态码
         * @param message 消息
         * <p>
         * <pre>0：成功 </pre>
         * <pre>10000：请求发送失败</pre>
         * <pre>10001：服务开启失败</pre>
         * <pre>10002：参数错误</pre>
         * <pre>10003：网络连接失败</pre>
         * <pre>10004：网络未开启</pre>
         * <pre>10005：服务正在开启</pre>
         * <pre>10006：服务已开启</pre>
         */
        override fun onStartTraceCallback(status: Int, message: String?) {
            L.i(status, "->", message)
            isTraceStart = status == 0
            if (autoTraceStart && status == 0) {
                // 开启采集
                _traceClient?.startGather(this)
            }
        }

        /**
         * 停止服务回调接口
         * @param errorNo 状态码
         * @param message 消息
         * <p>
         * <pre>0：成功</pre>
         * <pre>11000：请求发送失败</pre>
         * <pre>11001：服务停止失败</pre>
         * <pre>11002：服务未开启</pre>
         * <pre>11003：服务正在停止</pre>
         */
        override fun onStopTraceCallback(status: Int, message: String?) {
            L.i(status, "->", message)
            if (status == 0) {
                isTraceStart = false
            }
        }

        /**
         * 开启采集回调接口
         * @param errorNo 状态码
         * @param message 消息
         * <p>
         * <pre>0：成功</pre>
         * <pre>12000：请求发送失败</pre>
         * <pre>12001：采集开启失败</pre>
         * <pre>12002：服务未开启</pre>
         */
        override fun onStartGatherCallback(status: Int, message: String?) {
            L.i(status, "->", message)
            isGatherStart = status == 0
        }

        /**
         * 停止采集回调接口
         * @param errorNo 状态码
         * @param message 消息
         * <p>
         * <pre>0：成功</pre>
         * <pre>13000：请求发送失败</pre>
         * <pre>13001：采集停止失败</pre>
         * <pre>13002：服务未开启</pre>
         */
        override fun onStopGatherCallback(status: Int, message: String?) {
            L.i(status, "->", message)
            if (status == 0) {
                isGatherStart = false
            }
        }

        /**
         * 绑定服务回调接口
         * @param errorNo  状态码
         * @param message 消息
         * <p>
         * <pre>0：成功 </pre>
         * <pre>1：失败</pre>
         */
        override fun onBindServiceCallback(status: Int, message: String?) {
            L.i(status, "->", message, " $serviceId $entityName")
        }

        override fun onInitBOSCallback(status: Int, message: String?) {
            L.i(status, "->", message)
        }

        /**
         * 推送消息回调接口
         *
         * @param messageType 状态码
         * @param pushMessage 消息
         * <p>
         * <pre>0x01：配置下发</pre>
         * <pre>0x02：语音消息</pre>
         * <pre>0x03：服务端围栏报警消息</pre>
         * <pre>0x04：本地围栏报警消息</pre>
         * <pre>0x05~0x40：系统预留</pre>
         * <pre>0x41~0xFF：开发者自定义</pre>
         */
        override fun onPushCallback(messageNo: Byte, message: PushMessage?) {
            L.i(messageNo, "->", message)
        }
    }

    //自定义属性回调
    //http://mapopen-pub-yingyan.cdn.bcebos.com/androidsdk/doc/v3.1.7/index.html
    var customAttributeListener: OnCustomAttributeListener? = object : OnCustomAttributeListener {
        override fun onTrackAttributeCallback(): MutableMap<String, String>? {
            return null
        }

        //locTime - 回调时定位点的时间戳（毫秒）
        override fun onTrackAttributeCallback(locTime: Long): MutableMap<String, String>? {
            val result = hashMapOf<String, String>()
            result["deviceId"] = Device.deviceId
            result["androidId"] = Device.androidId
            if (app().havePermissions(Manifest.permission.READ_PHONE_STATE)) {
                result["imei"] = app().getIMEI(log = false) ?: ""
            }
            return result
        }
    }

    //<editor-fold desc="查询轨迹">

    /**
     * 查询历史轨迹
     * [startTime] 设置轨迹查询起止时间, 开始时间(单位：秒)
     * [endTime] 结束时间(单位：秒)
     * */
    fun queryHistoryTrack(
        tag: Int,
        entityName: String,
        startTime: Long,
        endTime: Long,
        processOption: ProcessOption? = null, //纠偏选项
        result: (response: HistoryTrackResponse?) -> Unit
    ) {

        if (!_checkConfig()) {
            return
        }

        // 创建历史轨迹请求实例
        val historyTrackRequest = HistoryTrackRequest(tag, serviceId, entityName)
        // 设置开始时间
        historyTrackRequest.startTime = startTime
        // 设置结束时间
        historyTrackRequest.endTime = endTime

        //http://lbsyun.baidu.com/index.php?title=android-yingyan/guide/trackprocess

        if (processOption != null) {
            // 设置需要纠偏
            historyTrackRequest.isProcessed = true

//            // 设置需要去噪
//            processOption.isNeedDenoise = true
//            // 设置需要抽稀
//            processOption.isNeedVacuate = true
//            // 设置需要绑路
//            processOption.isNeedMapMatch = true
//            // 设置精度过滤值(定位精度大于100米的过滤掉)
//            processOption.radiusThreshold = 100
//            // 设置交通方式为驾车
//            processOption.transportMode = TransportMode.driving
//            // 设置里程填充方式为驾车
//            historyTrackRequest.supplementMode = SupplementMode.driving

            // 设置纠偏选项
            historyTrackRequest.processOption = processOption
        }


        val traceClient = _traceClient ?: LBSTraceClient(app())

        // 初始化轨迹监听器
        val mTrackListener: OnTrackListener = object : OnTrackListener() {
            // 历史轨迹回调
            override fun onHistoryTrackCallback(response: HistoryTrackResponse?) {
                L.d(response)
                if (traceClient != _traceClient) {
                    traceClient.clear()
                }
                result(response)
            }
        }

        // 查询历史轨迹
        traceClient.queryHistoryTrack(historyTrackRequest, mTrackListener)
    }

    /**查询实时位置
     * [entityNames] entity标识列表（多个entityName，以英文逗号"," 分割）
     * [columnKey] 检索条件（格式为 : "key1=value1,key2=value2,....."）
     * [returnType] 返回结果的类型（0 : 返回全部结果，1 : 只返回entityName的列表）
     * [activeTime] 活跃时间，UNIX时间戳（指定该字段时，返回从该时间点之后仍有位置变动的entity的实时点集合）
     *
     * http://mapopen-pub-yingyan.cdn.bcebos.com/androidsdk/doc/v3.1.7/index.html
     * */
    fun queryEntityList(
        action: EntityListRequest.() -> Unit,
        result: (response: EntityListResponse?) -> Unit
    ) {

        if (!_checkConfig()) {
            return
        }

        val traceClient = _traceClient ?: LBSTraceClient(app())

        //Entity监听器
        val entityListener: OnEntityListener = object : OnEntityListener() {

            //查询Entity列表回调接口
            override fun onEntityListCallback(response: EntityListResponse?) {
                L.d(response)
                if (traceClient != _traceClient) {
                    traceClient.clear()
                }
                result(response)
            }
        }

        val request = EntityListRequest().apply {
            //setTag()
            setServiceId(serviceId)
            //coordTypeOutput = CoordType.bd09ll
            pageIndex = 1
            pageSize = 1000
            filterCondition = FilterCondition().apply {
                //entityNames =
                //columns
                //activeTime
            }

            action()
        }

        //查询实时轨迹
        traceClient.queryEntityList(request, entityListener)
    }

    /**获取当前位置*/
    fun getCurrentLocation(
        tag: Int,
        processOption: ProcessOption? = null, //纠偏选项
        entityListener: OnEntityListener,
        trackListener: OnTrackListener
    ) {

        if (!_checkConfig()) {
            return
        }

        val traceClient = _traceClient ?: LBSTraceClient(app())

        // 网络连接正常，开启服务及采集，则查询纠偏后实时位置；否则进行实时定位
        if (isNetworkAvailable()) {
            val request = LatestPointRequest(tag, serviceId, entityName)
            request.processOption = processOption
            traceClient.queryLatestPoint(request, trackListener)
        } else {
            val locRequest = LocRequest(serviceId)
            traceClient.queryRealTimeLoc(locRequest, entityListener)
        }
    }

    //</editor-fold desc="查询轨迹">
}