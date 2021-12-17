package com.pqixing.intellij.gradle

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.project.Project
import com.pqixing.XKeys
import com.pqixing.intellij.XApp
import com.pqixing.tools.UrlUtils

open class GradleTaskListener(
    val proxy: ExternalSystemTaskNotificationListener?,
    val project: Project,
    val callBack: (r: GradleResult) -> Unit
) : ExternalSystemTaskNotificationListenerAdapter(proxy) {


    val result: GradleResult = GradleResult()

    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        super.onStart(id, workingDir)
        XApp.activateWindow(project, "Build")
    }

    override fun onEnd(id: ExternalSystemTaskId) {
        super.onEnd(id)
        callBack(result)
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        super.onFailure(id, e)
        result.error = e
        result.success = false
    }

    override fun onSuccess(id: ExternalSystemTaskId) {
        super.onSuccess(id)
        result.success = true
    }

    override fun onCancel(id: ExternalSystemTaskId) {
        super.onCancel(id)
        result.error = RuntimeException("onCancel")
        result.success = false
    }

    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        super.onTaskOutput(id, text, stdOut)
        if (text.startsWith(XKeys.PREFIX_IDE_LOG)) {
            result.param += UrlUtils.getParams(text)
        }
    }

}