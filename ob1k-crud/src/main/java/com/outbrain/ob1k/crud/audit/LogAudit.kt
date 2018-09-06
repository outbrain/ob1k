package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.HttpRequestMethodType
import com.outbrain.ob1k.Request
import org.slf4j.LoggerFactory

class LogAudit : ICrudAudit {
    private val logger = LoggerFactory.getLogger(LogAudit::class.java)
    var methods: Set<HttpRequestMethodType> = setOf(HttpRequestMethodType.POST, HttpRequestMethodType.PUT, HttpRequestMethodType.DELETE)

    override fun audit(username: String?, request: Request) {
        if (methods.contains(request.method)) {
            logger.info("$username ${request.method} ${request.path}")
        }
    }
}