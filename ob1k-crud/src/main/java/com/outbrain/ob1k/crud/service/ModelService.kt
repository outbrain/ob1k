package com.outbrain.ob1k.crud.service

import com.outbrain.ob1k.Service
import com.outbrain.ob1k.concurrent.ComposableFutures
import com.outbrain.ob1k.crud.model.Model

class ModelService(private val model: Model) : Service {

    fun getModel() = ComposableFutures.fromValue(model)
}