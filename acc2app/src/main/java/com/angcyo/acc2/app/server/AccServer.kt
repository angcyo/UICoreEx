package com.angcyo.acc2.app.server

import android.content.Context
import com.angcyo.server.AndServerService
import com.angcyo.server.DslAndServer

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/05
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class AccServer : AndServerService() {
    override fun initServer() {
        super.initServer()
    }
}

fun Context.startAccServer() {
    DslAndServer.startServer(this, AccServer::class.java)
}

fun Context.stopAccServer() {
    DslAndServer.stopServer(this, AccServer::class.java)
}