package com.outbrain.ob1k.crud.dao

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.EntityDescription
import com.outbrain.ob1k.db.ResultSetMapper
import com.outbrain.ob1k.db.TypedRowData

class JsonObjectMapper(private val description: EntityDescription) : ResultSetMapper<JsonObject> {
    private val map: MutableMap<String, JsonObject> = mutableMapOf()

    override fun map(row: TypedRowData, columnNames: MutableList<String>?): JsonObject {
        val jsonObject = description.fromRow(row)!!
        description.deepReferences().forEach {
            val fromEntity = it.fromDesc.fromRow(row)
            val toEntity = it.toDesc.fromRow(row)
            if (fromEntity != null && toEntity != null) {
                if (fromEntity.getAsJsonArray(it.fromField.name) == null) {
                    fromEntity.add(it.fromField.name, JsonArray())
                }
                fromEntity.getAsJsonArray(it.fromField.name).add(toEntity)
            }
        }
        return jsonObject

    }


    private fun EntityDescription.fromRow(row: TypedRowData): JsonObject? {
        val id = row.getRaw(idDBFieldName())?.toString()
        if (id.isNullOrEmpty() || id?.toLowerCase() == "null") {
            return null
        }
        val key = "${resourceName}_$id"
        val jsonObject = map.getOrPut(key) { JsonObject() }
        fields.asSequence()
                .filter { it.type != EFieldType.REFERENCEMANY }
                .filter { it.type != EFieldType.LIST }
                .forEach { it.fillJsonObject(jsonObject, it.name, row.getRaw(it.dbName)?.toString()) }

        return jsonObject
    }
}