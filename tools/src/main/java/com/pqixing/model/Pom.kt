package com.pqixing.model

/**
 * pom信息信息包裹类
 */
class Pom {
    var artifactId: String = ""

    var groupId: String = ""

    var version: String = ""
    var packaging: String = ""
    var name: String = ""

    val allExclude: HashSet<String> = HashSet()
    val dependency: HashSet<String> = HashSet()

    override fun toString(): String {
        return "XPom(artifactId='$artifactId', groupId='$groupId', version='$version', packaging='$packaging', name='$name', allExclude=$allExclude, dependency=$dependency)"
    }


}