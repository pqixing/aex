package com.pqixing.aex.model.define

import com.pqixing.model.impl.ModuleX
import groovy.lang.Closure

interface IProjectEx {
    fun module(name: String): ModuleX
    fun module(name: String, desc: String): ModuleX
    fun module(name: String, desc: String, type: String): ModuleX
    fun module(name: String, closure: Closure<*>?): ModuleX
    fun module(name: String, desc: String, closure: Closure<*>?): ModuleX
    fun module(name: String, desc: String, type: String, closure: Closure<*>?): ModuleX
    fun asModule(type: String): ModuleX
    fun asModule(type: String, closure: Closure<*>): ModuleX
    fun modules(closure: Closure<*>)
}