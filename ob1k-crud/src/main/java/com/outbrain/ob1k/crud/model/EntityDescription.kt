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

    fun idFieldName() = idField().name

    fun idField() = this("id")!!

    fun referenceTo(resource: String) = fields.find { (it.type == EFieldType.REFERENCE || it.type == EFieldType.REFERENCEMANY) && it.reference == resource }

    fun addReferenceTo(referenceFieldName: String, target: EntityDescription) {
        val referenceField = this(referenceFieldName)!!

        var reverseField = target.fields.find { it.name == "${resourceName}s" }
        if (reverseField == null) {
            reverseField = EntityField("_", "_", "_", EFieldType.REFERENCEMANY)
            target.fields += reverseField
        }

        val displayField = this("name") ?: this("title") ?: idField()
        val targetDisplayField = target("name") ?: target("title") ?: target.idField()

        reverseField.dbName = "_"
        reverseField.name = "${resourceName}s"
        reverseField.label = "${title}s"
        reverseField.type = EFieldType.REFERENCEMANY
        reverseField.required = false
        reverseField.readOnly = true
        reverseField.autoGenerate = false
        reverseField.reference = resourceName
        reverseField.target = target.resourceName
        reverseField.display = EntityFieldDisplay(displayField.name, EDisplayType.Chip, displayField.name)

        referenceField.name = target.resourceName
        referenceField.label = target.title
        referenceField.type = EFieldType.REFERENCE
        referenceField.reference = target.resourceName
        referenceField.target = null
        referenceField.required = true
        referenceField.display = EntityFieldDisplay(targetDisplayField.name, EDisplayType.Select, targetDisplayField.name)


        target.references += this
    }
}