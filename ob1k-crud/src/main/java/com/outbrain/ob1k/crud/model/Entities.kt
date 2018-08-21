package com.outbrain.ob1k.crud.model

open class Entities<T>(val total: Int = 0, val data: List<T> = emptyList()) {
    operator fun get(i: Int) = data[i]
}