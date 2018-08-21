package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.crud.model.Entities
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.db.BasicDao

class MySQLCrudDao(private val desc: EntityDescription, private val basicDao: BasicDao) : ICrudAsyncDao<JsonObject> {
    private val mapper = JsonObjectMapper(desc)

    override fun list(pagination: IntRange,
                      sort: Pair<String, String>,
                      filter: JsonObject): ComposableFuture<Entities<JsonObject>> {
        val where = where(filter)
        val orderBy = orderBy(sort)
        val limit = "limit ${pagination.first},${pagination.last - pagination.first + 1}"
        val select = "select * from ${desc.table} $where $orderBy $limit"
        val count = "select count(*) from ${desc.table} $where"
        val countFuture = basicDao.get(count)
        val listFuture = basicDao.list(select, mapper)
        return countFuture.flatMap { total -> listFuture.map { Entities(total.values.first().toString().toInt(), it) } }
    }

    private fun orderBy(sort: Pair<String, String>): String {
        val idFieldName = desc.idFieldName()
        val fieldName = desc(sort.first.unqoute())
        return "order by ${fieldName?.dbName ?: idFieldName} ${sort.second.unqoute()}"
    }

    private fun where(filter: JsonObject): String {
        val filteredFields = desc.fields.map { it to filter.get(it.name) }.filter { it.second != null }.filter { !it.second.isJsonNull }
        if (filteredFields.isEmpty()) {
            return ""
        }
        val matchExpressions = filteredFields.map { "${it.first.dbName}${it.first.type.toMysqlMatchValue(it.second.toString().unqoute())}" }
        return "where ${matchExpressions.joinToString(" AND ")}"
    }

    override fun read(id: Int) = basicDao.list("select * from ${desc.table} where ${desc.idFieldName()}=$id", mapper).map { it.firstOrNull() }

    override fun create(entity: JsonObject) = basicDao.executeAndGetId(entity.asInsert()).map { entity.withId(it) }

    override fun update(id: Int, entity: JsonObject) = basicDao.execute(entity.asUpdate(id)).map { entity }

    override fun delete(id: Int) = basicDao.execute("DELETE FROM ${desc.table} WHERE ${desc.idFieldName()}=$id").map { id }

    override fun resourceName() = desc.resourceName

    private fun JsonObject.asInsert(): String {
        val jsonValues = desc.editableFields().map { it.dbName to it.type.toMysqlValue(get(it.name).asString) }
        val cols = jsonValues.map { it.first }.joinToString(",")
        val values = jsonValues.map { it.second }.joinToString(",")
        return "INSERT INTO ${desc.table} ($cols) VALUES($values)"
    }

    private fun JsonObject.asUpdate(id: Int): String {
        val keyval = desc.editableFields().map { "${it.dbName}=${it.type.toMysqlValue(get(it.name).asString)}" }.joinToString(",")
        return "UPDATE ${desc.table} SET $keyval WHERE ${desc.idFieldName()}=$id"
    }

    private fun String.unqoute() = removeSurrounding("\"")

    private fun JsonObject.withId(id: Long): JsonObject {
        this.addProperty("id", id)
        return this
    }
}