package com.angcyo.bluetooth.fsc

import androidx.lifecycle.ViewModel
import com.angcyo.http.tcp.Tcp
import com.angcyo.viewmodel.IViewModel

/**
 * WIFI收发指令
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2023/05/06
 */
class WifiApiModel : ViewModel(), IViewModel {

    val tcp = Tcp()

}