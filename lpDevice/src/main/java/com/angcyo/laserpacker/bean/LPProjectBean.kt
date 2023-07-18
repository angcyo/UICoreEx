package com.angcyo.laserpacker.bean

import android.graphics.Bitmap
import com.angcyo.library.annotation.MM

/**
 * LP工程结构, 里面包含很多子元素[com.angcyo.laserpacker.bean.LPElementBean]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/03/06
 */
data class LPProjectBean(

    /**画布的宽高*/
    @MM
    var width: Double = 0.0,
    @MM
    var height: Double = 0.0,

    /**预览的base64图片
     * (data:image/xxx;base64,xxx) 带协议头
     *
     * Canvas: trying to draw too large(141018708bytes) bitmap.
     * */
    var preview_img: String? = null,

    /**item list 的所有数据
     * [com.angcyo.laserpacker.bean.LPElementBean]
     * */
    var data: String? = null,

    /**工程名*/
    var file_name: String? = null,

    /**工程创建时间, 13位毫秒*/
    var create_time: Long = -1,

    /**工程创建时间, 13位毫秒*/
    var update_time: Long = -1,

    /**数据内容版本*/
    var version: Int = 1,

    /**固件版本号*/
    var swVersion: Int = 0,

    /**硬件版本号*/
    var hwVersion: Int = 0,

    /**扩展设备*/
    var exDevice: String? = null,

    /**产品名, 比如L4 C1等
     * [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.productName]*/
    var productName: String? = null,

    /** [com.angcyo.objectbox.laser.pecker.entity.EngraveConfigEntity.moduleState]*/
    var moduleState: Int = -1,

    /**每个图层对应的雕刻参数*/
    var laserOptions: List<LPLaserOptionsBean>? = null,

    //---

    /**工程保存的上一次雕刻参数*/
    var lastPower: Int = -1,
    var lastDepth: Int = -1,
    var lastType: Int = -1,
    var lastDpi: Float = -1f,

    //---

    /**预览图
     * [preview_img]*/
    @Transient var _previewImgBitmap: Bitmap? = null,

    /**本地对应的文件路径, 如果有*/
    var _filePath: String? = null,

    /**是否处于调试模式下, 用于debug下方便断点*/
    var _debug: Boolean? = null
)
