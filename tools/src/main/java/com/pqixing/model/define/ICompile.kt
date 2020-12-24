package com.pqixing.model.define

open class ICompile(var name: String = "") {
    var version: String = ""

    /**
     * 依赖模式
     * runtimeOnly , compileOnly , implementation , compile
     */
    var scope: String = "api"

    /**
     * 忽略版本
     * group:module
     */
    var excludes = HashSet<String>()
}