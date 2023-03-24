package com.angcyo.websocket.data

import com.angcyo.websocket.WSClient

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2023/03/24
 * Copyright (c) 2020 angcyo. All rights reserved.
 */
data class WSServerMessageInfo(
    val client: WSClient,
    /**消息*/
    val message: String? = null,
    /**消息字节数组, 不为空时, 优先使用此数据*/
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WSServerMessageInfo

        if (client != other.client) return false
        if (message != other.message) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = client.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}