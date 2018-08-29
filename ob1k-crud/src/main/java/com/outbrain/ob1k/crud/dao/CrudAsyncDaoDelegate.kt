package com.outbrain.ob1k.crud.dao

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.crud.model.EntityDescription


class CrudAsyncDaoDelegate<T>(private val desc: EntityDescription,
                              private val delegate: ICrudAsyncDao<T>,
                              dateformat: String = "yyyy-MM-dd") : ICrudAsyncDao<JsonObject> {
    private val gson = GsonBuilder().setDateFormat(dateformat).create()
    private val type = delegate.type()

    override fun list(pagination: IntRange, sort: Pair<String, String>, filter: JsonObject): ComposableFuture<Entities<JsonObject>> {
        return delegate.list(pagination, sort, filter).map { Entities(it.total, it.data.map { type.jsonOf(it) }) }
    }


    override fun read(id: String) = delegate.read(id).map { it?.let { type.jsonOf(it) } }

    override fun create(entity: JsonObject) = delegate.create(type.typeOf(entity)).map { type.jsonOf(it) }

    override fun update(id: String, entity: JsonObject) = delegate.update(id, type.typeOf(entity)).map { type.jsonOf(it) }

    override fun delete(id: String) = delegate.delete(id)

    override fun resourceName() = delegate.resourceName()

    override fun type() = JsonObject::class.java

    private fun Class<T>.typeOf(obj: JsonObject): T {
        desc.fields.filter { it.type == EFieldType.REFERENCEMANY }.forEach { obj.remove(it.name) }
        return gson.fromJson(obj, this)
    }

    private fun Class<T>.jsonOf(t: T): JsonObject {
        val jsonObject = gson.toJsonTree(t, this).asJsonObject
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