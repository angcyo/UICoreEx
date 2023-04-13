package com.angcyo.server.file

import com.angcyo.core.component.model.DataShareModel
import com.angcyo.core.vmApp
import com.angcyo.library.ex.copyToFile
import com.angcyo.library.ex.nowTimeString
import com.angcyo.library.libCacheFile
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestBody
import com.yanzhenjie.andserver.annotation.RequestParam
import com.yanzhenjie.andserver.annotation.RestController
import com.yanzhenjie.andserver.http.multipart.MultipartFile


/**
 * 文件服务相关的控制接口
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/04/09
 * Copyright (c) 2020 angcyo. All rights reserved.
 */

@RestController(FileServerService.GROUP_NAME)
class FileController {

    /**实现一个上传文件的口, 文件接收*/
    @PostMapping("/uploadFile")
    fun handleFileUpload(@RequestParam("file") file: MultipartFile?): String {
        // 处理上传的文件
        file?.let {
            //file.name //这个值就是 file 字符串
            val cacheFile = libCacheFile(file.filename ?: file.name)
            val filePath = cacheFile.absolutePath
            it.stream.copyToFile(filePath)

            vmApp<DataShareModel>().shareFileOnceData.postValue(cacheFile)
            return "success:${filePath}"
        }
        return "no file!"
    }

    /**多文件上传接口*/
    @PostMapping("/uploadFiles")
    fun handleFileUpload(@RequestParam("files") files: Array<MultipartFile>?): String? {
        // 处理上传的文件
        files?.let {
            return buildString {
                it.forEach {
                    appendLine(handleFileUpload(it))
                }
            }
        }
        return "no file!"
    }

    /**接收字符串的接口*/
    @PostMapping("/body")
    fun handleMessage(@RequestBody body: String): String {
        return "${nowTimeString()} 成功:${body}"
    }
}