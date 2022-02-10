package com.pqixing.profm.model.define

import com.pqixing.model.define.ICompile
import groovy.lang.Closure

interface IModuleEx {
    fun compiles(): MutableList<ICompile>
    fun compiles(closure: Closure<*>)
    fun compile(scope: String, name: String): ICompile
    fun compile(scope: String, name: String, version: String): ICompile
    fun compile(scope: String, name: String, version: String, closure: Closure<*>?): ICompile
    fun implementation(name: String): ICompile
    fun implementation(name: String, version: String): ICompile
    fun implementation(name: String, version: String, closure: Closure<*>?): ICompile
    fun compileOnly(name: String): ICompile
    fun compileOnly(name: String, version: String): ICompile
    fun compileOnly(name: String, version: String, closure: Closure<*>?): ICompile
    fun runtimeOnly(name: String): ICompile
    fun runtimeOnly(name: String, version: String): ICompile
    fun runtimeOnly(name: String, version: String, closure: Closure<*>?): ICompile
    fun api(name: String): ICompile
    fun api(name: String, version: String): ICompile
    fun api(name: String, version: String, closure: Closure<*>?): ICompile
    fun proxy(name: String): ICompile
    fun proxy(name: String, version: String): ICompile
    fun proxy(name: String, version: String, closure: Closure<*>?): ICompile
}