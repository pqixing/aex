package com.pqixing.model.define

import java.net.URI

open class IMaven(open val name: String) {
    var url: String = ""
    var user: String = ""
    var psw: String = ""
    var group: String = "com.pqixing"
    var version: String = "1.0"
    var task: String = "uploadArchives"
    fun uri():URI= URI(if (url.startsWith("http")) url else "file://$url")
}