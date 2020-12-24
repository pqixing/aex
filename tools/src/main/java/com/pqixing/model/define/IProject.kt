package com.pqixing.model.define

import com.pqixing.model.impl.ModuleX

open class IProject(val name:String) {
    var desc: String = ""
    var url: String = ""
    var path: String = ""
    lateinit var maven: IMaven
    lateinit var git: IGit
    val modules = mutableListOf<ModuleX>()
}