package com.pqixing.aex.maven

import com.pqixing.aex.android.tasks.BaseTask
import com.pqixing.aex.model.ModuleEx
import com.pqixing.aex.utils.setting
import com.pqixing.tools.FileUtils
import java.io.File

open class ToMavenTask : BaseTask() {
    val pro = project
    val set = project.setting()
    val module = set.manifest.findModule(project.name) as ModuleEx
    val local = module.localEx()
    var maven = module.project.maven
    val forMaven = local.forMmaven
    val lastVersion =
        "${forMaven.version.substringBeforeLast(".")}.${forMaven.version.substringAfterLast(".").toInt() - 1}"
    var resultStr = ""

    //假上传,不执行实际的上传,只保证tomaven任务成功执行
    var fakeUpload = false

    override fun prepare() {
        super.prepare()
        val uploadTask = maven.task
        val up1 = project.tasks.findByName(uploadTask)
        val up2 = project.rootProject.tasks.findByName(uploadTask)
        fakeUpload = isRepeat() && !unCheck.contains(TYPE_REPEAT)
        if (!fakeUpload && up1 != null && up2 != null) {
            up2.mustRunAfter(up1)
            this.dependsOn(up2, up1)
        }
    }

    /**
     * toMaven时,忽略检查项目
     * oldType 1,  不校验是否和上次代码是否相同,允许提交重复
     * oldType 2,  不检验本地是否存在未提交修改
     * oldType 3,  不检验分支和root工程是否一致
     */
    var unCheck: String = set.manifest.config.ignore
    val TYPE_REPEAT = "1"
    val TYPE_CLEAN = "2"
    val TYPE_BRANCH = "3"

    /**
     * 当准备运行该任务之前，先检测
     */
    override fun whenReady() {

        //如果是可以正常上传的，检测是否合法
        checkLocal()

        checkLose()

        //检查本地代码是否有未提交
        check(
            !set.gitHelper.checkIfClean(local.git, getRelativePath(module.path)),
            TYPE_CLEAN, "${module.name} Code not clean"
        )
        //检查本地分支代码是否一致
        check(
            local.branch != set.manifest.branch,
            TYPE_BRANCH, "${module.name} branch ${local.branch} diff from root ${set.manifest.branch}"
        )

        resultStr = "${forMaven.group}:${forMaven.artifactId}:${if (fakeUpload) lastVersion else forMaven.version}"
        FileUtils.delete(project.buildDir)

        //设置上传的版本号的文件
        val rootPro = pro.rootProject
        val rootUploadFile = File(rootPro.buildDir, "${rootPro.name}.txt")
        FileUtils.writeText(rootUploadFile, resultStr)
    }

    fun getRelativePath(path: String): String? {
        val of = path.indexOf("/")
        return if (of > 0) return path.substring(of + 1) else null
    }

    /**
     * 检查上一个提交版本的日志，如果日志一样，则不允许上传
     */
    private fun isRepeat(): Boolean = kotlin.runCatching {
        set.vm.getPom(
            forMaven.url,
            forMaven.group,
            forMaven.artifactId,
            lastVersion
        ).name
    }.getOrNull() == forMaven.name

    /**
     * 检查是否需要忽略错误
     * @return 返回结果 0,uncheckType, <0 , request check
     */
    private fun check(request: Boolean, type: String, msg: String) {
        if(!request) return
        if (unCheck.contains(type)) set.println(msg) else set.writeResult(msg, true)
    }

    private fun checkLose() {
        local.depend.loses.takeIf { it.isNotEmpty() }?.let {
            set.writeResult("${project.name}  There are some dependency lose!! -> $it", true)
        }
    }

    private fun checkLocal() {
        local.depend.locals.takeIf { it.isNotEmpty() }?.let {
            set.writeResult("${project.name} Contain local project, please remove it before upload -> $it", true)
        }
    }


    override fun runTask() {
            //假上传
        Thread.sleep(1000)
        if (fakeUpload) set.println("Fake upload task , version will not change")
        //更新本地版本信息
        set.vm.loadVersionFile(true)
        set.writeResult(resultStr)
    }
}