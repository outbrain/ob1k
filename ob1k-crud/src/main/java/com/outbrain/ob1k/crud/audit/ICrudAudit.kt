package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.Request

interface ICrudAudit {
    fun audit(username: String?, request: Request)
}