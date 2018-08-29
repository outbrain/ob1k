package com.outbrain.ob1k.crud

import com.google.gson.JsonObject
import com.outbrain.ob1k.concurrent.ComposableFuture
import com.outbrain.ob1k.concurrent.ComposableFutures
import com.outbrain.ob1k.crud.dao.*
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.crud.model.EntityField
import com.outbrain.ob1k.crud.model.Model
import com.outbrain.ob1k.crud.service.CrudService
import com.outbrain.ob1k.crud.service.ModelService
import com.outbrain.ob1k.db.BasicDao
import java.util.concurrent.ExecutorService

class CrudApplication(private val dao: BasicDao? = null,
                      commaDelimitedTables: String = "") {
    private val tables = commaDelimitedTables.split(",").filter { it.isNotEmpty() }
    var model = Model()

    init {
        if (commaDelimitedTables.isNotBlank()) {
            dao?.let { withTableInfo(dao, tables).get() }
            dao?.let { updateTableReferences(dao).get() }
        }
    }

    private fun withTableInfo(dao: BasicDao, tables: List<String>) = ComposableFutures.foreach(tables, this, { table, _ -> withTableInfo(dao, table) })

    private fun withTableInfo(dao: BasicDao, table: String): ComposableFuture<CrudApplication> {
        return dao.list("SHOW COLUMNS FROM $table", EntityFieldMapper())
                .map { EntityDescription(table = table, id = model.total, fields = it) }
                .map { withEntity(it) }
    }

    private fun updateTableReferences(dao: BasicDao) =
            dao.list("SELECT TABLE_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME " +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                    "WHERE REFERENCED_TABLE_NAME IS NOT NULL AND " +
                    "TABLE_NAME IN (${tables.map { "'$it'" }.joinToString(",")})", RefFieldMapper(model))

    fun withEntity(resourceName: String, fields: List<EntityField>) = withEntity(EntityDescription(table = "_$resourceName", id = model.total, fields = fields))

    fun withEntity(it: EntityDescription): CrudApplication {
        model = model.withEntity(it)
        return this
    }

    fun <T> withEntity(it: Class<T>): CrudApplication {
        model = model.withEntity(it)
        return this
    }

    fun addReference(from: String, to: String) = addReference(from, to, to)
    fun addOneToOneReference(from: String, to: String) = addOneToOneReference(from, to, to)
    fun addOneToManyReference(from: String, to: String) = addOneToManyReference(from, to, "${to}s")

    fun addReference(from: String, to: String, by: String): CrudApplication {
        get(from).add2DirectionReferenceTo(get(to), by)
        return this
    }

    fun addOneToOneReference(from: String, to: String, by: String): CrudApplication {
        get(from).addOneToOneference(get(to), by)
        return this
    }

    fun addOneToManyReference(from: String, to: String, by: String): CrudApplication {
        get(from).addOneToManyReference(get(to), by)
        return this
    }

    fun newMySQLDao(table: String) = MySQLCrudDao(model.getByTable(table)!!, dao!!)

    fun <T> newCustomDao(dao: ICrudAsyncDao<T>, dateformat: String) = CrudAsyncDaoDelegate(get(dao.resourceName()), dao, dateformat)

    fun <T> newCustomDao(dao: ICrudDao<T>, dateformat: String, executorService: ExecutorService) = newCustomDao(AsyncDaoBridge(dao, executorService), dateformat)

    fun mysqlTablesAsServices() = dao?.let { tables.map { newMySQLDao(it) }.map { service(it) } } ?: listOf()

    fun <T> autoGenerateCustomDaoService(dateformat: String, dao: ICrudAsyncDao<T>) = CrudService(newCustomDao(dao, dateformat))

    fun daosAsServices(dateformat: String, daos: List<ICrudAsyncDao<*>>) = daos.map { CrudService(newCustomDao(it, dateformat)) }

    fun service(dao: ICrudAsyncDao<JsonObject>) = CrudService(dao)

    fun modelService() = ModelService(model)

    operator fun invoke(resourceName: String, fieldName: String) = model(resourceName, fieldName)
    operator fun invoke(resourceName: String) = model(resourceName)
    fun get(resourceName: String, fieldName: String) = this(resourceName, fieldName)!!
    fun get(resourceName: String) = this(resourceName)!!
}

