package com.pqixing.aex.maven

import com.pqixing.XHelper
import com.pqixing.XKeys
import com.pqixing.aex.android.tasks.BaseTask
import com.pqixing.aex.model.ModuleEx
import com.pqixing.aex.utils.setting
import com.pqixing.aexEncode
import com.pqixing.model.BrOpts
import com.pqixing.tools.FileUtils
import com.pqixing.tools.TextUtils

/**
 * 同步工程的代码和分支,为了Jekens 构建使用
 */
open class IndexRepoTask : BaseTask() {
    val set = project.setting()
    val manifest = set.manifest
    val module = manifest.findModule(project.name) as ModuleEx
    val maven = manifest.root.project.maven
    val forMaven = module.localEx().forMmaven
    val opts = BrOpts(manifest.config.opts)
    override fun prepare() {
        super.prepare()
        val uploadTask = maven.task
        val up1 = project.tasks.findByName(uploadTask)
        if (up1 != null) this.dependsOn(up1)
    }

    override fun whenReady() {
        super.whenReady()

        //只有设置了index的tag名称,才忽略指定分支的值,全量index不允许忽略
        parseVersion(opts.brs.takeIf { opts.target?.isNotEmpty() == true } ?: emptyList())
    }

    fun allBranches(): Set<String> {
//        set.vm.loadVersionFile(true)
        val branchs = mutableSetOf<String>()
//        branchs += set.vm.allVersions().keys.mapNotNull { it.split(":").firstOrNull()?.substringAfterLast(".") }
        branchs += manifest.fallbacks
        branchs += manifest.branch
        branchs += module.localEx().git.branchList().call().map { it.name.substringAfterLast("/") }
        return branchs.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * 从网络获取最新的版本号信息
     */
    private fun parseVersion(excludes: List<String> = emptyList()) {
        val versions = HashMap<String, Int>()
        set.println("parseVersion  start -> $opts")
        val start = System.currentTimeMillis()
        //上传版本号到服务端
        reloadVersion(allBranches().minus(excludes).toMutableSet(), versions)

        set.println("parseVersion  end -> ${System.currentTimeMillis() - start} ms -> $excludes")

        FileUtils.writeProperties(forMaven.file!!, versions)
    }

    override fun runTask() {
        //等待两秒
        Thread.sleep(300)
        //更新本地版本信息
        set.vm.loadVersionFile(true)
        val url = TextUtils.append(
            arrayOf(
                forMaven.url,
                forMaven.group.replace(".", "/"),
                forMaven.artifactId,
                forMaven.version,
                "${forMaven.artifactId}-${forMaven.version}.txt"
            )
        )
        set.println("upload to :$url")

        set.writeResult(forMaven.file?.absolutePath ?: "")
    }

    private fun reloadVersion(branchs: MutableSet<String>, versions: HashMap<String, Int>) {
        branchs.add("")
        val modules = manifest.sorted().minus(manifest.root).mapNotNull { it as? ModuleEx }
        set.println("Start Fetch :  branches = $branchs , modules=${modules.map{it.name}}")
        for (b in branchs) for (m in modules) {
            val maven = m.project.maven
            val url = TextUtils.append(arrayOf(maven.url, maven.group.replace(".", "/"), b.aexEncode(), m.name, XKeys.XML_META))
            val metaStr = XHelper.readUrlTxt(url)
            set.println("$url -> ${metaStr.length}")
            if (metaStr.isNotEmpty()) kotlin.runCatching {
                val meta = XHelper.parseMetadata(metaStr)
                set.println("request -> $url -> ${meta.versions}")
                addVersion(versions, meta.groupId.trim(), meta.artifactId.trim(), meta.versions)
            }
        }
    }

    /**
     * 把每个版本的最后版本号添加
     */
    private fun addVersion(curVersions: HashMap<String, Int>, groupId: String, artifactId: String, version: List<String>) {
//        Tools.println("addVersion -> $groupId $artifactId $version")
        //倒叙查询
        for (i in version.size - 1 downTo 0) {
            val v = version[i]
            val bv = v.substringBeforeLast(".")
            val lv = v.substringAfterLast(".")
            val key = "$groupId:$artifactId:$bv"
            val latValue = curVersions[key] ?: 0
            val newValue = lv.toIntOrNull() ?: 0
            if (newValue >= latValue) {
                curVersions[key] = newValue
            }
        }
    }

}