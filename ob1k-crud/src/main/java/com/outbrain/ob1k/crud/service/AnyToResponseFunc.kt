package com.outbrain.ob1k.crud.service

import com.outbrain.ob1k.Response
import com.outbrain.ob1k.server.netty.ResponseBuilder

class AnyToResponseFunc : java.util.function.Function<Any, Response> {
    override fun apply(any: Any) = ResponseBuilder.ok().setHeader("content-type","application/json").withContent(any.toString()).build()
}