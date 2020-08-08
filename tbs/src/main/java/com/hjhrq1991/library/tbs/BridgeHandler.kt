package com.hjhrq1991.library.tbs

interface BridgeHandler {
    fun handler(
        data: String?,
        function: CallBackFunction?
    )
}