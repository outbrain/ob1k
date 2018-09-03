package com.outbrain.ob1k.crud.model

import java.lang.reflect.Method

class TypeToEntityDescription {
    fun toDescription(id: Int, type: Class<*>): EntityDescription {
        val entityFields = EntityFields()
        type.declaredMethods
                .filter { it.isGetter() }
                .forEach {
                    val paramType = it.returnType
                    val type = when (paramType) {
                        java.lang.Integer::class.java -> EFieldType.NUMBER
                        java.lang.Long::class.java -> EFieldType.NUMBER
                        java.lang.Double::class.java -> EFieldType.NUMBER
                        java.lang.Float::class.java -> EFieldType.NUMBER
                        java.lang.String::class.java -> EFieldType.STRING
                        java.lang.Boolean::class.java -> EFieldType.BOOLEAN
                        java.util.Date::class.java -> EFieldType.DATE
                        else -> EFieldType.STRING
                    }
                    val nameCapitalized = it.name.removePrefix("get").removePrefix("is")
                    val name = nameCapitalized.decapitalize()
                    entityFields.with(name, type)
                }


        return EntityDescription(table = "_${type.simpleName.decapitalize()}", id = id, fields = entityFields.get().sortedBy { it.name }).idFirst()
    }

    private fun Method.isGetter() = parameterTypes.isEmpty() && (name.startsWith("get") || name.startsWith("is"))
}