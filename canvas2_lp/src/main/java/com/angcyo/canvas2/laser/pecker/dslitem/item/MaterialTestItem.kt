package com.angcyo.canvas2.laser.pecker.dslitem.item

import com.angcyo.bluetooth.fsc.laserpacker.HawkEngraveKeys
import com.angcyo.bluetooth.fsc.laserpacker.LaserPeckerConfigHelper
import com.angcyo.canvas2.laser.pecker.R
import com.angcyo.canvas2.laser.pecker.dialog.addMaterialTestDialog
import com.angcyo.canvas2.laser.pecker.dslitem.CanvasIconItem
import com.angcyo.item.style.itemDefaultNew
import com.angcyo.item.style.itemHaveNew
import com.angcyo.item.style.itemNewHawkKeyStr
import com.angcyo.library.component.hawk.HawkProperty
import com.angcyo.library.ex._string
import com.hingin.umeng.UMEvent
import com.hingin.umeng.umengEventValue2

/**
 * 添加材料测试阵列
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023-12-4
 */
class MaterialTestItem : CanvasIconItem() {

    companion object {

        /**是否可以显示材料测试阵列*/
        val canShowMaterialTest: Boolean
            get() = HawkEngraveKeys.enableItemEngraveParams && HawkEngraveKeys.enableSingleItemTransfer
    }

    init {
        itemIco = R.drawable.canvas_material_test
        itemText = _string(R.string.canvas_material_test)
        itemEnable = true
        itemHidden = !canShowMaterialTest
        itemNewHawkKeyStr = "materialTest"
        itemDefaultNew = LaserPeckerConfigHelper.haveNew(itemNewHawkKeyStr)
        itemClick = {
            itemHaveNew = false
            updateAdapterItem()
            UMEvent.CANVAS_MATERIAL_TEST.umengEventValue2()
            it.context.addMaterialTestDialog {
                renderDelegate = itemRenderDelegate
            }
        }

        HawkProperty.hawkPropertyChangeActionList.add {
            if (it == HawkEngraveKeys::enableItemEngraveParams.name || it == HawkEngraveKeys::enableSingleItemTransfer.name) {
                itemHidden = !canShowMaterialTest
            }
        }
    }

}