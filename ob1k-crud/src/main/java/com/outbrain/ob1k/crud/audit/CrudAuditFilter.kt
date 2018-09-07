package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.common.filters.AsyncFilter
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.security.server.AuthenticationCookieAesEncryptor
import com.outbrain.ob1k.security.server.BasicAuthenticationHeaderParser
import com.outbrain.ob1k.server.ctx.AsyncServerRequestContext


class CrudAuditFilter(private val encryptor: AuthenticationCookieAesEncryptor) : AsyncFilter<Response, AsyncServerRequestContext> {

    var callbacks: List<ICrudAudit> = mutableListOf(LogAudit())
    private val headerParser = BasicAuthenticationHeaderParser()


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
            return headerParser.extractCredentials(this)?.get()?.username
        }
    }
}
