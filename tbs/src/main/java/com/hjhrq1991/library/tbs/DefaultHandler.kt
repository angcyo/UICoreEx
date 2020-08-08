package com.hjhrq1991.library.tbs

import com.angcyo.library.L

class DefaultHandler : BridgeHandler {

    override fun handler(data: String?, function: CallBackFunction?) {
        L.d("DefaultHandler data:$data function:$function")
        function?.onCallBack("DefaultHandler response data:$data")
    }
}