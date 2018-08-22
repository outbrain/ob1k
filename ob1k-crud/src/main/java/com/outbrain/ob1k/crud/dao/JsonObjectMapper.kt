package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.crud.model.EntityField
import com.outbrain.ob1k.db.ResultSetMapper
import com.outbrain.ob1k.db.TypedRowData

class JsonObjectMapper(private val description: EntityDescription) : ResultSetMapper<JsonObject> {

    override fun map(row: TypedRowData?, columnNames: MutableList<String>?): JsonObject {
        val jsonObject = JsonObject()
        description.fields.forEach { it.type.fillJsonObject(jsonObject, it.name, row!!.getRaw(it.column())?.toString()) }
        return jsonObject
    }

    private fun EntityField.column() = if (type == EFieldType.REFERENCEMANY) name else dbName
}