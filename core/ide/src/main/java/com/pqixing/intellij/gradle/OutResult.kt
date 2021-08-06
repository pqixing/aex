package com.pqixing.intellij.gradle

import com.intellij.build.BuildViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.pqixing.intellij.XApp

class OutResult(project: Project, taskId: ExternalSystemTaskId, val visible: Boolean) {
    val out = if (!visible) null else kotlin.runCatching {
        com.intellij.openapi.externalSystem.service.execution.ExternalSystemEventDispatcher(
            taskId, com.intellij.openapi.components.ServiceManager.getService(project, BuildViewManager::class.java), true
        )
    }.getOrNull()


    fun onEvent(buildId: Any, event: BuildEvent) {
        try {
            out?.onEvent(buildId, event)
        } finally {

        }
    }

    fun onEvent(buildId: Any, event: Any) {
        kotlin.runCatching {
            if (event is com.intellij.openapi.externalSystem.model.task.event.ExternalSystemBuildEvent) {
                onEvent(event.getId(), event.buildEvent)
            } else if (event is com.intellij.openapi.externalSystem.model.task.event.ExternalSystemTaskExecutionEvent) {
                onEvent(event.getId(), ExternalSystemUtil.convert(event))
            }
        }
    }

    fun close() {
        kotlin.runCatching {
            out?.close()
        }
    }

    fun append(csq: CharSequence?, stdOut: Boolean? = null) {
        try {
            if (stdOut != null) out?.setStdOut(stdOut)
            if (visible) out?.append(csq)
        } catch (e: Exception) {
            XApp.log(csq?.toString())
        }
    }
}