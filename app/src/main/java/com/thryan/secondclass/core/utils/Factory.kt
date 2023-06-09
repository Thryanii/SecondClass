package com.thryan.secondclass.core.utils

import com.thryan.secondclass.core.result.VpnInfo
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import com.thryan.secondclass.core.result.HttpResult

class Factory(val mode: String) {
    inline fun <reified T> convert(value: String): HttpResult<T> =
        when (mode) {
            "XML" -> {
                val message = if (value.contains("Message")) value.substringAfter("<Message>")
                    .substringBefore("</Message>").replace("<![CDATA[", "").replace("]]>", "")
                else throw Exception("Webvpn验证失败")
                val twfid = value.substringAfter("<TwfID>").substringBefore("</TwfID>")

                if (value.contains("CSRF_RAND_CODE") && value.contains("RSA_ENCRYPT_KEY")) {
                    val csrf_rand_code = value.substringAfter("<CSRF_RAND_CODE>")
                        .substringBefore("</CSRF_RAND_CODE>")
                    val rsa_encrypt_key = value.substringAfter("<RSA_ENCRYPT_KEY>")
                        .substringBefore("</RSA_ENCRYPT_KEY>")
                    HttpResult(
                        message,
                        VpnInfo(twfid, csrf_rand_code, rsa_encrypt_key) as T
                    )
                } else {
                    HttpResult(message, twfid as T)
                }
            }

            "JSON" -> {
                if (value.contains("Server internal error")) throw Exception("500 Server internal error")
                if (value.contains("<html>")) throw Exception("webvpn登录失败")
                val json = Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                }
                //try {
                json.decodeFromString(value.trimIndent())
//                } catch (e: Exception) {
//                    throw Exception("返回参数错误")
//                }
            }

            else -> throw Exception("未知的类型")
        }

}
