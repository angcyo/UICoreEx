package com.angcyo.engrave.transition

import com.angcyo.canvas.graphics.IEngraveProvider
import com.angcyo.canvas.utils.CanvasConstant
import com.angcyo.engrave.transition.EngraveTransitionManager.Companion.toTransferData
import com.angcyo.engrave.transition.EngraveTransitionManager.Companion.writeTransferDataPath
import com.angcyo.objectbox.laser.pecker.entity.TransferConfigEntity
import com.angcyo.objectbox.laser.pecker.entity.TransferDataEntity

/**
 * 传输过来的是真实数据, 直接存文件即可
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/24
 */
class RawTransition : IEngraveTransition {

    override fun doTransitionTransferData(
        engraveProvider: IEngraveProvider,
        transferConfigEntity: TransferConfigEntity,
        param: TransitionParam?
    ): TransferDataEntity? {
        val dataItem = engraveProvider.getEngraveDataItem()
        val dataBean = dataItem?.dataBean
        if (dataBean?.mtype == CanvasConstant.DATA_TYPE_RAW && !dataBean.data.isNullOrEmpty()) {

            val transferDataEntity = TransferDataEntity()
            transferDataEntity.index = EngraveTransitionManager.generateEngraveIndex()
            transferDataEntity.engraveDataType = dataBean.engraveType
            initTransferDataEntity(engraveProvider, transferConfigEntity, transferDataEntity)
            transferDataEntity.lines = dataBean.lines

            //data
            transferDataEntity.dataPath = dataBean.data!!.toTransferData()
                .writeTransferDataPath("${transferDataEntity.index}")
        }
        return null
    }
}