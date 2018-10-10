package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.concurrent.ComposableFutures
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.crud.model.EntityField
import com.outbrain.ob1k.db.IBasicDao

class MySQLCrudDao(private val desc: EntityDescription, private val basicDao: IBasicDao) : ICrudAsyncDao<JsonObject> {

    override fun list(pagination: IntRange,
                      sort: Pair<String, String>,
                      filter: JsonObject?): ComposableFuture<Entities<JsonObject>> {
        val filter = filter ?: JsonObject()
        if (filter.filterAll()) return ComposableFutures.fromValue(Entities())
        val query = "select * from ${desc.table} ${filter.where()} ${sort.orderBy()} ${pagination.limit()}"
        val count = "select count(*) from ${desc.table} ${filter.where()}"
        val countFuture = basicDao.get(count).map { it.values.first().toString().toInt() }
        val listFuture = basicDao.query(query)
        return countFuture.flatMap { total -> listFuture.map { Entities(total, it) } }.onFailure(query)
    }

    override fun list(ids: List<String>): ComposableFuture<Entities<JsonObject>> {
        if (ids.isEmpty()) return ComposableFutures.fromValue(Entities())
        val idField = desc.idField()
        val query = "select * from ${desc.table} where ${idField.dbName} in (${ids.map { it.unqoute() }.joinToString(",")})"
        val count = "select count(*) from ${desc.table}"
        val countFuture = basicDao.get(count).map { it.values.first().toString().toInt() }
        val listFuture = basicDao.query(query)
        return countFuture.flatMap { total -> listFuture.map { Entities(total, it) } }.onFailure(query)
    }

    override fun read(id: String): ComposableFuture<JsonObject?> {
        val query = "select * from ${desc.table} ${desc.whereEq(id)}"
        return basicDao.query(query).map { it.lastOrNull() }.onFailure(query)
    }

    override fun create(entity: JsonObject): ComposableFuture<JsonObject> {
        val query = desc.insert(entity)
        return basicDao.executeAndGetId(query).map { entity.withId(it) }.onFailure(query)
    }

    override fun update(id: String, entity: JsonObject): ComposableFuture<JsonObject> {
        val query = desc.update(id, entity)
        return basicDao.execute(query).map { entity }.onFailure(query)
    }

    override fun delete(id: String): ComposableFuture<Int> {
        val query = "DELETE from ${desc.table} ${desc.whereEq(id)}"
        return basicDao.execute(query).map { it.toInt() }.onFailure(query)
    }

    override fun resourceName() = desc.resourceName

    override fun type() = JsonObject::class.java

    private fun JsonObject.where(): String {
        val filteredFields = desc.fields.map { it to get(it.name) }.filter { it.second != null }.filter { !it.second.isJsonNull }
        if (filteredFields.isEmpty()) {
            return ""
        }
        val matchExpressions = filteredFields.map { "${it.first.dbName}${it.first.toMysqlMatchValue(it.second.toString().unqoute())}" }
        return matchExpressions.joinToString(separator = " AND ", prefix = "where ")
    }

    private fun EntityDescription.update(id: String, jsonObject: JsonObject): String {
        val keyval = fields.filter { !it.readOnly }
                .filter { it.type != EFieldType.REFERENCEMANY }
                .distinctBy { it.dbName }
                .map { it to it.toSQLValue(jsonObject) }
                .map { it.first.dbName to it.second }
                .joinToString(",") { "${it.first}=${it.second}" }
        return "UPDATE $table SET $keyval ${whereEq(id)}"
    }

    private fun EntityDescription.insert(jsonObject: JsonObject): String {
        val jsonValues = fields
                .filter { !it.autoGenerate }
                .filter { it.type != EFieldType.REFERENCEMANY }
                .distinctBy { it.dbName }
                .map { it to it.toSQLValue(jsonObject) }
                .map { it.first.dbName to it.second }

        val cols = jsonValues.joinToString(",") { it.first }
        val values = jsonValues.joinToString(",") { it.second }
        return "INSERT INTO $table ($cols) VALUES($values)"
    }

    private fun IBasicDao.query(query: String) = list(query, JsonObjectMapper(desc))

    private fun EntityDescription.whereEq(id: String) = "where ${idDBFieldName()}=$id"

    private fun Pair<String, String>.orderBy() = "order by ${desc(first.unqoute())?.dbName
            ?: desc.idDBFieldName()} ${second.unqoute()}"

    private fun IntRange.limit() = "limit $first,${last - first + 1}"

    private fun String.unqoute() = removeSurrounding("\"")

    private fun JsonObject.withId(id: Long): JsonObject {
        this.addProperty("id", id)
        return this
    }

    private fun JsonObject.filterAll() = entrySet().any { it.value.isJsonPrimitive && it.value.asString == "[]" }

    private fun <T> ComposableFuture<T>.onFailure(query: String) = recoverWith { ComposableFutures.fromError(RuntimeException("failed to execute $query", it)) }

    private fun EntityField.toSQLValue(json: JsonObject) = json.get(name)?.let { if (it.isJsonNull) "\'NULL\'" else toMysqlValue(it.asString) }
            ?: "\'NULL\'"
}

