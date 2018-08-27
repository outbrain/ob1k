package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.concurrent.ComposableFutures
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.crud.model.EntityField
import com.outbrain.ob1k.db.BasicDao

class MySQLCrudDao(private val desc: EntityDescription, private val basicDao: BasicDao) : ICrudAsyncDao<JsonObject> {

    override fun list(pagination: IntRange,
                      sort: Pair<String, String>,
                      filter: JsonObject): ComposableFuture<Entities<JsonObject>> {

        if (filter.filterAll()) return ComposableFutures.fromValue(Entities())
        val query = "${desc.select()} ${desc.from()} ${desc.join()} ${filter.where()} ${desc.groupBy()} ${sort.orderBy()} ${pagination.limit()}"
        val count = "select count(*) ${desc.from()} ${filter.where()}"
        val countFuture = basicDao.get(count).map { it.values.first().toString().toInt() }
        val listFuture = basicDao.query(query)
        return countFuture.flatMap { total -> listFuture.map { Entities(total, it) } }.onFailure(query)
    }

    override fun read(id: Int): ComposableFuture<JsonObject?> {
        val query = "${desc.select()} ${desc.from()} ${desc.join()} ${desc.whereEq(id)} ${desc.groupBy()}"
        return basicDao.query(query).map { it.lastOrNull() }.onFailure(query)
    }

    override fun create(entity: JsonObject): ComposableFuture<JsonObject> {
        val query = desc.insert(entity)
        return basicDao.executeAndGetId(query).map { entity.withId(it) }.onFailure(query)
    }

    override fun update(id: Int, entity: JsonObject): ComposableFuture<JsonObject> {
        val query = desc.update(id, entity)
        return basicDao.execute(query).map { entity }.onFailure(query)
    }

    override fun delete(id: Int): ComposableFuture<Int> {
        val query = "DELETE ${desc.from()} ${desc.whereEq(id)}"
        return basicDao.execute(query).map { it.toInt() }.onFailure(query)
    }

    override fun resourceName() = desc.resourceName

    override fun type() = JsonObject::class.java

    private fun JsonObject.where(): String {
        val filteredFields = desc.fields.map { it to get(it.name) }.filter { it.second != null }.filter { !it.second.isJsonNull }
        if (filteredFields.isEmpty()) {
            return ""
        }
        val matchExpressions = filteredFields.map { "${it.first.dbName}${it.first.type.toMysqlMatchValue(it.second.toString().unqoute())}" }
        return matchExpressions.joinToString(separator = " AND ", prefix = "where ")
    }

    private fun EntityDescription.update(id: Int, jsonObject: JsonObject): String {
        val keyval = editableFields()
                .map { it to it.toSQLValue(jsonObject) }
                .map { it.first.dbName to it.second }
                .joinToString(",") { "${it.first}=${it.second}" }
        return "UPDATE $table SET $keyval ${whereEq(id)}"
    }

    private fun EntityDescription.insert(jsonObject: JsonObject): String {
        val jsonValues = fields
                .filter { !it.autoGenerate }
                .filter { it.type != EFieldType.REFERENCEMANY }
                .map { it to it.toSQLValue(jsonObject) }
                .map { it.first.dbName to it.second }

        val cols = jsonValues.joinToString(",") { it.first }
        val values = jsonValues.joinToString(",") { it.second }
        return "INSERT INTO $table ($cols) VALUES($values)"
    }

    private fun BasicDao.query(query: String) = list(query, JsonObjectMapper(desc))

    private fun EntityDescription.from() = "from $table"

    private fun EntityDescription.whereEq(id: Int) = "where ${idDBFieldName()}=$id"

    private fun EntityDescription.join() = references.map { "left join ${it.table} on ${idDBFieldName()}=${it.referenceTo(resourceName)!!.dbName}" }.joinToString(" ")

    private fun EntityDescription.groupBy() = if (references.isEmpty()) "" else "group by ${desc.idDBFieldName()}"

    private fun EntityDescription.select() = "select ${desc.table}.* ${groupConcatReferences()}"

    private fun EntityDescription.groupConcatReferences() = if (references.isEmpty()) "" else references.map { it.groupConcat() }.joinToString(separator = ",", prefix = ",")

    private fun EntityDescription.groupConcat() = "GROUP_CONCAT(${idDBFieldName()}) as ${desc.referenceTo(resourceName)!!.name}"

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

    private fun EntityField.toSQLValue(json: JsonObject) = json.get(name)?.let { if (it.isJsonNull) "\'NULL\'" else type.toMysqlValue(it.asString) }
            ?: "\'NULL\'"
}

