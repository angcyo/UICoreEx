package com.angcyo.engrave.data

import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerHelper
import com.angcyo.engrave.transition.EngraveTransitionManager

/**
 * 传输数据前的配置信息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/29
 */
data class TransferDataConfigInfo(
    /**
     * 雕刻显示的文件名, 28个字节
     * [com.angcyo.bluetooth.fsc.laserpacker.command.DataCmd.DEFAULT_NAME_BYTE_COUNT]
     * */
    var name: String = EngraveTransitionManager.generateEngraveName(),

    /**分辨率*/
    var px: Byte = LaserPeckerHelper.DEFAULT_PX,
)