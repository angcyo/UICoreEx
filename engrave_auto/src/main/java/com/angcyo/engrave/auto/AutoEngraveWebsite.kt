package com.angcyo.engrave.auto

import com.angcyo.base.dslAHelper
import com.angcyo.bluetooth.fsc.FscBleApiModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.canvas.data.CanvasOpenDataType
import com.angcyo.canvas.data.toCanvasProjectBean
import com.angcyo.canvas.data.toCanvasProjectItemBean
import com.angcyo.canvas.data.toTypeNameString
import com.angcyo.core.vmApp
import com.angcyo.engrave.model.AutoEngraveModel
import com.angcyo.http.rx.doMain
import com.angcyo.library.component.lastContext
import com.angcyo.library.ex.toBitmapOfBase64
import com.angcyo.library.ex.toBytes
import com.angcyo.library.ex.toInputStream
import com.angcyo.library.ex.uuid
import com.angcyo.viewmodel.observeOnce
import com.yanzhenjie.andserver.framework.body.StreamBody
import com.yanzhenjie.andserver.framework.body.StringBody
import com.yanzhenjie.andserver.framework.website.BasicWebsite
import com.yanzhenjie.andserver.http.HttpHeaders
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse
import com.yanzhenjie.andserver.http.ResponseBody
import com.yanzhenjie.andserver.util.MediaType
import java.io.InputStream

/**
 * 自动雕刻支持[com.angcyo.canvas.data.CanvasProjectBean] 和 [com.angcyo.canvas.data.CanvasProjectItemBean] body json 数据格式
 *
 * [com.angcyo.engrave.auto.AutoEngraveHelper.init]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/22
 */
class AutoEngraveWebsite : BasicWebsite() {

    override fun intercept(request: HttpRequest): Boolean {
        return request.path == "/engrave"
    }

    override fun getBody(request: HttpRequest, response: HttpResponse): ResponseBody {
        response.addHeader(HttpHeaders.Access_Control_Allow_Origin, "*")
        val bodyString = request.body?.string()
        val showActivity =
            request.parameter["activity"]?.firstOrNull()?.uppercase() == "TRUE" //是否需要显示界面

        if (bodyString.isNullOrEmpty()) {
            throw IllegalArgumentException("无效的请求体")
        }
        var engraveData: CanvasOpenDataType? = null
        val projectBean = bodyString.toCanvasProjectBean()
        var name = "Untitled"

        val fileName = projectBean?.file_name
        if (!fileName.isNullOrEmpty() && !projectBean.data.isNullOrEmpty()) {
            //是工程数据结构
            engraveData = projectBean
            name = fileName
        } else {
            //可能是com.angcyo.canvas.data.CanvasProjectItemBean数据
            val projectItemBean = bodyString.toCanvasProjectItemBean()
            if (projectItemBean != null && projectItemBean.mtype != -1) {
                //单个的item数据
                engraveData = projectItemBean
                name = projectItemBean.name ?: projectItemBean.mtype.toTypeNameString()
            }
        }

        //error
        if (engraveData == null) {
            throw IllegalArgumentException("无效的数据, body可是[CanvasProjectBean](必须包含file_name字段)和[CanvasProjectItemBean]的json字符串")
        }

        //open
        doMain {
            //device
            val fscBleApiModel = vmApp<FscBleApiModel>()
            val packerModel = vmApp<LaserPeckerModel>()
            val autoEngraveModel = vmApp<AutoEngraveModel>()
            val deviceAddress = request.parameter["device"]?.firstOrNull()?.uppercase() //需要连接的设备地址
            if (!deviceAddress.isNullOrBlank()) {
                val connectAddress = fscBleApiModel.lastDeviceState()?.device?.address?.uppercase()
                if (connectAddress == deviceAddress) {
                    //想要雕刻的设备和已连接的设备一致
                } else {
                    //否则直接连接设备
                    fscBleApiModel.connect(deviceAddress, "AutoEngrave")
                }
            }

            if (showActivity) {
                autoEngraveModel.engravePendingData.postValue(engraveData)
                lastContext.dslAHelper {
                    start(AutoEngraveActivity::class)
                }
            } else {
                //直接雕刻
                packerModel.initializeData.observeOnce {
                    if (it == true) {
                        //设备初始化完成之后
                        autoEngraveModel.startEngrave(uuid(), engraveData)
                        true
                    } else {
                        false
                    }
                }
            }
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