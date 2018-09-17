package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.HttpRequestMethodType
import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response
import org.slf4j.LoggerFactory

class LogAudit : ICrudAudit {
    private val logger = LoggerFactory.getLogger(LogAudit::class.java)
    var methods: Set<HttpRequestMethodType> = mutableSetOf(HttpRequestMethodType.POST, HttpRequestMethodType.PUT, HttpRequestMethodType.DELETE)

    override fun audit(username: String?, request: Request, response: Response) {
        if (methods.contains(request.method)) {
            val created = when (request.method) {
                HttpRequestMethodType.POST -> ": ${response.rawContent?.replace("\\s".toRegex(), "") ?: ""}"
                else -> ""
            }
            logger.info("$username ${request.method} ${request.path} $created")
        }
    }
}