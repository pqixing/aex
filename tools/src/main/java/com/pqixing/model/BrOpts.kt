package com.pqixing.model

class BrOpts(val opts: String = "") {
    var target: String? = null
    var brs: List<String> = emptyList()

    init {
        val split = opts.split("&")
        if (split.size > 1) {
            target = split[0]
        }
        brs = split.lastOrNull()?.split(",") ?: emptyList()
    }

    override fun toString(): String {
        val sb = StringBuilder()
        if (target != null) sb.append(target).append("&")
        sb.append(brs.joinToString(","))
        return sb.toString()
    }
}

class BuildOpts(val opts: String) {
    val target: String? = null
}