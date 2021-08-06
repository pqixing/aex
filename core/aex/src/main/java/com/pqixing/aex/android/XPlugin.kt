package com.pqixing.aex.android

import com.pqixing.*
import com.pqixing.aex.android.tasks.ApkBuildResult
import com.pqixing.aex.android.tasks.BaseTask
import com.pqixing.aex.maven.IndexRepoTask
import com.pqixing.aex.maven.ToMavenTask
import com.pqixing.aex.model.ModuleEx
import com.pqixing.aex.setting.XSetting
import com.pqixing.aex.utils.register
import com.pqixing.aex.utils.setting
import com.pqixing.model.BrOpts
import com.pqixing.model.impl.ModuleX
import com.pqixing.tools.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.*

/**
 * Created by pqixing on 17-12-20.
 * 管理代码工程的导入，maven仓库的依赖的版本生成
 */

open class XPlugin : Plugin<Project> {
    lateinit var module: ModuleEx
    lateinit var set: XSetting

    //同步后执行
    val doAfterList: MutableList<(p: Project) -> Unit> = mutableListOf()
    override fun apply(pro: Project) {
        pro.register(this)

        set = pro.setting()
        module = set.manifest.findModule(pro.name) as? ModuleEx ?: return
        pro.extensions.add("aex_module", module)
        pro.extensions.add("aex", set.manifest)

        pro.afterEvaluate { doAfterList.forEach { f -> f(it) } }

        forRepo(pro)

        forMaven(pro)
        //解析依赖
        if (module.reqDps()) forDps(pro)

        //如果是Android工程，添加插件处理
        if (module.isAndroid()) forAndroid(pro)

        //生成忽略文件
        ignore(pro, module)
        doAfterList.add { it.tasks.findByName("clean")?.doLast { FileUtils.delete(File(pro.projectDir, "build")) } }
    }

    private fun forDps(pro: Project) = doAfterList.add {
        val resolveUrl = module.localEx().depend.resolveDps()
        val dpsFile = File(pro.buildDir, XKeys.GRADLE_DEPENDENCIES)
        pro.apply(mapOf("from" to FileUtils.writeText(dpsFile, resolveUrl, true)))
    }

    private fun forAndroid(pro: Project) {

        val type = module.typeX()

        val assemble = type.app() || module.name == set.manifest.config.assemble
        //根据情况进行不同的Android插件依赖
        pro.apply(mapOf<String, String>("plugin" to if (assemble) "com.android.application" else "com.android.library"))

        //如果不是app模块运行，设置ApplicationId
        if (assemble && !type.app()) {
            type.mockRuns.forEach { module.localEx().depend.include("api '${it}'") }
            val idPath = FileUtils.writeText(
                File(pro.buildDir, "applicationId.gradle"),
                "android.defaultConfig.applicationId 'com.${module.localEx().branch.pure()}.${module.name.pure()}'"
            )
            pro.apply(mapOf("from" to idPath))
        }
        //开始注解切入
        if (assemble) pro.afterEvaluate {
            val buildApk = ApkBuildResult(set, pro)
            for (task in pro.tasks.filter { it.name.startsWith("assemble") }) {
                task.doLast { buildApk.onAssemble(it.name) }
            }
        }
    }

    private fun forRepo(pro: Project) {
        val repos = pro.repositories
        val dps = module.dps()
        val depends = module.sorted().filter { dps.contains(it.name) }.map { it.project.maven }.distinctBy { it.url }
        //所有依赖到的maven仓库,添加依赖
        for (dp in depends) repos.maven { maven ->
            maven.url = dp.uri()
            //如果不允许匿名访问,添加用户名和密码
            if (!dp.anonymous) maven.credentials {
                it.username = dp.user
                it.password = dp.psw.real()
            }
        }
    }

    private fun forMaven(pro: Project) {
        val forMaven = module.localEx().forMmaven
        //添加toMaven数据
        pro.extensions.add("forMaven", forMaven)

        //准备上传到仓库的模块,则创建ToMavenTask
        pro.tasks.create("ToMaven", if (forMaven.toMaven) ToMavenTask::class.java else DefaultTask::class.java)

        if (pro != pro.rootProject) return


        val maven = module.project.maven
        //如果执行的 IndexMavenTask
        val syncMaven = BaseTask.matchTask(XKeys.TASK_SYNC_MAVEN, set.gradle)
        pro.tasks.create(XKeys.TASK_SYNC_MAVEN, if (syncMaven) IndexRepoTask::class.java else DefaultTask::class.java)
        forMaven.group = maven.group
        forMaven.file = File(pro.buildDir, "${module.name}.txt")

        if (syncMaven) {
            val date = set.gitHelper.dateVersion.format(Date())
            //veresion设置 0.time.
            val target = BrOpts(set.manifest.config.opts).target
            if (target == null) {
                forMaven.version = "sync.${date}"
            } else {
                forMaven.version = "tag.${date}." + target.hash()
            }
            forMaven.toMaven = true
            return
        }

        //等待ToMaven的模块
        val m = (set.gradle.startParameter.taskNames.find { it.endsWith(":ToMaven") }
            ?.substringBeforeLast(":")?.replace(":", "")
            ?.let { set.manifest.findModule(it) } as? ModuleEx) ?: return

        val childMaven = m.localEx().forMmaven
        //设置待上传的模块版本号
        val version = set.vm.findUploadVersion(childMaven.group, childMaven.artifactId, m.version)
        childMaven.version = version
        //加载上传description信息,用来记录当前版本的git和额外信息
        childMaven.name = "${m.localEx().last?.name}:${m.localEx().last?.commitTime}"
        childMaven.toMaven = true

        //设置root工程的版本为待上传的版本信息
        val date = set.gitHelper.dateVersion.format(Date())
        val desc =
            "${childMaven.group}:${childMaven.artifactId}:${version.substringBeforeLast(".")}=${version.substringAfterLast(".")}"
        forMaven.version = "log.${date}.${desc.base64Encode()}"
        forMaven.toMaven = true


    }

    fun ignore(project: Project, module: ModuleX) {
        val defSets = module.typeX().ignores.toMutableSet()

        val ignoreFile = project.file(XKeys.GIT_IGNORE)

        val old = FileUtils.readText(ignoreFile) ?: ""
        defSets -= old.lines().map { it.trim() }

        if (defSets.isEmpty()) return
        val txt = StringBuilder(old)
        defSets.forEach { txt.append("\n$it") }
        FileUtils.writeText(ignoreFile, txt.toString())
    }

}

