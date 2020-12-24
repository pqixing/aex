package com.pqixing.aex.setting

import com.pqixing.aex.android.DependManager
import com.pqixing.aex.android.XPlugin
import com.pqixing.aex.model.LocalEx
import com.pqixing.aex.model.MavenEx
import com.pqixing.aex.model.ModuleEx
import com.pqixing.aex.model.ProjectEx
import com.pqixing.model.define.IMaven
import com.pqixing.model.impl.ManifestX
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

    //代理的build文件名字
    val buildFileName = "build/${manifest.config.build}/merge.gradle"
    fun startImport() {
        val setting: Settings = set.setting

        //自动抓取工程导入
        val imports = manifest.importModules().mapNotNull { it as? ModuleEx }
        val root = manifest.root as ModuleEx
        //合并根目录的代码
        setting.rootProject.name = root.name
        setting.rootProject.buildFileName = buildFileName

        //检出主模块
        checkoutModule(root, manifest.branch)
        //设置整个工程归属的分支
        if (manifest.branch.isEmpty()) {
            manifest.branch = (root.project as ProjectEx).repo?.repository?.branch ?: "master"
        }
        //尝试下载工程,hook build.gradle文件,//添加include配置
        val checkouts = imports.filter { checkoutModule(it, manifest.branch) }
        set.println("--------------------------------- Start include : ${checkouts.map { it.name }} --------------------------------- ")

        //导入basic模块和import的模块
        checkouts.forEach {
            setting.include(":" + it.name)
            val pro = setting.findProject(":" + it.name)!!
            pro.projectDir = it.absDir()
            pro.buildFileName = buildFileName
        }


        //hook配置的工程的build.gradle,合并原始build.gradle与预设的build.gradle文件,生成新的初始化文件，注入插件进行开发设置
        setting.gradle.beforeProject { pro -> beforeProject(pro) }
    }

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

        val files = mutableListOf<File>()
        files += type.merge_absolute.map { File(it) }
        files += type.merge_root.map { File(rootDir, it) }
        files += type.merge_git.map { File(gitDir, it) }
        files += type.merge_module.map { File(moduleDir, it) }

        val merges = files.filter { it.exists() && it.isFile }.distinctBy { it.absolutePath }
        set.println("       merge : ${getPath(files)}  ->  ${getPath(merges)}")
        if (merges.isNotEmpty()) {
            val regexs = type.replaces.map { Regex(it) }
            FileUtils.mergeFile(File(moduleDir, buildFileName), merges)
            {
                var s = it
                regexs.forEach { f -> s = s.replace(f, "") }
                s
            }
        }
    }

    fun getPath(files: List<File>): List<String> {
        val rootPath = set.rootDir.absolutePath + "/"
        return files.map { it.absolutePath.replace(rootPath, "") }
    }

    /**
     * 尝试切出模块
     */
    private fun checkoutModule(module: ModuleEx, branch: String): Boolean {
        set.println("\n--------------------------------- Start checkout : ${module.name} , $branch ---------------------------------")
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
        val forMmaven =
                m.ex(module.name, version = "", group = module.group(), psw = set.gitHelper.getPsw(m.psw))
        module.localEx = LocalEx(git, git.repository.branch, forMmaven, DependManager(set, module), last)

        if (module.localEx().branch != manifest.branch && manifest.usebr) {
            set.println("::: ${module.name} branch is ${module.localEx().branch} , does not match ${manifest.branch}!!!")
        }
    }

    private fun IMaven.ex(name: String = this.name,
                          url: String = this.url,
                          user: String = this.user,
                          psw: String = this.psw,
                          group: String = this.group,
                          version: String = this.version,
                          task: String = this.task): MavenEx {
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