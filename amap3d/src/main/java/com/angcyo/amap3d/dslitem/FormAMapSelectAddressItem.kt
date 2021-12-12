package com.angcyo.amap3d.dslitem

import android.view.Gravity
import androidx.fragment.app.Fragment
import com.angcyo.amap3d.R
import com.angcyo.amap3d.fragment.aMapSelector
import com.angcyo.base.dslFHelper
import com.angcyo.core.dslitem.IFragmentItem
import com.angcyo.item.DslLabelEditItem
import com.angcyo.item.form.FormItemConfig
import com.angcyo.item.form.IFormItem
import com.angcyo.library.L
import com.angcyo.library.ex._color

/**
 * 高德地图选地址 表单item
 * Email:1583534549@qq.com
 * @author sj
 * @date 2020/06/04
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class FormAMapSelectAddressItem : DslLabelEditItem(), IFormItem, IFragmentItem {

    /**选中后的经度*/
    var itemLongitude: Double = 0.0

    /**选中后的纬度*/
    var itemLatitude: Double = 0.0

    /**兴趣点名字*/
    var itemPoiName: CharSequence? = null

    /**地址*/
    var itemAddress: CharSequence? = null
        set(value) {
            field = value
            editItemConfig.itemEditText = value
        }

    override var itemFragment: Fragment? = null

    init {
        itemEditTipIcon = R.drawable.map_icon_gps

        configLabelTextStyle {
            textColor = _color(R.color.text_general_color)
        }

        configEditTextStyle {
            hint = IFormItem.DEFAULT_SELECTOR_HINT
            textColor = _color(R.color.text_sub_color)
            textGravity = Gravity.CENTER_VERTICAL or Gravity.RIGHT
            noEditModel = true
        }
        //formDrawLeft()

        itemRightIcoClick = { _, _ ->
            if (itemFragment == null) {
                L.w("itemFragment is null.")
            } else {
                itemFragment?.dslFHelper {
                    aMapSelector {
                        it?.apply {
                            itemLongitude = longitude
                            itemLatitude = latitude

                            itemPoiName = poiName
                            itemAddress = address

                            itemChanging = true
                        }
                    }
                }
            }
        }
    }

    //<editor-fold desc="表单相关">

    override var itemFormConfig: FormItemConfig = FormItemConfig().apply {
        formCheck = { params, end ->
            var error: Throwable? = null
            if (formRequired) {
                if (editItemConfig.itemEditText.isNullOrEmpty() ||
                    itemLatitude == 0.0 ||
                    itemLongitude == 0.0
                ) {
                    error = IllegalArgumentException("请选择位置")
                }
            }
            end(error)
        }
        onGetFormValue = {
            null
        }
    }

    //</editor-fold desc="表单相关">
}