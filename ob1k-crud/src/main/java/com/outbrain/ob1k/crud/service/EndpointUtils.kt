package com.outbrain.ob1k.crud.service

import com.google.gson.JsonParser

class EndpointUtils(private val jsonParser: JsonParser = JsonParser()) {
    fun sort(sort: String) = sort.clean().split(",").map { it.unquote() }.zipWithNext()[0]
    fun range(range: String) = range.clean().split(",").map { it.toInt() }.zipWithNext().map { IntRange(it.first, it.second) }[0]
    fun asJson(filter: String) = jsonParser.parse(filter).asJsonObject

    private fun String.clean() = removePrefix("[").removeSuffix("]")
    private fun String.unquote() = removeSurrounding("\"")
}