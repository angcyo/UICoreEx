package com.angcyo.tim.bean

import android.graphics.Bitmap
import com.angcyo.tim.util.FaceManager.defaultEmojiSize

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/11/11
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class Emoji {
    var desc: String? = null
    var filter: String? = null //[傲慢] [删除]
    var icon: Bitmap? = null
    var width: Int = defaultEmojiSize
    var height: Int = defaultEmojiSize
}