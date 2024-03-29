package com.pqixing.aex.setting

import com.pqixing.XKeys
import com.pqixing.aex.android.DependManager
import com.pqixing.aex.android.XPlugin
import com.pqixing.aex.model.LocalEx
import com.pqixing.aex.model.MavenEx
import com.pqixing.aex.model.ModuleEx
import com.pqixing.aex.model.ProjectEx
import com.pqixing.model.define.IMaven
import com.pqixing.model.impl.ManifestX
import com.pqixing.real
import com.pqixing.tools.FileUtils
import org.eclipse.jgit.api.Git
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import java.io.File

/**
 * 导入脚本
 */
class ImportScript(val manifest: ManifestX, val set: XSetting) {

    val gradle = set.gradle
    val srcCreate = SourceCreate(set, manifest)

    fun startImport() {
        val setting: Settings = set.setting

        //自动抓取工程导入
        val imports = manifest.importModules().mapNotNull { it as? ModuleEx }
        val root = manifest.root as ModuleEx
        //合并根目录的代码
        setting.rootProject.name = root.name
        setting.rootProject.buildFileName = buildFileName(root.name)

        //检出主模块
        checkoutModule(root, manifest.branch)
        //设置整个工程归属的分支
        if (manifest.branch.isEmpty()) {
            manifest.branch = (root.project as ProjectEx).repo?.repository?.branch ?: "master"
        }
        //尝试下载工程,hook build.gradle文件,//添加include配置
        val checkouts = imports.filter { checkoutModule(it, manifest.branch) }
        val checkFails = (imports - checkouts)
        if (checkFails.isNotEmpty()) {
            set.writeResult("Checkout module fail : ${checkFails.joinToString(",") { it.name }}", true)
        }

        //初始化仓库版本,方便日志打印
        val initVM = set.vm.name
        set.println("--------------------------------- Start Import : ${checkouts.map { it.name }} --------------------------------- ")

        //导入basic模块和import的模块
        checkouts.forEach {
            setting.include(":" + it.name)
            val pro = setting.findProject(":" + it.name)!!
            pro.projectDir = it.absDir()
            pro.buildFileName = buildFileName(it.name)
        }
        val flutters = checkouts.filter { it.typeX().flutter() }.map { it.name }
        //hook配置的工程的build.gradle,合并原始build.gradle与预设的build.gradle文件,生成新的初始化文件，注入插件进行开发设置
        setting.gradle.beforeProject { pro ->
            //优先evaluation flutter子项目
            if (pro != pro.rootProject && flutters.isNotEmpty() && !flutters.contains(pro.name)) for (name in flutters) {
                pro.evaluationDependsOn(":$name")
            }
            beforeProject(pro)
        }
    }

    fun buildFileName(module: String) = "build/${manifest.config.build}/$module.gradle"

    /**
     * 在开始同步工程前处理
     * 1,合并build.gradle文件
     * 2,对Library和Java模块进行Maven数据处理
     *
     */
    private fun beforeProject(pro: Project) {
        //所有项目添加XPlugin依赖
        pro.buildDir = File(pro.buildDir, manifest.config.build)
        pro.pluginManager.apply(XPlugin::class.java)
    }

    /**
     * hook工程的build.gradle文件
     */
    private fun hookBuildFile(module: ModuleEx?) {
        val type = module?.typeX() ?: return
        val rootDir = set.rootDir
        val gitDir = module.project.absDir()
        val moduleDir = module.absDir()

        //当前需要合并的文件
        val files = type.merges.mapNotNull { m ->
            val split = m.split(",")
            if (split.size == 2) Pair(split.first(), split.last()) else null
        }.mapNotNull {
            when (it.first) {
                "absolute" -> File(it.second)
                "root" -> File(rootDir, it.second)
                "git" -> File(gitDir, it.second)
                "module" -> File(moduleDir, it.second)
                else -> null
            }
        }

        if (module.reqDps()) {
            set.println("        depend : ${moduleDir.absolutePath}/build/${manifest.config.build}/${XKeys.GRADLE_DEPENDENCIES}")
        }

        val merges = files.filter { it.exists() && it.isFile }.distinctBy { it.absolutePath }
        if (merges.isNotEmpty()) {
            val result = File(moduleDir, buildFileName(module.name))
            FileUtils.mergeFile(result, merges) { replace(it, type.replaces.map { m -> Regex(m) }) }
            set.println("        merge  : ${result.absolutePath} <-  ${getPath(merges)} ")
        }
    }

    private fun replace(it: String, regexs: List<Regex>): String {
        val sb = StringBuilder(it)
        regexs.forEach { sb.replace(it, "") }
        return sb.toString()
    }

    fun getPath(files: List<File>): List<String> {
        val rootPath = set.rootDir.absolutePath + "/"
        return files.map { it.absolutePath.replace(rootPath, "") }
    }

    /**
     * 尝试切出模块
     */
    private fun checkoutModule(module: ModuleEx, branch: String): Boolean {
        set.println("\n> Start checkout : module = ${module.name} ; branch = $branch")
        val project = module.project as ProjectEx

        val projectDir = project.absDir()

        if (project.repo == null) {
            project.repo = set.gitHelper.open(projectDir) ?: set.gitHelper.clone(module, branch.takeIf { it.isNotEmpty() } ?: "master")
                    ?: return false
        }
        //加载模块信息
        initLocalInfo(module, project.repo!!, module.path)

        //如果当前模块不存在,尝试创建模块源码
        if (project.manifest.config.create) {
            srcCreate.tryCreateSrc(module)
        }
        //更换build.gradle文件
        hookBuildFile(module)
        return module.absDir().exists()
    }


    private fun initLocalInfo(module: ModuleEx, git: Git, path: String) {
        val command = git.log().setMaxCount(1)
        if (path.isNotEmpty()) {
            command.addPath(path)
        }
        val last = command.call().firstOrNull()
        val m = module.project.maven
        val forMmaven = m.ex(module.name, version = "", group = module.group(), psw = m.psw.real())
        module.localEx = LocalEx(git, git.repository.branch, forMmaven, DependManager(set, module), last)

        set.println("        commit : ${last?.name()} , ${last?.authorIdent} , ${last?.fullMessage?.trim()}")
        if (module.localEx().branch != manifest.branch && manifest.usebr) {
            set.println("::: ${module.name} branch is ${module.localEx().branch} , does not match ${manifest.branch}!!!")
        }
    }

    private fun IMaven.ex(
        name: String = this.name,
        url: String = this.url,
        user: String = this.user,
        psw: String = this.psw,
        group: String = this.group,
        version: String = this.version,
        task: String = this.task
    ): MavenEx {
        val ex = MavenEx("")
        ex.artifactId = name
        ex.url = url
        ex.user = user
        ex.psw = psw
        ex.group = group
        ex.version = version
        ex.task = task
        return ex
    }
}