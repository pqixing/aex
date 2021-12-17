package com.pqixing.intellij.gradle

import com.intellij.openapi.project.Project
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.gradle.GradleResult

interface IGradleExecute {
    fun runGradleTask(project: Project, request: GradleRequest, callBack: (r: GradleResult) -> Unit)
}