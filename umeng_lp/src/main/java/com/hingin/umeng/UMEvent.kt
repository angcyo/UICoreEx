package com.hingin.umeng

/** 友盟自定义事件
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/22
 */
object UMEvent {

    //region ---origin---

    /**连接设备事件*/
    const val CONNECT_DEVICE = "connect_device"

    //事件开始的时间 13位时间戳,毫秒
    const val KEY_START_TIME = "key_start_time"

    //事件完成的时间 13位时间戳,毫秒
    const val KEY_FINISH_TIME = "key_finish_time"

    //事件持续的时长 毫秒差值
    const val KEY_DURATION = "key_duration"

    /**创作事件*/
    const val CREATE = "create"

    /**预览事件*/
    const val PREVIEW = "preview"

    /**雕刻事件*/
    const val ENGRAVE = "engrave"

    /**创作文本*/
    const val CANVAS_TEXT = "canvas_text"

    /**创作形状*/
    const val CANVAS_SHAPE = "canvas_shape"

    /**创作素材*/
    const val CANVAS_MATERIAL = "canvas_material"

    /**创作涂鸦*/
    const val CANVAS_DOODLE = "canvas_doodle"

    /**智能指南*/
    const val SMART_ASSISTANT = "smart_assistant"

    /**毫米单位*/
    const val MM_UNIT = "mm_unit"

    /**公制单位*/
    const val INCH_UNIT = "inch_unit"

    /**创作图片*/
    const val CANVAS_IMAGE = "canvas_image"

    /**创作图片-版画*/
    const val CANVAS_IMAGE_PRINT = "canvas_image_print"

    /**创作图片-GCode*/
    const val CANVAS_IMAGE_GCODE = "canvas_image_gcode"

    /**创作图片-黑白画*/
    const val CANVAS_IMAGE_BW = "canvas_image_bw"

    /**创作图片-抖动*/
    const val CANVAS_IMAGE_DITHERING = "canvas_image_dithering"

    /**创作图片-灰度*/
    const val CANVAS_IMAGE_GREY = "canvas_image_grey"

    /**创作图片-印章*/
    const val CANVAS_IMAGE_SEAL = "canvas_image_seal"

    //endregion ---origin---

    //region ---rn---

    /**RN-发帖事件*/
    const val RN_PUBLISH = "rn_publish"

    /**RN-点赞事件*/
    const val RN_LIKE = "rn_like"

    /**RN-下载事件*/
    const val RN_DOWNLOAD = "rn_download"

    /**RN-评论事件*/
    const val RN_COMMENT = "rn_comment"

    /**RN-分享事件*/
    const val RN_SHARE = "rn_share"

    /**RN-帮助事件*/
    const val RN_HELP = "rn_help"

    /**RN-FAQ事件*/
    const val RN_FAQ = "rn_faq"

    /**RN-新手引导事件*/
    const val RN_GUIDE = "rn_guide"

    /**RN-新手教程事件*/
    const val RN_COURSE = "rn_course"

    //endregion ---rn---

}