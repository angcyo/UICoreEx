package com.hingin.umeng

/** 友盟自定义事件
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/22
 */
object UMEvent {

    /**连接设备事件*/
    const val CONNECT_DEVICE = "connect_device"

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
}