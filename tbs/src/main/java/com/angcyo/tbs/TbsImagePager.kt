package com.angcyo.tbs

import com.angcyo.http.base.fromJson
import com.angcyo.library.ex.size
import com.angcyo.library.ex.toUri
import com.angcyo.library.model.LoaderMedia
import com.angcyo.pager.dslPager
import com.angcyo.tbs.core.TbsWebFragment
import com.angcyo.tbs.core.inner.TbsWebView
import kotlin.math.max
import kotlin.math.min

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2022/01/17
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
object TbsImagePager {

    const val PAGER_HANDLER_NAME = "showImagePager"

    /**图片预览方法注入
     * js调用方式
     *
     * androidJs.showImagePager(
     * {
     *   "index": 0,
     *   "images": [
     *     "url1",
     *     "url2"
     *   ]
     * }
     * )
     *
     * */
    fun register(fragment: TbsWebFragment, webView: TbsWebView) {
        webView.registerHandler(PAGER_HANDLER_NAME) { data, function ->
            val bean = data?.fromJson<ImagePickerBean>()
            if (bean == null || bean.images.isNullOrEmpty()) {
                function?.onCallBack("无效的数据")
            } else {
                val size = bean.images.size()
                val index = min(max(bean.index, 0), size - 1)
                fragment.dslPager {
                    startPosition = index
                    loaderMediaList.addAll(bean.images.map {
                        if (it.startsWith("http")) {
                            LoaderMedia(url = it)
                        } else {
                            val uri = webView.url.toUri()
                            val url = uri?.buildUpon()?.path(it)?.build()?.toString()
                            LoaderMedia(url = url)
                        }
                    })
                }
                function?.onCallBack("开始预览:[$index/${size}]")
            }
        }
    }
}

data class ImagePickerBean(
    val index: Int = 0,
    val images: List<String>? = null,
)