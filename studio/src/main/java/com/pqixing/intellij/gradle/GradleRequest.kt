package com.pqixing.intellij.gradle

import com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
import com.pqixing.XKeys
import com.pqixing.intellij.XApp
import com.pqixing.tools.UrlUtils
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

data class GradleRequest(
    val tasks: List<String>,
    val env: Map<String, String> = emptyMap(),
    var visible: Boolean = true,
    var activate: Boolean = true
) {


    fun getVmOptions(): String {
        val env = mapOf("include" to "", "local" to "false", "build" to "ide").plus(this.env)

        val option = StringBuilder()
//        if (GradleUtils.debugPort != 0) option.append("-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=${GradleUtils.debugPort}  -Dorg.gradle.debug=true  --no-daemon ")

        for (it in env.filter { it.key.isNotEmpty() && it.value.isNotEmpty() }) {
            option.append("-D${it.key}=\"${it.value}\" ")
        }
        return option.toString()
    }


    fun runGradle(project: Project, callBack: (r: GradleResult) -> Unit) {
        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)
        val parse = GradleParse(project, tasks.firstOrNull() ?: "Build", taskId, !activate, visible, callBack)
        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, getVmOptions(), false)


        val gradleBuildInvoker = GradleBuildInvoker.getInstance(project);
        val request = GradleBuildInvoker.Request(gradleBuildInvoker.project, File(project.basePath!!), tasks, taskId)
        GradleTaskManager.setupGradleScriptDebugging(setting)
        GradleTaskManager.appendInitScriptArgument(tasks, null, setting)
        // @formatter:off
        request.setJvmArguments(ParametersListUtil.parse(getVmOptions()))
            .setCommandLineArguments(setting.arguments)
            .withEnvironmentVariables(setting.env)
            .passParentEnvs(setting.isPassParentEnvs)
            .setTaskListener(parse)
            .waitForCompletion()
        // @formatter:on
        gradleBuildInvoker.executeTasks(request)
        XApp.log("./gradlew ${tasks.joinToString(" ")} ${getVmOptions()}  -Dorg.gradle.debug=true  --no-daemon ")
//        AndroidGradleTaskManager().executeTasks(taskId, tasks, project.basePath!!, setting, null, parse)
    }

}


class GradleParse(
    val project: Project,
    val title: String,
    taskId: ExternalSystemTaskId,
    var activate: Boolean = false,
    visible: Boolean,
    val callBack: (r: GradleResult) -> Unit
) :
    ExternalSystemTaskNotificationListenerAdapter() {
    val result: GradleResult = GradleResult()
    val out = OutResult(project, taskId, visible)


    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
        out.onEvent(id, StartBuildEventImpl(DefaultBuildDescriptor(id, title, workingDir!!, System.currentTimeMillis()), "running..."))
    }

    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) = out.onEvent(event.id, event)


    override fun onEnd(id: ExternalSystemTaskId) {
        out.onEvent(id, FinishBuildEventImpl(id, null, System.currentTimeMillis(), "finished", SuccessResultImpl()))
        out.close()
        callBack(result)
    }

    override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
        out.append(e.message ?: "", true)
        result.error = e
        result.success = false
    }

    override fun onSuccess(id: ExternalSystemTaskId) {
        result.success = true
    }

    override fun onCancel(id: ExternalSystemTaskId) = onFailure(id, RuntimeException("Cancel"))

    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
        //尝试调起运行面板
        if (!activate) {
            activate = XApp.activateWindow(project, "Build")
        }

        out.append(text, stdOut)
        if (text.startsWith(XKeys.PREFIX_IDE_LOG)) {
            result.param += UrlUtils.getParams(text)
        }
    }

}

class GradleResult {
    var success: Boolean = false
    var param = mapOf<String, String>()
    var error: Exception? = null

    //获取默认的数据
    fun getDefaultOrNull() = if (success) param["msg"] else null
}

