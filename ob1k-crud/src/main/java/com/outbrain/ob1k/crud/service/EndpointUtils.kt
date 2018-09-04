package com.outbrain.ob1k.crud.service

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.outbrain.ob1k.Response
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.server.netty.ResponseBuilder


private val jsonParser = JsonParser()

fun Entities<JsonObject>.toResponse(range: IntRange): Response {
    val arrOfData = JsonArray()
    val result = JsonObject()
    data.forEach { arrOfData.add(it) }

    result.addProperty("total", total)
    result.add("data", arrOfData)
    return ResponseBuilder.ok()
            .setHeader("content-range", "${range.first}-${Math.min(range.last, data.size)}/$total")
            .setHeader("content-type", "application/json")
            .withContent(data.toString())
            .build()
}

fun JsonObject?.toResponse() = ResponseBuilder.ok()
        .setHeader("content-type", "application/json")
        .withContent(toString())
        .build()

fun String.range() = clean().split(",").map { it.toInt() }.zipWithNext().map { IntRange(it.first, it.second) }[0]
fun String.unqoutedPair() = clean().split(",").map { it.unquote() }.zipWithNext()[0]
fun String.json() = jsonParser.parse(this).asJsonObject
fun JsonObject.ids(): List<String>? {
    val entrySet = entrySet()
    if (entrySet.size != 1) {
        return null
    }
    val key = entrySet.iterator().next().key
    if (key.unquote() != "id") {
        return null
    }
    val value = get(key)
    return if (value.isJsonArray) value.asJsonArray.asListOfStrings() else null
}

private fun JsonArray.asListOfStrings(): List<String> {
    val lst = mutableListOf<String>()
    for (i in 0 until size()) {
        val value = get(i).asJsonPrimitive.asString
        lst += value.unquote()
    }
    return lst
}

private fun String.clean() = removePrefix("[").removeSuffix("]")
private fun String.unquote() = removeSurrounding("\"")