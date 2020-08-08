package com.hjhrq1991.library.tbs

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

/**
 * data of bridge
 * @author haoqing
 */
class Message {
    var callbackId //callbackId
            : String? = null
    var responseId //responseId
            : String? = null
    var responseData //responseData
            : String? = null
    var data //data of message
            : String? = null
    var handlerName //name of handler
            : String? = null

    fun toJson(): String? {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(
                CALLBACK_ID_STR,
                callbackId
            )
            jsonObject.put(DATA_STR, data)
            jsonObject.put(
                HANDLER_NAME_STR,
                handlerName
            )
            jsonObject.put(
                RESPONSE_DATA_STR,
                responseData
            )
            jsonObject.put(
                RESPONSE_ID_STR,
                responseId
            )
            return jsonObject.toString()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        private const val CALLBACK_ID_STR = "callbackId"
        private const val RESPONSE_ID_STR = "responseId"
        private const val RESPONSE_DATA_STR = "responseData"
        private const val DATA_STR = "data"
        private const val HANDLER_NAME_STR = "handlerName"
        fun toObject(jsonStr: String?): Message {
            val m = Message()
            try {
                val jsonObject = JSONObject(jsonStr)
                m.handlerName = if (jsonObject.has(HANDLER_NAME_STR)) jsonObject.getString(
                    HANDLER_NAME_STR
                ) else null
                m.callbackId = if (jsonObject.has(CALLBACK_ID_STR)) jsonObject.getString(
                    CALLBACK_ID_STR
                ) else null
                m.responseData = if (jsonObject.has(RESPONSE_DATA_STR)) jsonObject.getString(
                    RESPONSE_DATA_STR
                ) else null
                m.responseId = if (jsonObject.has(RESPONSE_ID_STR)) jsonObject.getString(
                    RESPONSE_ID_STR
                ) else null
                m.data = if (jsonObject.has(DATA_STR)) jsonObject.getString(
                    DATA_STR
                ) else null
                return m
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return m
        }

        @JvmStatic
        fun toArrayList(jsonStr: String?): List<Message> {
            val list: MutableList<Message> =
                ArrayList()
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val m = Message()
                    val jsonObject = jsonArray.getJSONObject(i)
                    m.handlerName = if (jsonObject.has(HANDLER_NAME_STR)) jsonObject.getString(
                        HANDLER_NAME_STR
                    ) else null
                    m.callbackId = if (jsonObject.has(CALLBACK_ID_STR)) jsonObject.getString(
                        CALLBACK_ID_STR
                    ) else null
                    m.responseData = if (jsonObject.has(RESPONSE_DATA_STR)) jsonObject.getString(
                        RESPONSE_DATA_STR
                    ) else null
                    m.responseId = if (jsonObject.has(RESPONSE_ID_STR)) jsonObject.getString(
                        RESPONSE_ID_STR
                    ) else null
                    m.data = if (jsonObject.has(DATA_STR)) jsonObject.getString(
                        DATA_STR
                    ) else null
                    list.add(m)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return list
        }
    }
}