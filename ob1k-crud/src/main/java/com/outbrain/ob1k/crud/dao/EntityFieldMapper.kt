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
        val readOnly = row.getString("Extra") == "auto_increment"
        return EntityField(dbName, name, label, type, required, readOnly, readOnly)
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