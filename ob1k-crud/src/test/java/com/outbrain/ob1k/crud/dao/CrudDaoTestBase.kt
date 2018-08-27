package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.After
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class CrudDaoTestBase {
    private val daos = getPersonAndJobDaos();
    private val personDao = daos.first
    private val jobDao = daos.second
    private val email = "${Random().nextInt()}@outbrain.com"
    private val title = "${Random().nextInt()}QA"
    private val jsonParser = JsonParser()

    abstract fun getPersonAndJobDaos(): Pair<ICrudAsyncDao<JsonObject>, ICrudAsyncDao<JsonObject>>

    @After
    fun tearDown() {
        jobDao.list(filter = JsonObject().with("title", title)).andThen({ it.value.data.forEach { jobDao.delete(it.id()).get() } }).get()
        personDao.list(filter = JsonObject().with("email", email)).andThen({ it.value.data.forEach { personDao.delete(it.id()).get() } }).get()
    }

    @org.junit.Test
    internal fun `create person`() {
        val json = person("create")
        val created = personDao.create(json).get()
        val id = created.id()
        assert(id > 0)
        created.addProperty("id", id)
        assertEqualsJson(json, created)
    }

    @org.junit.Test
    internal fun `update person`() {
        val created = personDao.create(person("update")).get()
        created.addProperty("alive", false)
        val updated = personDao.update(created.id(), created).get()
        assertEquals(false, updated.get("alive").asBoolean)
    }

    @org.junit.Test
    internal fun `read person`() {
        val created = personDao.create(person("read")).get()
        val read = personDao.read(created.get("id").asInt).get()
        assertEqualsJson(created, read!!)
    }

    @org.junit.Test
    internal fun `delete person`() {
        val created = personDao.create(person("delete")).get()
        val id = created.id()
        personDao.delete(id).get()
        assertNull(personDao.read(id).get())
    }

    @org.junit.Test
    internal fun `sort persons`() {
        (8 downTo 1).forEach { personDao.create(person("${it}sort")).get() }
        val entities1 = personDao.list(sort = "name" to "ASC", filter = JsonObject().with("email", email)).get()
        (0..entities1.data.size - 2).forEach {
            val name1 = entities1.data[it].value("name")
            val name2 = entities1.data[it + 1].value("name")
            assertTrue(name1 < name2)
        }
        val entities2 = personDao.list(sort = "id" to "DESC").get()
        (0..entities2.data.size - 2).forEach {
            val id1 = entities2.data[it].id()
            val id2 = entities2.data[it + 1].id()
            assertTrue(id1 > id2)
        }
    }

    @org.junit.Test
    internal fun `filter persons`() {
        (1..5).forEach { personDao.create(person("${it}filter1")).get() }
        (1..5).forEach { personDao.create(person("${it}filter2")).get() }
        assertEquals(10, personDao.list(filter = jsonParser.parse("{\"name\": \"filter\",\"email\": \"$email\"}").asJsonObject).get().data.size)
        assertEquals(5, personDao.list(filter = jsonParser.parse("{\"name\": \"filter1\",\"email\": \"$email\"}").asJsonObject).get().data.size)
    }

    @org.junit.Test
    internal fun `paginate persons`() {
        (1..10).forEach { personDao.create(person("paging")).get() }
        val entities = personDao.list(pagination = 0..5).get()
        assertEquals(6, entities.data.size)
        assertTrue { entities.total > entities.data.size }
    }

    private fun assertEqualsJson(expected: JsonObject, actual: JsonObject) = expected.entrySet().map { it.key }.forEach { assertEquals(expected.get(it), actual.get(it)) }

    @Test
    internal fun `many jobs to a person`() {
        val id0 = personDao.create(person("manyToOne")).get().id()
        val id1 = personDao.create(person("manyToOne")).get().id()

        var job0 = jobDao.create(job("manyToOne", id0)).get()

        job0 = jobDao.read(job0.id()).get()!!
        assertEquals(id0, job0.int("person"))

        job0.addProperty("person", id1)
        job0 = jobDao.update(job0.id(), job0).get()
        assertEquals(id1, job0.int("person"))

        jobDao.create(job("manyToOne", id0)).get()
        jobDao.create(job("manyToOne", id1)).get()

        assertEquals(2, jobDao.list(filter = JsonObject().with("person", id1)).get().data.size)
    }

    @Test
    internal fun `person to many jobs`() {
        val id0 = personDao.create(person("oneToMany")).get().id()
        val id1 = personDao.create(person("oneToMany")).get().id()
        val job0 = jobDao.create(job("oneToMany", id0)).get()
        val job1 = jobDao.create(job("oneToMany", id0)).get()

        assertFails { personDao.delete(id0).get() }

        val person = personDao.read(id0).get()!!
        val jobsOfId0 = person.value("jobs")
        assertEquals("[${job0.id()},${job1.id()}]", jobsOfId0)
        assertEquals("[]", personDao.read(id1).get()!!.value("jobs"))
        assertEquals(2, jobDao.list(filter = JsonObject().with("id", jobsOfId0)).get().data.size)
    }

    private fun person(testcase: String) = JsonObject()
            .with("name", "$testcase${Random().nextInt()}")
            .with("alive", true)
            .with("email", email)


    private fun job(testcase: String, personId: Int) = JsonObject()
            .with("company", "$testcase${Random().nextInt()}")
            .with("title", title)
            .with("person", personId)


    private fun JsonObject.with(name: String, value: Int): JsonObject {
        addProperty(name, value)
        return this
    }

    private fun JsonObject.with(name: String, value: String): JsonObject {
        addProperty(name, value)
        return this
    }

    private fun JsonObject.with(name: String, value: Boolean): JsonObject {
        addProperty(name, value)
        return this
    }

    private fun JsonObject.id() = int("id")
    private fun JsonObject.int(name: String) = get(name).asInt
    private fun JsonObject.value(name: String) = get(name).asString
}