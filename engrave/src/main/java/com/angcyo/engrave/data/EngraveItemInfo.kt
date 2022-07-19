package com.angcyo.engrave.data

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/07/19
 */
data class EngraveItemInfo(

    /**正在雕刻的uuid
     * [com.angcyo.canvas.items.BaseItem.uuid]
     * [com.angcyo.engrave.data.EngraveReadyDataInfo.rendererItemUuid]
     * */
    var uuid: String? = null,

    /**雕刻进度*/
    var progress: Int = -1,
)
