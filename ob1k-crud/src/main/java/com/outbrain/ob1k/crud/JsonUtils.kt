package com.outbrain.ob1k.crud

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.model.EntityField


internal fun JsonObject.with(name: String, value: Number?): JsonObject {
    addProperty(name, value)
    return this
}

internal fun JsonObject.with(name: String, value: String?): JsonObject {
    addProperty(name, value)
    return this
}

internal fun JsonObject.with(name: String, value: Boolean?): JsonObject {
    addProperty(name, value)
    return this
}

internal fun JsonObject.with(name: String, value: JsonArray?): JsonObject {
    add(name, value)
    return this
}

internal fun JsonObject.with(name: String, value: List<JsonObject>?): JsonObject {
    value?.let {
        val arr = JsonArray()
        it.forEach { arr.add(it) }
        add(name, arr)
    }
    return this
}

internal fun JsonObject.with(field: EntityField, value: Number?) = with(field.name, value)
internal fun JsonObject.with(field: EntityField, value: String?) = with(field.name, value)
internal fun JsonObject.with(field: EntityField, value: Boolean?) = with(field.name, value)
internal fun JsonObject.with(field: EntityField, value: JsonArray?) = with(field.name, value)
internal fun JsonObject.with(field: EntityField, value: List<JsonObject>?) = with(field.name, value)
internal fun JsonObject.getInt(field: EntityField) = getInt(field.name)
internal fun JsonObject.getLong(field: EntityField) = getLong(field.name)
internal fun JsonObject.getString(field: EntityField) = getString(field.name)
internal fun JsonObject.getBoolean(field: EntityField) = getBoolean(field.name)
internal fun JsonObject.getList(field: EntityField) = getList(field.name)
internal fun JsonObject.id() = getLong("id")
internal fun JsonObject.getInt(name: String) = get(name)?.asInt
internal fun JsonObject.getLong(name: String) = get(name)?.asLong
internal fun JsonObject.getString(name: String) = get(name)?.toString()
internal fun JsonObject.getBoolean(name: String) = get(name)?.asBoolean

internal fun JsonObject.getList(name: String): List<JsonObject>? {
    val jsonArray = getAsJsonArray(name) ?: return null
    return jsonArray.map { it.asJsonObject }
}