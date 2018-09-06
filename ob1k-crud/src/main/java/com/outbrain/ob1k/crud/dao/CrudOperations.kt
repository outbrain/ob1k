package com.outbrain.ob1k.crud.dao

import java.beans.Introspector
import java.beans.PropertyDescriptor
import java.util.*


fun <T> List<T>.range(range: IntRange) = if (isEmpty()) this else subList(rangeCheck(range.start), rangeCheck(range.last) + 1)

fun <T> List<T>.like(like: T?): List<T> {
    if (like == null) {
        return this
    }
    val filterProperties = like.properties().filter { like.value(it) != null }
    return filter { item -> 
        filterProperties.all {
            val itemValue = item.value(it)
            val likeValue = like.value(it)
            when (likeValue) {
                is String -> itemValue.toString().contains(likeValue)
                else -> likeValue == itemValue
            }
        }
    }
}

fun <T> Collection<T>.sort(type: Class<*>, sort: Pair<String, String>): List<T> {
    val sortProperty = type.properties().find { it.name == sort.first }!!
    return sortedWith(compareBy<T> { it.asComparable(sortProperty) }.reverseIfNeeded(sort.second))
}


private fun Any?.value(pd: PropertyDescriptor) = this?.let { pd.readMethod.invoke(it) }
private fun Any.properties() = javaClass.properties()
private fun Class<*>.properties() = Introspector.getBeanInfo(this).propertyDescriptors
private fun <T> Comparator<T>.reverseIfNeeded(order: String) = if (order.toLowerCase() == "desc") reversed() else this
private fun List<*>.rangeCheck(idx: Int) = Math.min(Math.max(idx, 0), size - 1)
private fun <T> T.asComparable(sortProperty: PropertyDescriptor): Comparable<*>? {
    val value = value(sortProperty)
    return when (value) {
        is Number -> value.toDouble()
        is Date -> value
        else -> value.toString()
    }
}