package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.concurrent.ComposableFutures
import com.outbrain.ob1k.crud.*
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
        val query = "select * from (select * from ${desc.table} ${filter.where()} ${sort.orderBy()} ${pagination.limit()}) as ${desc.table} ${desc.leftJoin()}"
        val count = "select count(*) from ${desc.table} ${filter.where()}"
        val countFuture = basicDao.get(count).map { it.values.first().toString().toInt() }
        val listFuture = basicDao.query(query)
        return countFuture.flatMap { total -> listFuture.map { Entities(total, it) } }.onFailure(query)
    }

    override fun list(ids: List<String>): ComposableFuture<Entities<JsonObject>> {
        if (ids.isEmpty()) return ComposableFutures.fromValue(Entities())
        val idField = desc.idField()
        val query = "select * from ${desc.table} ${desc.leftJoin()} where ${idField.dbName} in (${ids.map { it.unqoute() }.joinToString(",")})"
        val count = "select count(*) from ${desc.table}"
        val countFuture = basicDao.get(count).map { it.values.first().toString().toInt() }
        val listFuture = basicDao.query(query)
        return countFuture.flatMap { total -> listFuture.map { Entities(total, it) } }.onFailure(query)
    }

    override fun read(id: String): ComposableFuture<JsonObject?> {
        val query = desc.read(id)
        return basicDao.query(query).map { it.lastOrNull() }.onFailure(query)
    }

    override fun create(entity: JsonObject) = desc.insertRecursive(entity)

    override fun update(id: String, entity: JsonObject) = read(id).flatMap {
        if (it == null) desc.insertRecursive(entity)
        else desc.updateRecursive(id.toLong(), entity, it)
    }

    override fun resourceName() = desc.resourceName

    override fun type() = JsonObject::class.java

    private fun EntityDescription.insertRecursive(entity: JsonObject): ComposableFuture<JsonObject> {
        return executeInsert(entity).flatMap { inserted ->
            shallowReferences().map { refMeta ->
                val references = entity.getList(refMeta.fromField)
                if (references == null) {
                    inserted.asFuture()
                } else {
                    references.asSequence()
                            .map { it.with(refMeta.toField, inserted.id()) }
                            .map { refMeta.toDesc.insertRecursive(it) }
                            .toList()
                            .mapAll { inserted.with(refMeta.fromField, it) }
                }
            }.mapAll { inserted }
        }
    }

    private fun EntityDescription.executeInsert(entity: JsonObject): ComposableFuture<JsonObject> {
        val query = insert(entity)
        return basicDao.executeAndGetId(query).map { entity.with("id", it) }.onFailure(query)
    }

    private fun EntityDescription.updateRecursive(id: Long, entity: JsonObject, orig: JsonObject): ComposableFuture<JsonObject> {
        return executeUpdate(id, entity).flatMap { updated ->
            shallowReferences().map { refMeta ->
                val references = entity.getList(refMeta.fromField) ?: emptyList()
                val origReferences = orig.getList(refMeta.fromField) ?: emptyList()

                val toAdd = references.filter { it.id() == null }
                val toUpdate = references.filter { it.id() != null }
                val toDelete = origReferences.filter { e -> toUpdate.none { it.id() == e.id() } }
                val addFuture = toAdd.map { it.with(refMeta.toField, id) }.map { refMeta.toDesc.insertRecursive(it) }.mapAll { updated }
                val updateFuture = toUpdate.map { e -> refMeta.toDesc.updateRecursive(e.id()!!, e, origReferences.find { it.id() == e.id() }!!) }.mapAll { updated }
                val deleteFuture = toDelete.map { refMeta.toDesc.deleteRecursive(it) }.mapAll { updated }

                addFuture.flatMap { updateFuture }.flatMap { deleteFuture }

            }.mapAll { updated }
        }
    }

    private fun EntityDescription.executeUpdate(id: Any, entity: JsonObject): ComposableFuture<JsonObject> {
        val query = update(id, entity)
        return basicDao.execute(query).map { entity }.onFailure(query)
    }

    private fun EntityDescription.shallowReferences() = deepReferences().filter { it.fromDesc == desc }

    override fun delete(id: String) = read(id).flatMap { it?.let { desc.deleteRecursive(it) } ?: 0.asFuture() }

    private fun EntityDescription.deleteRecursive(entity: JsonObject): ComposableFuture<Int> {
        return shallowReferences()
                .map { refMeta ->
                    val references = entity.getList(refMeta.fromField) ?: emptyList()
                    references.asSequence()
                            .map { refMeta.toDesc.deleteRecursive(it) }
                            .toList()
                            .mapAll { it.sum() }
                }
                .mapAll { it.sum() }.flatMap { n ->
                    executeDelete(entity.id()!!).map { it + n }
                }
    }

    private fun EntityDescription.executeDelete(id: Any): ComposableFuture<Int> {
        val query = "DELETE from $table ${whereEq(id)}"
        return basicDao.execute(query).map { it.toInt() }.onFailure(query)
    }

    private fun JsonObject.where(): String {
        val filteredFields = desc.fields.map { it to get(it.name) }.filter { it.second != null }.filter { !it.second.isJsonNull }
        if (filteredFields.isEmpty()) {
            return ""
        }
        val matchExpressions = filteredFields.map { "${it.first.dbName}${it.first.toMysqlMatchValue(it.second.toString().unqoute())}" }
        return matchExpressions.joinToString(separator = " AND ", prefix = "where ")
    }

    private fun EntityDescription.read(id: String) = "select * from $table ${leftJoin()} ${whereEq(id)}"

    private fun EntityDescription.update(id: Any, jsonObject: JsonObject): String {
        val keyval = fields.asSequence()
                .filter { !it.readOnly }
                .filter { it.type != EFieldType.REFERENCEMANY }
                .filter { it.type != EFieldType.LIST }
                .distinctBy { it.dbName }
                .map { it to it.toSQLValue(jsonObject) }
                .map { it.first.dbName to it.second }
                .joinToString(",") { "${it.first}=${it.second}" }

        return "UPDATE $table SET $keyval ${whereEq(id)}"
    }

    private fun EntityDescription.insert(jsonObject: JsonObject): String {
        val jsonValues = fields
                .asSequence()
                .filter { !it.autoGenerate }
                .filter { it.type != EFieldType.REFERENCEMANY }
                .filter { it.type != EFieldType.LIST }
                .distinctBy { it.dbName }
                .map { it to it.toSQLValue(jsonObject) }
                .map { it.first.dbName to it.second }

        val cols = jsonValues.joinToString(",") { it.first }
        val values = jsonValues.joinToString(",") { it.second }
        return "INSERT INTO $table ($cols) VALUES($values)"
    }

    private fun IBasicDao.query(query: String) = list(query, JsonObjectMapper(desc)).map { it.distinct() }

    private fun EntityDescription.whereEq(id: Any) = "where ${idDBFieldName()}=$id"


    private fun EntityDescription.leftJoin() = deepReferences()
            .map { " left join ${it.toDesc.table} on ${it.fromDesc.idDBFieldName()}=${it.toField.dbName}" }
            .joinToString(separator = " ")


    private fun Pair<String, String>.orderBy() = "order by ${desc(first.unqoute())?.dbName
            ?: desc.idDBFieldName()} ${second.unqoute()}"

    private fun IntRange.limit() = "limit $first,${last - first + 1}"

    private fun String.unqoute() = removeSurrounding("\"")


    private fun JsonObject.filterAll() = entrySet().any { it.value.isJsonPrimitive && it.value.asString == "[]" }

    private fun <T> ComposableFuture<T>.onFailure(query: String) = recoverWith { ComposableFutures.fromError(RuntimeException("failed to execute $query:${it.message}", it)) }

    private fun EntityField.toSQLValue(json: JsonObject) = json.get(name)?.let { if (it.isJsonNull) "\'NULL\'" else toMysqlValue(it.asString) }
            ?: "\'NULL\'"
}


