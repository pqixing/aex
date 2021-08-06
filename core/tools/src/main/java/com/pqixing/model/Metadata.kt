package com.pqixing.model

import java.util.*

/**
 * 头信息包裹类
 */
class Metadata {
    var artifactId: String = ""

    var groupId: String = ""

    var release: String = ""

    val versions: LinkedList<String> = LinkedList()
    override fun toString(): String {
        return "XMetadata(artifactId='$artifactId', groupId='$groupId', release='$release', versions=$versions)"
    }

}