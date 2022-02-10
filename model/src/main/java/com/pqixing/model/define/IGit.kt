package com.pqixing.model.define

import com.pqixing.base64Decode

data class IGit(val name: String,
                var url: String = "",
                var user: String = "",
                var psw: String = "",
                var group: String = ""
)