package com.pqixing.intellij

import com.intellij.dvcs.repo.VcsRepositoryManager
import com.intellij.notification.*
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDirectoryMapping
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.pqixing.XHelper
import com.pqixing.model.impl.ProjectX
import com.pqixing.tools.FileUtils
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


object XApp {
    private val executors: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val exeTimes = hashMapOf<String, Long>()
    private val enables = hashMapOf<String, Boolean>()
    val maps: Properties by lazy { FileUtils.readProperties(File(cacheDir(), "share.xml")) }
    private val BALLOON: NotificationGroup by lazy { notificationGroup(true) }
    private val LOG: NotificationGroup by lazy { notificationGroup(false) }

    /**
     * 检查当前是否是ex项目
     */
    fun isExProject(project: Project?, check: Boolean, async: (s: Boolean) -> Unit = {}): Boolean {
        return isEnable(project, "isExProject", check, async) {
            XHelper.ideImportFile(project?.basePath ?: "").exists()
        }
    }

    /**
     * 检查当前是否有更新
     */
    fun isRepoUpdate(project: Project?, check: Boolean, async: (s: Boolean) -> Unit = {}): Boolean {
        return isExProject(project, false) && isEnable(project, "isRepoUpdate", check, async) {
            it ?: return@isEnable null

            val basePath = project?.basePath ?: return@isEnable false
            val manifest = XHelper.readManifest(basePath) ?: return@isEnable false
            XHelper.checkRepoUpdate(true, basePath, manifest.root.name, manifest.root.project.maven)
        }
    }


    private fun isEnable(project: Project?, key: String, check: Boolean, async: (s: Boolean) -> Unit, call: (old: Boolean?) -> Boolean?): Boolean {
        val newKey = "${project.hashCode()}_$key"
        var oldValue = enables[newKey]

        //5秒内不重新检查
        if (!check || System.currentTimeMillis() - (exeTimes[newKey] ?: 0L) < 5000L) return oldValue ?: false
        exeTimes[newKey] = System.currentTimeMillis()
        if (oldValue == null) {
            oldValue = call(oldValue)
        }
        if (oldValue != null) enables[newKey] = oldValue
        executors.execute {
            val value = call(oldValue ?: false)
            if (value != null && value != oldValue) {
                enables[newKey] = value
                async(value)
            }
        }
        return enables[newKey] ?: false
    }

    /**
     * 获取group方法,兼容性处理
     */
    private fun notificationGroup(balloon: Boolean): NotificationGroup {
        return kotlin.runCatching {
            @Suppress("MissingRecentApi")
            if (balloon) NotificationGroup.balloonGroup("XApp") else NotificationGroup.logOnlyGroup("XApp")
        }.getOrElse {
            NotificationGroup("XApp", if (balloon) NotificationDisplayType.BALLOON else NotificationDisplayType.NONE)
        }
    }

    fun key(project: Project?) = project?.basePath?.hashCode()?.toString() ?: "null"
    fun String.toKey(project: Project?) = "${key(project)}_$this"


    fun ideaApp() = ApplicationManager.getApplication()

    fun runAsyn(project: Project? = null, title: String = "Start Task ", cmd: (indicator: ProgressIndicator) -> Unit) {

        val importTask = object : Task.Backgroundable(project, title) {
            override fun run(indicator: ProgressIndicator) {
                val start = System.currentTimeMillis()
//                log("runAsyn start -> $title")
                cmd(indicator)
                indicator.cancel()
//                log("runAsyn end -> $title ${System.currentTimeMillis() - start}")
            }
        }
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(importTask, BackgroundableProcessIndicator(importTask))
    }

    fun invoke(wait: Boolean = false, cmd: () -> Unit) {
        if (wait) ideaApp().invokeAndWait(cmd) else ideaApp().invokeLater(cmd)
    }

    fun invokeAnWait(cmd: Runnable) {
        ideaApp().invokeAndWait(cmd)
    }

