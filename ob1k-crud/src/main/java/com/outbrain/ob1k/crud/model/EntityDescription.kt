package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class EntityDescription(@JsonIgnore val table: String,
                             val id: Int,
                             val resourceName: String = table.substringAfterLast("_"),
                             var title: String = resourceName.capitalize(),
                             var endpoint: String = "../crud",
                             var fields: List<EntityField> = emptyList(),
                             var editable: Boolean = true,
                             var icon: String? = null,
                             var perPage: Int? = null) {
    @JsonIgnore
    val references = mutableListOf<EntityDescription>()

    operator fun invoke(name: String): EntityField? = fields.find { name == it.name }
    fun getByColumn(dbName: String): EntityField? = fields.find { dbName == it.dbName }

    fun idDBFieldName() = idField().dbName

    fun idField() = this("id")!!

    fun add2DirectionReferenceTo(target: EntityDescription, referenceFieldName: String) {
        addOneToOneference(target, referenceFieldName)
        target.addOneToManyReference(this, "${resourceName}s")
    }

    fun addOneToOneference(target: EntityDescription, referenceFieldName: String) {

        val referenceField = this(referenceFieldName)!!

        val targetDisplayField = target("name") ?: target("title") ?: target.idField()

        referenceField.name = target.resourceName
        referenceField.label = target.title
        referenceField.type = EFieldType.REFERENCE
        referenceField.reference = target.resourceName
        referenceField.target = null
        referenceField.required = true
        referenceField.display = EntityFieldDisplay(targetDisplayField.name, EDisplayType.Select, targetDisplayField.name)
    }

    fun addOneToManyReference(target: EntityDescription, referenceFieldName: String) {
        var reverseField = fields.find { it.name == referenceFieldName }
        if (reverseField == null) {
            reverseField = EntityField(dbName = "_", name = "_", label = "_", type = EFieldType.REFERENCEMANY)
            fields += reverseField
        }

        val displayField = target("name") ?: target("title") ?: idField()

        reverseField.dbName = "_"
        reverseField.name = "${target.resourceName}s"
        reverseField.label = "${target.title}s"
        reverseField.type = EFieldType.REFERENCEMANY
        reverseField.required = false
        reverseField.readOnly = false
        reverseField.autoGenerate = false
        reverseField.reference = target.resourceName
        reverseField.target = resourceName
        reverseField.display = EntityFieldDisplay(displayField.name, EDisplayType.Chip, displayField.name)

        references += target
    }

    fun reindex(predicate: (EntityField) -> Boolean, idx: Int): EntityDescription {
        val prevIdx = fields.indexOfFirst(predicate)
        if (prevIdx >= 0) {
            val entry = fields[prevIdx]
            fields = fields.subList(0, prevIdx) + fields.subList(prevIdx + 1, fields.size)
            fields = fields.subList(0, idx) + entry + fields.subList(idx, fields.size)
        }
        return this
    }

    fun reindex(name: String, idx: Int) = reindex({ it.name == name }, idx)

    fun idFirst() = reindex("id", 0)
}