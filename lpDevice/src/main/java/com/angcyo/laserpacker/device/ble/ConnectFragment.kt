package com.angcyo.laserpacker.device.ble

import android.os.Bundle
import com.angcyo.core.fragment.BaseDslFragment
import com.angcyo.laserpacker.device.ble.dslitem.ConnectHistoryItem
import com.angcyo.objectbox.laser.pecker.LPBox
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity
import com.angcyo.objectbox.laser.pecker.entity.DeviceConnectEntity_
import com.angcyo.objectbox.page

/**
 * 设备连接记录
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/01/05
 */
class ConnectFragment : BaseDslFragment() {

    init {
        fragmentTitle = "连接记录"
        enableAdapterRefresh = true
    }

    override fun initBaseView(savedInstanceState: Bundle?) {
        super.initBaseView(savedInstanceState)
    }

    override fun onLoadData() {
        super.onLoadData()
        val list = DeviceConnectEntity::class.page(page, LPBox.PACKAGE_NAME) {
            orderDesc(DeviceConnectEntity_.connectTime)
        }
        loadDataEnd(ConnectHistoryItem::class, list) { entity ->
            itemConnectEntity = entity
        }
    }
}