    fun cacheDir() = File(System.getProperty("user.home"), ".idea/aex").also { if (!it.exists()) it.mkdirs() }

    fun invokeWrite(cmd: () -> Unit) = invoke { ideaApp().runWriteAction(cmd) }

    fun String.putSp(value: String?, project: Project? = null) {
        if (maps.put(real(this, project), value) != value) invokeWrite { FileUtils.writeProperties(File(cacheDir(), "share.xml"), maps) }
    }

    fun String.getSp(default: String? = null, project: Project? = null) =
        if (maps.containsKey(real(this, project))) maps[real(this, project)] else default

    fun real(key: String, project: Project?): String = key + (project?.basePath?.hashCode()?.toString() ?: "")
    fun <T> Boolean?.getOrElse(get: T, e: T) = if (this == true) get else e

    @JvmStatic
    fun put(key: String, value: String?) = key.putSp(value, null)

    @JvmStatic
    fun get(key: String, default: String? = null) = key.getSp(default, null)

    fun openFile(project: Project, file: File) = FileEditorManager.getInstance(project).openFile(VfsUtil.findFileByIoFile(file, false)!!, true)

    fun log(msg: String?, event: Boolean = false, project: Project? = null) {
        if (msg.isNullOrBlank()) return
        notify(project, msg = msg, group = if (event) BALLOON else LOG)
    }

    fun notify(
        project: Project?,
        title: String = "",
        msg: String = "",
        type: NotificationType = NotificationType.INFORMATION,
        actions: List<AnAction> = emptyList(),
        group: NotificationGroup = BALLOON
    ) = invoke {
        group.createNotification(title, "", msg, type).also { n -> actions.forEach { n.addAction(it) } }.notify(project)
    }

    fun copy(text: String) = Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)

    @JvmStatic
    fun syncVcs(project: Project, projects: Collection<ProjectX>, syncVcs: Boolean, wait: Boolean) = invoke(wait) {
        val dirs = projects.mapNotNull { it.absDir().takeIf { f -> f.exists() } }.map { it.canonicalPath }

        if (syncVcs) {
            //根据导入的CodeRoot目录,自动更改AS的版本管理
            val pVcs: ProjectLevelVcsManagerImpl = ProjectLevelVcsManagerImpl.getInstance(project) as ProjectLevelVcsManagerImpl
            pVcs.directoryMappings = dirs.filter { File(it, ".git").exists() }.map { VcsDirectoryMapping(it, "Git") }
            pVcs.notifyDirectoryMappingChanged()
        } else {
            /**
             * 所有代码的跟目录
             * 对比一下,当前导入的所有工程,是否都在version管理中,如果没有,提示用户进行管理
             */
            /**
             * 所有代码的跟目录
             * 对比一下,当前导入的所有工程,是否都在version管理中,如果没有,提示用户进行管理
             */
            val controlPaths = VcsRepositoryManager.getInstance(project).repositories.map { it.presentableUrl }
            val unHandle = dirs.filter { !controlPaths.contains(it) }
            if (unHandle.isNotEmpty())
                Messages.showMessageDialog(
                    "Those project had import but not in Version Control\n ${unHandle.joinToString { "\n" + it }} \n Please check Setting -> Version Control After Sync!!",
                    "Miss Vcs Control",
                    null
                )
        }
    }

    /**
     * Event Log
     * Build
     */
    fun activateWindow(project: Project, name: String): Boolean = kotlin.runCatching {
        @Suppress("MissingRecentApi")
        val tm = ToolWindowManager.getInstance(project)
        val window = tm.getToolWindow(name)
        if (window != null) invoke { window.activate(null) }
        window != null
    }.getOrElse { true }
}

class XNotifyAction(val name: String, val expire: Boolean = true, val call: (n: Notification) -> Unit) : NotificationAction(name) {
    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
        call(notification)
        if (expire) notification.expire()
    }
}


