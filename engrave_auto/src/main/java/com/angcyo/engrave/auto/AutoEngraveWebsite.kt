package com.angcyo.engrave.auto

import com.angcyo.base.dslAHelper
import com.angcyo.canvas.data.CanvasOpenDataType
import com.angcyo.canvas.data.toCanvasProjectBean
import com.angcyo.canvas.data.toCanvasProjectItemBean
import com.angcyo.canvas.data.toTypeNameString
import com.angcyo.core.vmApp
import com.angcyo.engrave.model.AutoEngraveModel
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.library.ex.toBytes
import com.angcyo.library.ex.toInputStream
import com.yanzhenjie.andserver.framework.body.StreamBody
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.framework.website.BasicWebsite
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/22
 */
class AutoEngraveWebsite : BasicWebsite() {

    override fun intercept(request: HttpRequest): Boolean {
        return request.path == "/engrave"
    }

    override fun getBody(request: HttpRequest, response: HttpResponse): ResponseBody {
        val bodyString = request.body?.string()
        if (bodyString.isNullOrEmpty()) {
            throw IllegalArgumentException("无效的请求体")
        }
        var engraveData: CanvasOpenDataType? = null
        val projectBean = bodyString.toCanvasProjectBean()
        var name = "Untitled"
        if (projectBean?.data.isNullOrEmpty()) {
            //非工程数据
            val projectItemBean = bodyString.toCanvasProjectItemBean()
            if (projectItemBean != null && projectItemBean.mtype != -1) {
                //单个的item数据
                engraveData = projectItemBean
                name = projectItemBean.name ?: projectItemBean.mtype.toTypeNameString()
            }
        } else {
            engraveData = projectBean
            name = projectBean?.file_name ?: "Untitled"
        }

        if (engraveData == null) {
            throw IllegalArgumentException("无效的数据")
        }

        //open
        vmApp<AutoEngraveModel>().engravePendingData.postValue(engraveData)
        lastContext.dslAHelper {
            start(AutoEngraveActivity::class)
        }

        //result
        val previewBitmap = projectBean?.preview_img?.toBitmapOfBase64()
        return if (previewBitmap == null) {
            StringBody("准备雕刻:${name}")
        } else {
            val bytes = previewBitmap.toBytes()
            val inputStream: InputStream? = bytes?.toInputStream()
            StreamBody(inputStream, (bytes?.size ?: 0).toLong(), MediaType.IMAGE_PNG)
        }
    }
}