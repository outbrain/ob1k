package com.outbrain.ob1k.crud.dao

import com.outbrain.ob1k.crud.model.*
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
        val sourceNameField = sourceTable("name") ?: return false
        val targetNameField = targetTable("name") ?: return false


        val targetReferenceField = EntityField("_",
                sourceTable.resourceName + "s",
                sourceTable.title + "s",
                EFieldType.REFERENCEMANY,
                true,
                true,
                sourceTable.resourceName,
                "id",
                EntityFieldDisplay(sourceNameField.name, EDisplayType.Chip))

        val sourceReferenceField = EntityField(sourceField.dbName,
                targetTable.resourceName,
                targetTable.title,
                EFieldType.REFERENCE,
                true,
                false,
                targetTable.resourceName,
                "id",
                EntityFieldDisplay(targetNameField.name, EDisplayType.Select, targetNameField.name))

        targetTable.fields += targetReferenceField
        sourceTable.fields += sourceReferenceField
        targetTable.references += sourceTable
        return true
    }
}