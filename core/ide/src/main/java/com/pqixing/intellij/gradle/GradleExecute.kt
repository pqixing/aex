package com.pqixing.intellij.gradle

import com.intellij.notification.NotificationType
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.project.Project
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XNotifyAction
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

object GradleExecute {

    var output: Boolean = hasOutput()
    var option: Boolean = false

    /**
     * 是否需要日志输出
     * AndroidStudio 2021.1.1 版本上,执行gradle任务时,不需要额外打印日志
     */
    private fun hasOutput(): Boolean = kotlin.runCatching {
        com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker::class.java.declaredMethods.find { it.name == "createBuildTaskListener" } != null
    }.getOrDefault(false)

    //当前可用的执行模式,默认三种都可使用
    private var tasks: HashMap<String, Boolean> = linkedMapOf("invoker" to true, "default" to true, "android" to true)

    fun execute(project: Project, request: GradleRequest, callBack: (r: GradleResult) -> Unit) {
        var execute = false
        var count = 0

        if (!execute && tasks["invoker"] == true) {
            execute = kotlin.runCatching { executeByInvoker(project, request, callBack) }.isSuccess
            count++
        }

        if (!execute && tasks["default"] == true) {
            execute = kotlin.runCatching { executeTasksByDefault(project, request, callBack) }.isSuccess
            count++
        }

        if (!execute && tasks["android"] == true) {
            execute = kotlin.runCatching { executeTasksByAndroid(project, request, callBack) }.isSuccess
            count++
        }

        val actions = mutableListOf<XNotifyAction>()
        actions += XNotifyAction("Retry") { execute(project, request, callBack) }
        if (option || count > 1) {
            //可以选择使用执行的编译模式和兼容模式
            actions += tasks.map { i ->
                XNotifyAction("${i.key} = ${i.value}") {
                    tasks[i.key] = !i.value
                    execute(project, request, callBack)
                }
            }
            actions += XNotifyAction("output  = ${output}") {
                output = !output
                execute(project, request, callBack)
            }
        }
        XApp.notify(
            project,
            "Start Gradle Task : ${request.tasks}",
            "./gradlew ${request.tasks.joinToString(" ")} ${request.getVmOptions()}  -Dorg.gradle.debug=true  --no-daemon",
            NotificationType.INFORMATION,
            actions,
            if (execute) XApp.LOG else XApp.BALLOON
        )
    }


    private fun executeByInvoker(project: Project, req: GradleRequest, callBack: (r: GradleResult) -> Unit) {

        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)

        val invoker = com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker.getInstance(project);
        val request =
            com.android.tools.idea.gradle.project.build.invoker.GradleBuildInvoker.Request(project, File(project.basePath!!), req.tasks, taskId)

        val jvmOptions = req.getVmOptions()
        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, jvmOptions, false)

        GradleTaskManager.setupGradleScriptDebugging(setting)
        GradleTaskManager.appendInitScriptArgument(req.tasks, null, setting)

        val listener =
            GradleTaskListener(invoker.createBuildTaskListener(request, req.tasks.firstOrNull()), project, req.tasks, callBack)

        request.setJvmArguments(com.intellij.util.execution.ParametersListUtil.parse(jvmOptions))
            .setCommandLineArguments(setting.arguments)
            .withEnvironmentVariables(setting.env)
            .passParentEnvs(setting.isPassParentEnvs)
            .setTaskListener(listener)
            .waitForCompletion()

        invoker.executeTasks(request)
    }


    private fun executeTasksByDefault(project: Project, req: GradleRequest, callBack: (r: GradleResult) -> Unit) {

        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)

        val jvmOptions = req.getVmOptions()
        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, jvmOptions, false)

        val listener = GradleTaskListener(
            if (!output) null else GradleCompatListener.createTaskListener(project, taskId, req.tasks.firstOrNull()),
            project,
            req.tasks,
            callBack
        )

        GradleTaskManager().executeTasks(taskId, req.tasks, project.basePath!!, setting, jvmOptions, listener)
    }

    private fun executeTasksByAndroid(project: Project, req: GradleRequest, callBack: (r: GradleResult) -> Unit) {
        val taskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, project)

        val jvmOptions = req.getVmOptions()

        val setting = GradleExecutionSettings(null, null, DistributionType.BUNDLED, jvmOptions, false)

        val listener = GradleTaskListener(
            if (!output) null else GradleCompatListener.createTaskListener(project, taskId, req.tasks.firstOrNull()),
            project,
            req.tasks,
            callBack
        )

        com.android.tools.idea.gradle.task.AndroidGradleTaskManager()
            .executeTasks(taskId, req.tasks, project.basePath!!, setting, jvmOptions, listener)
    }

}