package com.outbrain.ob1k.crud.model

import java.lang.reflect.Method

fun Class<*>.toDescription(id: Int): EntityDescription {
    val entityFields = EntityFields()
    declaredMethods
            .filter { it.isGetter() }
            .forEach {
                val retType = it.returnType
                val type = when (retType) {
                    java.lang.Integer::class.java -> EFieldType.NUMBER
                    java.lang.Long::class.java -> EFieldType.NUMBER
                    java.lang.Double::class.java -> EFieldType.NUMBER
                    java.lang.Float::class.java -> EFieldType.NUMBER
                    java.lang.String::class.java -> EFieldType.STRING
                    java.lang.Boolean::class.java -> EFieldType.BOOLEAN
                    java.util.Date::class.java -> EFieldType.DATE
                    java.util.List::class.java -> EFieldType.LIST
                    java.util.Set::class.java -> EFieldType.LIST
                    else -> if (retType.isEnum) EFieldType.SELECT_BY_STRING else EFieldType.STRING

                }
                val nameCapitalized = it.name.removePrefix("get").removePrefix("is")
                val name = nameCapitalized.decapitalize()
                entityFields.with(name, type)
                val field = entityFields.get().last()
                field.setChoices(retType)
                if (type == EFieldType.LIST) {
                    val declaredField = getDeclaredField(name)
                    val collectionOf = (it.getAnnotation(CollectionOf::class.java)
                            ?: declaredField?.getAnnotation(CollectionOf::class.java)
                            ?: throw UnsupportedOperationException("missing ${CollectionOf::class.java} to $it"))
                    field.fields = collectionOf.value.java.toDescription(0).fields.toMutableList()
                }
            }



    return EntityDescription(table = "_${simpleName.decapitalize()}", id = id, fields = entityFields.get().sortedBy { it.name }).idFirst()
}

private fun Method.isGetter() = parameterTypes.isEmpty() && (name.startsWith("get") || name.startsWith("is"))