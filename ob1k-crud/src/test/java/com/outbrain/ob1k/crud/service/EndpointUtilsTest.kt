package com.outbrain.ob1k.crud.service

import kotlin.test.assertEquals
import kotlin.test.assertNull

class EndpointUtilsTest {
    @org.junit.Test
    internal fun `parse range`() {
        val range = "[20,50]".range()
        assertEquals(20, range.first)
        assertEquals(50, range.last)
    }

    @org.junit.Test
    internal fun `parse sort`() {
        val sortString = "[\"name\",\"abcd\"]".unqoutedPair()
        val sortInt = "[\"num\",5]".unqoutedPair()
        assertEquals("name", sortString.first)
        assertEquals("abcd", sortString.second)
        assertEquals("num", sortInt.first)
        assertEquals("5", sortInt.second)
    }

    @org.junit.Test
    internal fun `parse json`() {
        val json = "{\"name\":\"abcd\",\"num\":5}".json()
        assertEquals("abcd", json.get("name").asString)
        assertEquals(5, json.get("num").asInt)
    }

    @org.junit.Test
    internal fun `parse ids`() {
        assertEquals(2, "{\"id\":[\"1\",\"2\"]}".json().ids()!!.size)
        assertNull("{\"id\":1}".json().ids())
        assertNull("{\"name\":\"abcd\",\"num\":5}".json().ids())
    }
}