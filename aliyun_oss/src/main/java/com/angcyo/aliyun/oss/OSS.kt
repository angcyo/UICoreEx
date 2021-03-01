package com.angcyo.aliyun.oss

import android.content.Context
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.OSSLog
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask
import com.alibaba.sdk.android.oss.model.*
import com.angcyo.library.L


/**
 * https://help.aliyun.com/document_detail/32042.html
 *
 * https://github.com/aliyun/aliyun-oss-android-sdk/blob/master/app/src/main/java/com/alibaba/oss/app/service/OssService.java
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/11/21
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

class OSS {

    companion object {
        /**根据[endpoint]保存对应的[OSS]对象*/
        val ossMap = hashMapOf<String, OSS>()

        /**获取第一个初始化的OSS对象*/
        fun first(): OSS? {
            ossMap.keys.firstOrNull()?.let {
                return ossMap[it]
            }
            return null
        }

        /**OSS[endpoint]*/
        operator fun get(endpoint: String): OSS? {
            return ossMap[endpoint]
        }
    }

    val configuration = ClientConfiguration().apply {
        connectionTimeout = 15 * 1000 // 连接超时，默认15秒
        socketTimeout = 15 * 1000 // socket超时，默认15秒
        maxConcurrentRequest = 5 // 最大并发请求书，默认5个
        maxErrorRetry = 2 // 失败后最大重试次数，默认2次
    }

    /**
     * OSS endpoint, check out:
     *
     * http://help.aliyun.com/document_detail/oss/user_guide/endpoint_region.html
     *
     * //访问域名和数据中心
     * https://help.aliyun.com/document_detail/31837.htm
     *
     */
    var endpoint: String = "oss-cn-shenzhen.aliyuncs.com"

    /**默认的[bucketName]*/
    var bucketName: String? = null

    //oss client
    lateinit var client: OSSClient

    var onProgress: (request: OSSRequest, currentSize: Long, totalSize: Long) -> Unit =
        { _, _, _ -> }

    var onSuccess: (request: OSSRequest, result: OSSResult) -> Unit = { _, _ -> }

    var onFailure: (request: OSSRequest, clientException: ClientException?, serviceException: ServiceException?) -> Unit =
        { _, _, _ -> }

    /**
     * 移动端是不安全环境，不建议直接使用阿里云主账号ak，sk的方式。建议使用STS方式。具体参
     * https://help.aliyun.com/document_detail/31920.html
     *
     * https://ram.console.aliyun.com
     *
     * 创建用户获取 aki aks
     * https://ram.console.aliyun.com/users/angcyo
     * */
    fun init(
        context: Context,
        accessKeyId: String,
        accessKeySecret: String,
        debug: Boolean = false
    ) {
        init(context, OSSPlainTextAKSKCredentialProvider(accessKeyId, accessKeySecret), debug)
    }

    /**
     * [provider] 鉴权模式
     * */
    fun init(context: Context, provider: OSSCredentialProvider, debug: Boolean = false) {
        client = OSSClient(context.applicationContext, endpoint, provider, configuration)
        if (debug) {
            OSSLog.enableLog()
        } else {
            OSSLog.disableLog()
        }
        ossMap[endpoint] = this
    }

    /**https://help.aliyun.com/document_detail/32047.html
     * 简单上传文件
     * [bucketName]
     * [uploadObj] 可以是文件名, 也可以是ByteArray数据, 为null, 则会删除对象
     * */
    fun upload(
        bucketName: String,
        objectName: String,
        uploadObj: Any?
    ): OSSAsyncTask<OSSResult>? {

        val progressAction = onProgress
        val successAction = onSuccess
        val failureAction = onFailure

        if (uploadObj == null) {
            //删除
            val del = DeleteObjectRequest(bucketName, objectName)
            val task = client.asyncDeleteObject(del,
                object : OSSCompletedCallback<DeleteObjectRequest, DeleteObjectResult> {
                    override fun onSuccess(
                        request: DeleteObjectRequest,
                        result: DeleteObjectResult
                    ) {
                        L.d("PutObject", "UploadSuccess")
                        L.d("RequestId", result.requestId)
                        successAction(request, result)
                    }

                    override fun onFailure(
                        request: DeleteObjectRequest,
                        clientException: ClientException?,
                        serviceException: ServiceException?
                    ) {
                        // 请求异常。
                        clientException?.printStackTrace()
                        if (serviceException != null) {
                            // 服务异常。
                            L.e("ErrorCode", serviceException.errorCode)
                            L.e("RequestId", serviceException.requestId)
                            L.e("HostId", serviceException.hostId)
                            L.e("RawMessage", serviceException.rawMessage)
                        }
                        failureAction(request, clientException, serviceException)
                    }
                })
            return task as OSSAsyncTask<OSSResult>
        }

        // 构造上传请求。
        val put = when (uploadObj) {
            is String -> PutObjectRequest(bucketName, objectName, uploadObj)
            is ByteArray -> PutObjectRequest(bucketName, objectName, uploadObj)
            else -> return null
        }

        put.setProgressCallback { request, currentSize, totalSize ->
            L.d("PutObject", "currentSize: $currentSize totalSize: $totalSize")
            progressAction(request, currentSize, totalSize)
        }

        // 文件元信息的设置是可选的。
        // ObjectMetadata metadata = new ObjectMetadata();
        // metadata.setContentType("application/octet-stream"); // 设置content-type。
        // metadata.setContentMD5(BinaryUtil.calculateBase64Md5(uploadFilePath)); // 校验MD5。
        // put.setMetadata(metadata);
        val task: OSSAsyncTask<PutObjectResult> = client.asyncPutObject(put,
            object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest, result: PutObjectResult) {
                    L.d("PutObject", "UploadSuccess")
                    L.d("ETag", result.eTag)
                    L.d("RequestId", result.requestId)
                    successAction(request, result)
                }

                override fun onFailure(
                    request: PutObjectRequest,
                    clientException: ClientException?,
                    serviceException: ServiceException?
                ) {
                    // 请求异常。
                    clientException?.printStackTrace()
                    if (serviceException != null) {
                        // 服务异常。
                        L.e("ErrorCode", serviceException.errorCode)
                        L.e("RequestId", serviceException.requestId)
                        L.e("HostId", serviceException.hostId)
                        L.e("RawMessage", serviceException.rawMessage)
                    }
                    failureAction(request, clientException, serviceException)
                }
            })
        return task as OSSAsyncTask<OSSResult>
        // task.cancel(); // 可以取消任务。
        // task.waitUntilFinished(); // 等待任务完成。
    }

    fun upload(
        objectName: String,
        uploadObj: Any?,
        bucketName: String = this.bucketName ?: "default"
    ): OSSAsyncTask<OSSResult>? {
        return upload(bucketName, objectName, uploadObj)
    }
}