package com.outbrain.ob1k.crud.model

import org.junit.Test
import kotlin.test.assertEquals

class ReindexTest {
    @Test
    internal fun `reindexing 1 to 2`() {
        assertEquals(listOf("0", "2", "1", "3", "4"), desc().reindex("1", 2).fields.map { it.name })
    }

    @Test
    internal fun `reindexing 1 to 0`() {
        assertEquals(listOf("1", "0", "2", "3", "4"), desc().reindex("1", 0).fields.map { it.name })
    }

    @Test
    internal fun `reindexing 1 to 4`() {
        assertEquals(listOf("0", "2", "3", "4", "1"), desc().reindex("1", 4).fields.map { it.name })
    }

    @Test
    internal fun `reindexing id first`() {
        val desc = desc()
        desc.fields[3].name = "id"
        assertEquals(listOf("id", "0", "1", "2", "4"), desc.idFirst().fields.map { it.name })
    }

    @Test
    internal fun `reindexing id first when no id`() {
        assertEquals(listOf("0", "1", "2", "3", "4"), desc().idFirst().fields.map { it.name })
    }

    private fun desc() = EntityDescription(table = "x", id = 0, fields = EntityFields()
            .with("0", EFieldType.STRING)
            .with("1", EFieldType.STRING)
            .with("2", EFieldType.STRING)
            .with("3", EFieldType.STRING)
            .with("4", EFieldType.STRING)
            .get())
}