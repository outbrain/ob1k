package com.outbrain.ob1k.crud.filter

import com.outbrain.ob1k.HttpRequestMethodType
import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.common.filters.AsyncFilter
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.security.server.AuthenticationCookieAesEncryptor
import com.outbrain.ob1k.server.ctx.AsyncServerRequestContext
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger(CrudAuditFilter::class.java)

val loggedMethods = setOf(HttpRequestMethodType.POST, HttpRequestMethodType.PUT, HttpRequestMethodType.DELETE)

class CrudAuditFilter(private val encryptor: AuthenticationCookieAesEncryptor,
                      private vararg val callbacks: (username: String?, request: Request) -> Unit) : AsyncFilter<Response, AsyncServerRequestContext> {


    override fun handleAsync(ctx: AsyncServerRequestContext): ComposableFuture<Response> {
        val username = ctx.username()
        val request = ctx.request
        callbacks.forEach { it(username, request) }
        if (loggedMethods.contains(request.method)) {
            logger.info("$username ${request.method} ${request.path}")
        }
        return ctx.invokeAsync()
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
