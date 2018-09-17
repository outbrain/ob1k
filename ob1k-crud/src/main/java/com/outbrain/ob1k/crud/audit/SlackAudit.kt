package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.HttpRequestMethodType
import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.http.HttpClient
import com.outbrain.ob1k.http.common.ContentType

class SlackAudit(private val httpClient: HttpClient,
                 private val url: String,
                 private val prefix: String) : ICrudAudit {

    var methods: Set<HttpRequestMethodType> = mutableSetOf(HttpRequestMethodType.POST, HttpRequestMethodType.PUT, HttpRequestMethodType.DELETE)

    override fun audit(username: String?, request: Request, response: Response) {
        username?.let {
            if (methods.contains(request.method)) {
                val created = when (request.method) {
                    HttpRequestMethodType.POST -> ": ${response.rawContent?.replace("\\s".toRegex(), "") ?: ""}"
                    else -> ""
                }
                val message = "$prefix $username ${request.method} ${request.uri} $created"
                httpClient.post(url).setContentType(ContentType.JSON).setBody(SlackMessage(message)).asResponse()
            }
        }
    }
}

data class SlackMessage(val text: String)