package com.outbrain.ob1k.crud.model

data class RangeStyle(val style: Map<String, String> = mapOf(),
                      val value: String? = null,
                      val values: List<String>? = null,
                      val range: List<Number?>? = null)
