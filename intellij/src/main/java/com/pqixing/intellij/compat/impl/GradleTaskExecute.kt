package com.pqixing.intellij.compat.impl

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.pqixing.intellij.XApp
import com.pqixing.intellij.gradle.*
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/**
 * 生成兼容性listener
 */
private fun listener(project: Project, taskId: ExternalSystemTaskId, tasks: List<String>): ExternalSystemTaskNotificationListener? {
    runCatching {
        com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker.Request(project, File(project.basePath!!), tasks, taskId)
    }.onSuccess {
        return CompatListener(project, taskId, tasks.joinToString(","))
    }
    return null
}

class SystemExecute : IGradleExecute {
    override fun runGradleTask(project: Project, request: GradleRequest, callBack: (r: GradleResult) -> Unit) {
        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)

        val jvmOptions = request.getVmOptions()

        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, jvmOptions, false)
        val listener = GradleTaskListener(listener(project, taskId, request.tasks), project, callBack)


        for (ext in GradleTaskManagerExtension.EP_NAME.extensions) {
            if (ext.executeTasks(taskId, request.tasks, project.basePath!!, setting, request.getVmOptions(), listener)) {
                return
            }
        }
        throw RuntimeException("Cannot handle task by : ${GradleTaskManagerExtension.EP_NAME.extensions.joinToString { it.javaClass.name }}")
    }

}

/**
 * 直接调用InvokerExecute
 */
class InvokerExecute : IGradleExecute {
    override fun runGradleTask(project: Project, request: GradleRequest, callBack: (r: GradleResult) -> Unit) {
        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)
        val param =
            com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker.Request(project, File(project.basePath!!), request.tasks, taskId)

        val jvmOptions = request.getVmOptions()
        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, jvmOptions, false)
        val listener = GradleTaskListener(listener(project, taskId, request.tasks), project, callBack)

        param.setJvmArguments(ParametersListUtil.parse(request.getVmOptions()))
            .setCommandLineArguments(setting.arguments)
            .withEnvironmentVariables(setting.env)
            .passParentEnvs(setting.isPassParentEnvs)
            .setTaskListener(listener)
            .waitForCompletion()
        com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker.getInstance(project).executeTasks(param)
    }
}
