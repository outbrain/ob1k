package com.outbrain.ob1k.crud.dao

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.crud.model.EntityDescription


class CrudAsyncDaoDelegate<T>(private val desc: EntityDescription,
                              private val delegate: ICrudAsyncDao<T>,
                              dateformat: String = "yyyy-MM-dd") : ICrudAsyncDao<JsonObject> {
    private val gson = GsonBuilder().setDateFormat(dateformat).create()
    private val type = delegate.type()

    override fun list(pagination: IntRange, sort: Pair<String, String>, filter: JsonObject?) =
            delegate.list(pagination, sort, filter?.asT()).map { it.asJson() }

    override fun list(ids: List<String>) = delegate.list(ids).map { it.asJson() }

    override fun read(id: String) = delegate.read(id).map { it?.asJson() }

    override fun create(entity: JsonObject) = delegate.create(entity.asT()).map { it.asJson() }

    override fun update(id: String, entity: JsonObject) = delegate.update(id, entity.asT()).map { it.asJson() }

    override fun delete(id: String) = delegate.delete(id)

    override fun resourceName() = delegate.resourceName()

    override fun type() = JsonObject::class.java

    private fun JsonObject.asT(): T {
        desc.fields.filter { it.type == EFieldType.REFERENCEMANY }.forEach { remove(it.name) }
        return gson.fromJson(this, type)
    }

    private fun Entities<T>.asJson() = Entities(total, data.map { it.asJson() })

    private fun T.asJson(): JsonObject {
        val jsonObject = gson.toJsonTree(this, type).asJsonObject
        desc.fields
                .filter { it.type == EFieldType.REFERENCEMANY }
                .forEach {
                    val name = it.name
                    val jsonElement = jsonObject.remove(name)
                    jsonElement?.let { jsonObject.addProperty(name, jsonElement.toString()) }
                }
        return jsonObject
    }

}