package com.pqixing.bumblebee.compat

import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.gradle.GradleResult
import com.pqixing.intellij.gradle.GradleTaskListener
import com.pqixing.intellij.gradle.IGradleExecute
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File


class InvokerCompatExecute : IGradleExecute {
    override fun runGradleTask(project: Project, request: GradleRequest, callBack: (r: GradleResult) -> Unit) {


        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)

        val jvmOptions = request.getVmOptions()
        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, jvmOptions, false)
        val listener = GradleTaskListener(null, project, callBack)

        val params = GradleBuildInvoker.Request.Builder(project, File(project.basePath!!), request.tasks)
            .setJvmArguments(ParametersListUtil.parse(request.getVmOptions()))
            .setTaskId(taskId)
            .setCommandLineArguments(setting.arguments)
            .withEnvironmentVariables(setting.env)
            .passParentEnvs(setting.isPassParentEnvs)
            .setListener(listener)
            .waitForCompletion().build()
        GradleBuildInvoker.getInstance(project).executeTasks(params)
    }
}