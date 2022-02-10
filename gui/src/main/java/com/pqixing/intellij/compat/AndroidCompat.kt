package com.pqixing.intellij.compat

import com.intellij.openapi.project.Project
import com.pqixing.intellij.XApp
import com.pqixing.intellij.compat.impl.AdbExecute
import com.pqixing.intellij.compat.impl.AdbInstall
import com.pqixing.intellij.compat.impl.InvokerExecute
import com.pqixing.intellij.compat.impl.SystemExecute
import com.pqixing.intellij.compat.interfaces.IAdbExecute
import com.pqixing.intellij.compat.interfaces.IAdbInstall
import com.pqixing.intellij.compat.interfaces.IDeviceProvide
import com.pqixing.intellij.device.DeviceWrapper
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.gradle.GradleResult
import com.pqixing.intellij.gradle.IGradleExecute

object AndroidCompat : IGradleExecute, IDeviceProvide, IAdbExecute, IAdbInstall {
    private val GRADLE_EXECUTE = mutableListOf<IGradleExecute>()
    private val DEVICE_PROVIDE = mutableListOf<IDeviceProvide>()
    private val ADB_COMMAND = mutableListOf<IAdbExecute>()

    private val ADB_INSTALL = mutableListOf<IAdbInstall>()

    init {
        //注册
        register(GRADLE_EXECUTE) { SystemExecute() }
        register(GRADLE_EXECUTE) { InvokerExecute() }
        register(GRADLE_EXECUTE, "com.pqixing.bumblebee.compat.InvokerCompatExecute")

        register(DEVICE_PROVIDE, "com.pqixing.bumblebee.compat.DeviceProvide")

        register(ADB_COMMAND) { AdbExecute() }
        register(ADB_COMMAND, "com.pqixing.bumblebee.compat.AdbExecuteNew")

        register(ADB_INSTALL) { AdbInstall() }
    }


    private fun <T> register(list: MutableList<T>, block: () -> T) {
        kotlin.runCatching { list.add(block()) }.onFailure { XApp.log(it.toString()) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> register(list: MutableList<T>, className: String) {
        register(list) { Class.forName(className).getConstructor().newInstance() as T }
    }

    private fun <T : Any, R> exe(list: List<T>, block: (t: T) -> R): R? {
        for (l in list) {
            XApp.log("Start task by : " + l.javaClass.name)
            kotlin.runCatching { block(l) }.onFailure { XApp.log(it.message) }.onSuccess { return it }
        }
        return null
    }

    fun isGradleProject(project: Project?): Boolean = kotlin.runCatching {
        project ?: return false
        val info = com.android.tools.idea.gradle.project.GradleProjectInfo.getInstance(project)
        return info.isBuildWithGradle && (info.androidModules.isNotEmpty() || com.android.tools.idea.IdeInfo.getInstance().isAndroidStudio)
    }.getOrDefault(false)


    override fun getSelectDevices(project: Project): List<DeviceWrapper>? {
        return exe(DEVICE_PROVIDE) { it.getSelectDevices(project) }
    }

    override fun runAdbTask(device: DeviceWrapper, cmd: String): List<String>? {
        return exe(ADB_COMMAND) { it.runAdbTask(device, cmd) }
    }

    override fun install(device: DeviceWrapper, path: String, param: String): Boolean {
        return exe(ADB_INSTALL) { it.install(device, path, param) } ?: false

    }

    override fun runGradleTask(project: Project, request: GradleRequest, callBack: (r: GradleResult) -> Unit) {
        XApp.log("./gradlew ${request.tasks.joinToString(" ")} ${request.getVmOptions()}  -Dorg.gradle.debug=true  --no-daemon")
        exe(GRADLE_EXECUTE) { it.runGradleTask(project, request, callBack) }
    }
}