package com.outbrain.ob1k.crud.audit

import com.outbrain.ob1k.Request
import com.outbrain.ob1k.Response

interface ICrudAudit {
    fun audit(username: String?, request: Request, response: Response)
}