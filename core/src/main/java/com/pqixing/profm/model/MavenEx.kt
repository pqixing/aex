package com.pqixing.profm.model

import com.pqixing.model.define.IMaven
import java.io.File

data class MavenEx(override var name: String) : IMaven(name) {
    var file: File? = null
    var artifactId: String = ""
    var toMaven = false
}