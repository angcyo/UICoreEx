package com.angcyo.engrave.dslitem.preview

import com.angcyo.bluetooth.fsc.laserpacker.DeviceStateModel
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.engrave.model.EngraveModel
import com.angcyo.engrave.model.PreviewModel
import com.angcyo.objectbox.laser.pecker.entity.PreviewConfigEntity

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/09/24
 */
abstract class BasePreviewItem : DslAdapterItem() {

    //产品模式
    val laserPeckerModel = vmApp<LaserPeckerModel>()
    val deviceStateModel = vmApp<DeviceStateModel>()

    //雕刻模式
    val engraveModel = vmApp<EngraveModel>()

    //预览模式
    val previewModel = vmApp<PreviewModel>()

    /**参数配置实体*/
    var itemPreviewConfigEntity: PreviewConfigEntity? = null

    /**查询设备状态*/
    fun queryDeviceStateCmd() {
        deviceStateModel.queryDeviceState()
    }
}