package com.pqixing.model

import com.pqixing.base64Encode

class BasicCredentials(val user: String, val password: String, private val vail: Boolean = true) {
    fun isVail() = vail && user.isNotEmpty() && password.isNotEmpty()

    fun credentials(): String = "Basic " + "$user:$password".base64Encode()
}