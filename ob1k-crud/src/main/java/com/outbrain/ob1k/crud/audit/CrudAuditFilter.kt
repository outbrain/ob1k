package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.common.filters.AsyncFilter
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.security.server.AuthenticationCookieAesEncryptor
import com.outbrain.ob1k.server.ctx.AsyncServerRequestContext
import org.slf4j.LoggerFactory


class CrudAuditFilter(private val encryptor: AuthenticationCookieAesEncryptor,
                      private val callbacks: List<ICrudAudit> = listOf(LogAudit())) : AsyncFilter<Response, AsyncServerRequestContext> {

    private val logger = LoggerFactory.getLogger(CrudAuditFilter::class.java)

    override fun handleAsync(ctx: AsyncServerRequestContext): ComposableFuture<Response> {
        val username = ctx.username()
        val request = ctx.request
        return ctx.invokeAsync<Response>().map {
            callbacks.forEach { it.audit(username, request) }
            it
        }
    }


    private fun AsyncServerRequestContext.username() = request.username()


    private fun Request.username(): String? {
        val encodedCookie = getCookie("ob1k-session")
        if (encodedCookie.isNullOrEmpty()) return null
        return try {
            encryptor.decrypt(encodedCookie).username
        } catch (e: Exception) {
            logger.warn("Error decrypting cookie for request $this")
            null
        }
    }
}
