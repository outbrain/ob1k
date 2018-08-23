package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.concurrent.ComposableFutures
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.db.BasicDao

//private val logger = KotlinLogging.logger {}
class MySQLCrudDao(private val desc: EntityDescription, private val basicDao: BasicDao) : ICrudAsyncDao<JsonObject> {

    override fun list(pagination: IntRange,
                      sort: Pair<String, String>,
                      filter: JsonObject): ComposableFuture<Entities<JsonObject>> {
        if (filter.filterAll()) return ComposableFutures.fromValue(Entities())
        val select = "${desc.select()} ${desc.from()} ${desc.join()} ${filter.where()} ${desc.groupBy()} ${sort.orderBy()} ${pagination.limit()}"
        val count = "select count(*) ${desc.from()} ${filter.where()}"
        val countFuture = basicDao.get(count).map { it.values.first().toString().toInt() }
        val listFuture = basicDao.query(select)
        //  logger.info { count }
        //  logger.info { select }
        return countFuture.flatMap { total -> listFuture.map { Entities(total, it) } }.recoverWith { ComposableFutures.fromError(RuntimeException("failed to execute $select", it)) }
    }

    override fun read(id: Int): ComposableFuture<JsonObject?> {
        val query = "${desc.select()} ${desc.from()} ${desc.join()} ${desc.whereEq(id)} ${desc.groupBy()}"
        return basicDao.query(query).map { it.lastOrNull() }
    }

    override fun create(entity: JsonObject) = basicDao.executeAndGetId(desc.insert(entity)).map { entity.withId(it) }

    override fun update(id: Int, entity: JsonObject) = basicDao.execute(desc.update(id, entity)).map { entity }

    override fun delete(id: Int) = basicDao.execute("DELETE ${desc.from()} ${desc.whereEq(id)}").map { it.toInt() }

    override fun resourceName() = desc.resourceName

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
                .filter { jsonObject.get(it.name) != null }
                .map { "${it.dbName}=${it.type.toMysqlValue(jsonObject.get(it.name).asString)}" }
                .joinToString(",")
        return "UPDATE $table SET $keyval ${whereEq(id)}"
    }

    private fun EntityDescription.insert(jsonObject: JsonObject): String {
        val jsonValues = fields
                .filter { it.type != EFieldType.REFERENCEMANY }
                .filter { jsonObject.get(it.name) != null }
                .map { it.dbName to it.type.toMysqlValue(jsonObject.get(it.name).asString) }

        val cols = jsonValues.map { it.first }.joinToString(",")
        val values = jsonValues.map { it.second }.joinToString(",")
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
}
