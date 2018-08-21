package com.outbrain.ob1k.crud.service

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.server.netty.ResponseBuilder

class EntitiesOfJsonToResponse(private var range: IntRange) : java.util.function.Function<Entities<JsonObject>, Response> {
    override fun apply(t: Entities<JsonObject>): Response {
        val data = JsonArray()
        val result = JsonObject()
        t.data.forEach { data.add(it) }
        result.addProperty("total", t.total)
        result.add("data", data)
        return ResponseBuilder.ok()
                .setHeader("content-range", "${range.first}-${Math.min(range.last, data.size())}/${t.total}")
                .setHeader("content-type", "application/json")
                .withContent(data.toString())
                .build()
    }
}