package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.After
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class CrudDaoTestBase {
    private val dao = personDao()
    private val email = "${Random().nextInt()}@outbrain.com"
    private val jsonParser = JsonParser()

    @After
    open fun tearDown() {
        dao.list().andThen({ entities -> entities.value.data.forEach { dao.delete(it.get("id").asInt).get() } }).get()
        //dao.list(filter = jsonParser.parse("{\"email\": \"$email\"}").asJsonObject).andThen({ it.value.data.forEach { dao.delete(it.get("id").asInt).get() } }).get()
    }

    @org.junit.Test
    internal fun `create person`() {
        val json = entity("create")
        val created = dao.create(json).get()
        val id = created.get("id").asInt
        assert(id > 0)
        created.addProperty("id", id)
        assertEqualsJson(json, created)
    }

    @org.junit.Test
    internal fun `update person`() {
        val created = dao.create(entity("update")).get()
        created.addProperty("alive", false)
        val updated = dao.update(created.get("id").asInt, created).get()
        assertEquals(false, updated.get("alive").asBoolean)
    }

    @org.junit.Test
    internal fun `read person`() {
        val created = dao.create(entity("read")).get()
        val read = dao.read(created.get("id").asInt).get()
        assertEqualsJson(created, read!!)
    }

    @org.junit.Test
    internal fun `delete person`() {
        val created = dao.create(entity("delete")).get()
        val id = created.get("id").asInt
        dao.delete(id).get()
        assertNull(dao.read(id).get())
    }

    @org.junit.Test
    internal fun `sort persons`() {
        (8 downTo 1).forEach { dao.create(entity("${it}sort")).get() }
        val entities1 = dao.list(sort = "name" to "ASC").get()
        (0..entities1.data.size - 2).forEach {
            val name1 = entities1.data[it].get("name").asString
            val name2 = entities1.data[it + 1].get("name").asString
            assertTrue(name1 < name2)
        }
        val entities2 = dao.list(sort = "id" to "DESC").get()
        (0..entities2.data.size - 2).forEach {
            val id1 = entities2.data[it].get("id").asInt
            val id2 = entities2.data[it + 1].get("id").asInt
            assertTrue(id1 > id2)
        }
    }

    @org.junit.Test
    internal fun `filter persons`() {
        (1..5).forEach { dao.create(entity("${it}filter1")).get() }
        (1..5).forEach { dao.create(entity("${it}filter2")).get() }
        assertEquals(10, dao.list(filter = jsonParser.parse("{\"name\": \"filter\",\"email\": \"$email\"}").asJsonObject).get().data.size)
        assertEquals(5, dao.list(filter = jsonParser.parse("{\"name\": \"filter1\",\"email\": \"$email\"}").asJsonObject).get().data.size)
    }

    @org.junit.Test
    internal fun `paginate persons`() {
        (1..10).forEach { dao.create(entity("paging")).get() }
        val entities = dao.list(pagination = 0..5).get()
        assertEquals(6, entities.data.size)
        assertTrue { entities.total > entities.data.size }
    }

    abstract fun personDao(): ICrudAsyncDao<JsonObject>

    private fun entity(testcase: String): JsonObject {
        val json = JsonObject()
        json.addProperty("name", "$testcase${Random().nextInt()}")
        json.addProperty("alive", true)
        json.addProperty("email", email)
        return json
    }

    private fun assertEqualsJson(expected: JsonObject, actual: JsonObject) = expected.entrySet().map { it.key }.forEach { assertEquals(expected.get(it), actual.get(it)) }
}