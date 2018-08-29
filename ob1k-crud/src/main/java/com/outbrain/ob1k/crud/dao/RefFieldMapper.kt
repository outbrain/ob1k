package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.model.Model
import com.outbrain.ob1k.db.ResultSetMapper
import com.outbrain.ob1k.db.TypedRowData

class RefFieldMapper(val desc: Model) : ResultSetMapper<Boolean> {
    override fun map(row: TypedRowData?, columnNames: MutableList<String>?): Boolean {
        val sourceTableName = row!!.getString("TABLE_NAME")
        val sourceColumn = row.getString("COLUMN_NAME")
        val targetTableName = row.getString("REFERENCED_TABLE_NAME")
        val targetColumn = row.getString("REFERENCED_COLUMN_NAME")

        val sourceTable = desc.getByTable(sourceTableName) ?: return false
        val targetTable = desc.getByTable(targetTableName) ?: return false
        val sourceField = sourceTable.getByColumn(sourceColumn) ?: return false

        sourceTable.add2DirectionReferenceTo(targetTable, sourceField.name)
        return true
    }
}