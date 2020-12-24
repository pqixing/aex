package com.pqixing.model.define

/**
 * 修改枚举类为空
 */
open class IType (val name:String) {
    var merge_absolute = mutableListOf<String>()
    var merge_root = mutableListOf<String>("script/maven.gradle")
    var merge_git = mutableListOf<String>()
    var merge_module = mutableListOf<String>("build.gradle")
    var git_ignore = mutableListOf<String>("*.iml")
    var replaces = mutableListOf<String>()
    var runs = mutableListOf<String>()
}