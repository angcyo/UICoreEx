package com.angcyo.laserpacker.open

import android.app.Activity
import android.os.Bundle
import com.angcyo.base.dslAHelper
import com.angcyo.library.component.ROpenFileHelper
import com.angcyo.library.ex.*
import com.angcyo.putData

/**
 * 注册一个: 使用[xxx]打开文件 的Activity
 *
 * 支持的后缀列表
 * [.jpg] [.jpeg] [.png]
 * [.gcode]
 * [.svg]
 *
 * 支持字体导入
 * [.ttf] [.otf] [.ttc]
 *
 * 支持固件升级
 * [.lpbin]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/15
 */
class CanvasOpenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //跳板转发
        dslAHelper {
            start(CanvasOpenPreviewActivity::class.java) {
                putData(ROpenFileHelper.parseIntent(getIntent()))
            }
        }
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}