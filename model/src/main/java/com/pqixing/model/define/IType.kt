package com.pqixing.model.define

/**
 * 修改枚举类为空
 */
open class IType(val name: String) {
    var merges = mutableListOf<String>()
    var ignores = mutableListOf<String>("*.iml")
    var replaces = mutableListOf<String>()
    var mockRuns = mutableListOf<String>()
}