package com.angcyo.acc2.app.dslitem

import com.angcyo.acc2.app.R
import com.angcyo.acc2.app.model.AdaptiveModel
import com.angcyo.core.vmApp
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.library.component.appBean
import com.angcyo.library.ex.elseNull
import com.angcyo.library.ex.it
import com.angcyo.widget.DslViewHolder
import com.angcyo.widget.span.span

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/09/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class AppTextItem : DslAdapterItem() {

    var packageName: String? = null
    var appName: String? = null

    init {
        itemLayoutId = R.layout.app_text_item
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        super.onItemBind(itemHolder, itemPosition, adapterItem, payloads)

        itemHolder.tv(R.id.lib_text_view)?.text = span {
            packageName?.appBean()?.it {
                append(appName ?: it.appName)
                append(":")
                append(it.versionName)

                if (vmApp<AdaptiveModel>().getAdaptiveInfo(it.packageName) == null) {
                    //未找到适配信息
                    append(" ×")
                }
            }.elseNull {
                append(appName)
                append(":未安装")
            }
        }
    }
}