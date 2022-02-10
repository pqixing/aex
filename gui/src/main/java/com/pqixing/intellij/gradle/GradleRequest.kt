package com.pqixing.intellij.gradle

import com.intellij.openapi.project.Project
import com.pqixing.intellij.compat.AndroidCompat

data class GradleRequest(
    val tasks: List<String>,
    val env: Map<String, String> = emptyMap(),
    val param: String = ""
) {

    fun getVmOptions(): String {
        val env = mapOf("include" to "", "local" to "false", "build" to "ide").plus(this.env)

        val option = StringBuilder("  $param  ")

        for (it in env.filter { it.key.isNotEmpty() && it.value.isNotEmpty() }) {
            option.append("-D${it.key}=\"${it.value}\" ")
        }

        return option.toString()
    }


    fun executeTasks(project: Project, callBack: (r: GradleResult) -> Unit) {
        AndroidCompat.runGradleTask(project,this,callBack)
    }
}

class GradleResult {
    var success: Boolean = false
    var param = mapOf<String, String>()
    var error: Exception? = null

    //获取默认的数据
    fun getDefaultOrNull() = if (success) param["msg"] else null
}

