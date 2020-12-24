package com.pqixing.model.impl

import com.pqixing.model.define.IType
import java.util.*

/**
 * 修改枚举类为空
 */
class TypeX(name: String) : IType(name) {
    companion object {
        private const val ROOT = "ROOT"
        private const val JAVA = "JAVA"
        private const val APP = "APP"
        private const val DOC = "DOC"
        private const val LIBRARY = "LIBRARY"
        fun items() = arrayOf(ROOT, DOC, JAVA, APP, LIBRARY)
    }

    fun root() = ROOT == name
    fun java() = JAVA == name
    fun doc() = DOC == name
    fun app() = APP == name
    fun library() = LIBRARY == name
    fun android() = app() || library()

    init {
        merge_root.add(0, "script/${name.toLowerCase(Locale.CHINA)}.gradle")
        if (name == "ROOT") {
            git_ignore.add("local.gradle")
        }

        if (name == "LIBRARY") {
            runs.add("com.pqixing.aex:launch:+")
        }

        if (name == "APP" || name == "LIBRARY") {
            replaces.add("apply *?plugin: *?['\"]com.android.(application|library)['\"]")
            merge_root.add(0, "script/android.gradle")
        }
    }
}