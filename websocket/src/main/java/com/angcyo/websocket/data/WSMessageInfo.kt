package com.angcyo.websocket.data

/**
 * 收到客户端的消息
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/19
 */
data class WSMessageInfo(
    val clientInfo: WSClientInfo,
    /**消息*/
    val message: String? = null,
    /**消息字节数组, 不为空时, 优先使用此数据*/
    val bytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WSMessageInfo

        if (clientInfo != other.clientInfo) return false
        if (message != other.message) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clientInfo.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}
