package com.pqixing.model.define

import com.pqixing.model.BasicCredentials
import com.pqixing.real
import java.net.URI

open class IMaven(open val name: String) {
    var url: String = ""
    var user: String = ""
    var psw: String = ""
    var group: String = "com.pqixing"
    var version: String = "1.0"

    //匿名访问网络
    var anonymous: Boolean = true
    var task: String = "uploadArchives"
    fun uri(): URI = URI(if (url.startsWith("http")) url else "file://$url")
    fun asCredentials() = BasicCredentials(user, psw.real(), !anonymous)
}