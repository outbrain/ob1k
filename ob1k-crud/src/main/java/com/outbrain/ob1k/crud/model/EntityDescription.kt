package com.outbrain.ob1k.crud.model

import com.fasterxml.jackson.annotation.JsonIgnore

data class EntityDescription(@JsonIgnore val table: String,
                             val id: Int,
                             val resourceName: String = table.substringAfterLast("_"),
                             var title: String = resourceName.capitalize(),
                             val endpoint: String = "../${resourceName}s",
                             var fields: List<EntityField> = emptyList()) {
    @JsonIgnore
    val references = mutableListOf<EntityDescription>()

    operator fun invoke(name: String): EntityField? = fields.find { name == it.name }
    fun getByColumn(dbName: String): EntityField? = fields.find { dbName == it.dbName }

    fun editableFields() = fields.filter { !it.readOnly }

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
            reverseField = EntityField("_", "_", "_", EFieldType.REFERENCEMANY)
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
}