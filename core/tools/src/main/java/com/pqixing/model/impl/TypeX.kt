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
        private const val FLUTTER = "FLUTTER"
        fun items() = arrayOf(ROOT, DOC, JAVA, APP, LIBRARY, FLUTTER)
    }

    fun root() = ROOT == name
    fun java() = JAVA == name
    fun doc() = DOC == name
    fun app() = APP == name
    fun library() = LIBRARY == name
    fun flutter() = FLUTTER == name
    fun android() = app() || library() || flutter()

    init {

        merge("root", "script/${name.lowercase(Locale.CHINA)}.gradle")
        if (name == "ROOT") {
            ignores.add("local.gradle")
        }

        mockRun("com.didi.dchat:door:+")

        if (android()) {
            replaces.add("apply *?plugin *?: *?['\"]com.android.(application|library)['\"]")
            merge("root", "script/android.gradle")
        }

        merge("root", "script/maven.gradle")
        merge("module", "build.gradle")
    }

    fun ignore(module: String){
        ignores.add(module)
    }
    fun mockRun(module: String) {
        mockRuns.add(module)
    }

    fun merge(type: String, path: String) {
        merges.add("$type,$path")
    }

    fun merge(index: Int, type: String, path: String) {
        merges.add(index, "$type,$path")
    }

}