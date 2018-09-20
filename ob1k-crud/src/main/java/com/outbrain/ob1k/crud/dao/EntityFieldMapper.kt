package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.model.EFieldType
import com.outbrain.ob1k.crud.model.EntityField
import com.outbrain.ob1k.db.ResultSetMapper
import com.outbrain.ob1k.db.TypedRowData

class EntityFieldMapper : ResultSetMapper<EntityField> {
    override fun map(row: TypedRowData?, columnNames: MutableList<String>?): EntityField {
        val dbName = row!!.getString("Field")
        val name = dbName.substringAfter("_")
        val label = name.capitalize()
        val required = row.getString("Null")!! == "NO"
        val dbType = row.getString("Type")
        val type = dbType.toAppType()
        val audoGenerate = row.getString("Extra") == "auto_increment"
        val entityField = EntityField(dbName, name, label, type, required, audoGenerate, audoGenerate)
        if (audoGenerate) {
            entityField.nonNullOptions().useGrouping = false
        }
        return entityField
    }


    private fun String.toAppType(): EFieldType {
        return when {
            startsWith("varchar") || startsWith("tinytext") -> EFieldType.STRING
            startsWith("tinyint") -> EFieldType.BOOLEAN
            startsWith("date") || startsWith("timestamp") -> EFieldType.DATE
            else -> EFieldType.NUMBER
        }
    }
}