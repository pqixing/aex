package com.pqixing.aex.setting

import com.pqixing.XHelper
import com.pqixing.XKeys
import com.pqixing.aex.android.tasks.BaseTask
import com.pqixing.aex.maven.VersionManager
import com.pqixing.aex.model.ManifestEx
import com.pqixing.aex.model.ProjectEx
import com.pqixing.aex.utils.AEX
import com.pqixing.aex.utils.GitHelper
import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import java.io.File

/**
 * 设置页面插件
 */
open class XSetting : Plugin<Settings> {

    var plugins = mutableMapOf<Project, MutableSet<Plugin<*>>>()

    lateinit var manifest: ManifestEx
    lateinit var gradle: Gradle
    lateinit var setting: Settings
    lateinit var rootDir: File
    val gitHelper: GitHelper by lazy { GitHelper(this) }
    val vm: VersionManager by lazy { VersionManager(this) }
    fun println(msg: String, write: Boolean = manifest.config.log) {
        if (write) kotlin.io.println(msg)
    }

    override fun apply(setting: Settings) {
        val start = System.currentTimeMillis()
        this.setting = setting
        this.gradle = setting.gradle
        this.rootDir = setting.rootDir
        AEX.register(this)

        manifest = ManifestEx(rootDir.absolutePath)
        gitHelper.open(rootDir)?.let { git ->
            manifest.branch = git.repository.branch
            gitHelper.close(git)
        }
        setting.extensions.add("aex", manifest)


        //同步结束,开始进行导入处理
        gradle.settingsEvaluated { _ ->
            //同步另外两个文件
            for (f in listOf(
                File(rootDir, "local.gradle"), File(rootDir, ".idea/local.gradle")
            ).filter { it.exists() }) {
                setting.apply(mapOf("from" to f.absolutePath))
            }
            println("---------------------------------  AEX Loader Start : https://github.com/pqixing/aex  --------------------------------- \n")
            //同步结束
            manifest.afterEvaluated(this)
            //输入结果到缓存目录,ide读取
            if (manifest.config.build != "ide") XHelper.writeManifest(manifest)
            //进行代码导入
            ImportScript(manifest, this).startImport()
        }

        //在task开始前，执行任务的检查
        gradle.afterProject { pro -> pro.tasks.mapNotNull { it as? BaseTask }.forEach { it.prepare() } }
        gradle.taskGraph.whenReady { g ->
            val taskGraph = g.allTasks
            if (taskGraph.isNotEmpty()) {
                println("TaskGraph -> ${taskGraph.joinToString(",") { i -> i.project.name + ":" + i.name }}")
            }
            g.allTasks.mapNotNull { it as? BaseTask }.forEach { it.whenReady() }
        }


        gradle.addListener(object : BuildAdapter() {
            override fun buildFinished(result: BuildResult) {
                manifest.projects.mapNotNull { it as? ProjectEx }.forEach { gitHelper.close(it.repo) }
                plugins.clear()
                AEX.unregister(this@XSetting)
                println("Build Finish Spend: ${System.currentTimeMillis() - start}")
            }
        })
    }

    fun allProjects() = gradle.rootProject.allprojects.map { it.name }.toSet()

    /**
     * 输出结果，用于Ide的交互获取
     * @param exitCode 退出标记 0 表示正常退出，1表示异常退出
     */
    fun writeResult(msg: String, exit: Boolean = false) = writeResult(mapOf("msg" to msg), exit)

    fun writeResult(params: Map<String, String>, exit: Boolean = false) {
        val sb = StringBuilder(XKeys.PREFIX_IDE_LOG).append("?")
        if (params.isNotEmpty()) {
            params.forEach { (t, u) -> sb.append(t).append("=").append(u).append("$") }
            sb.deleteCharAt(sb.length - 1)
        }
        val msg = sb.toString()
        println(msg, true)
        if (exit) throw  GradleException(msg)
    }
}


