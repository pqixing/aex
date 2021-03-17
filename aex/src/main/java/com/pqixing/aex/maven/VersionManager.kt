package com.pqixing.aex.maven

import com.pqixing.XHelper
import com.pqixing.XKeys
import com.pqixing.aex.setting.XSetting
import com.pqixing.base64Decode
import com.pqixing.hash
import com.pqixing.model.BasicCredentials
import com.pqixing.model.Pom
import com.pqixing.tools.FileUtils
import com.pqixing.tools.TextUtils
import java.io.File

class VersionManager(val set: XSetting) {
    val manifest = set.manifest

    /**
     * 当前最新的版本信息
     */
    private val curVersions: VersionModel = VersionModel()

    /**
     * 当前依赖的版本号,集合了tag和分支的设定
     */
    private val compileVersion: VersionModel = VersionModel()

    //开始加载仓库上的版本信息
    val maven = manifest.root.project.maven
    val name = manifest.root.name

    init {
        loadVersionFile(manifest.config.sync || set.gradle.startParameter.taskNames.find { it.contains(XKeys.TASK_TO_MAVEN) } != null)
    }

    fun loadVersionFile(reload: Boolean) {
        //从网络拉取
        val pomFile = XHelper.reloadRepoMetaFile(reload, manifest.dir, name, maven)
        val pomsDir = pomFile.parentFile
        val baseUrl = TextUtils.append(arrayOf(maven.url, maven.group.replace(".", "/"), name))

        if (reload) {
            set.println("Fetch : ${baseUrl}/${XKeys.XML_META} \nTo    : ${pomFile.absolutePath}")
        } else XHelper.post {
            if (XHelper.checkRepoUpdate(false, manifest.dir, name, maven)) {
                set.println("Fetch : Some components have been updated")
            }
        }

        curVersions.clear()
        compileVersion.clear()

        val credentials = maven.asCredentials()
        val hash = manifest.branch.hash()
        var tag: VersionModel? = null
        var sync: VersionModel? = null
        //按照第二位进行排序
        for (version in XHelper.parseMetadata(pomFile).versions.sortedBy { it.split(".").getOrNull(1) }) {
            val file = File(pomsDir, "$version.txt")
            val url = TextUtils.append(arrayOf(baseUrl, version, "${name}-$version.txt"))
            when {
                version.startsWith("tag.") -> {
                    if (hash == version.substringAfterLast(".")) {
                        tag = VersionModel().setUrl(file, url, credentials)
                    }
                }
                version.startsWith("sync.") -> {
                    curVersions.clear()
                    sync = VersionModel().setUrl(file, url, credentials)
                }
                version.startsWith("log.") -> {
                    val spilt = version.substringAfterLast(".").base64Decode().split("=")
                    curVersions[spilt[0]] = spilt[1].toInt()
                }
            }
        }
        if (sync != null) {
            val syncVersion = sync.check()
            syncVersion.putAll(curVersions)
            curVersions.clear()
            curVersions.putAll(sync)
        }
        //compile所依赖的版本号
        compileVersion.putAll(curVersions)
        //如果使用分支,添加当前分支的tag文件
        if (manifest.usebr && tag != null) {
            compileVersion.putAll(tag.check())
        }
        //添加设定的目标版本号文件
        val targetUrl = manifest.config.mapping
        if (targetUrl.isNotEmpty()) {
            val target = VersionModel().setUrl(File(pomsDir, targetUrl.hash() + ".txt"), targetUrl, credentials)
            compileVersion.putAll(target.check())
        }

        FileUtils.writeProperties(File(pomsDir, "current.properties"), curVersions)
        FileUtils.writeProperties(File(pomsDir, "compiles.properties"), compileVersion)
        set.println("[ current.properties , compiles.properties ] -> ${pomsDir.absolutePath}")
    }

    //pom对象,在内存中缓存一份,防止频繁读取
    private var pomCache: HashMap<String, Pom> = hashMapOf()

    /**
     * 检查改分支是否存在版本号
     */
    fun checkBranchVersion(groupId: String, module: String): Boolean =
        curVersions.findVersion(groupId, module, "") != null

    /**
     * 获取仓库aar中，exclude的传递
     */
    fun getPom(mavenUrl: String, groupId: String, module: String, version: String, credentials: BasicCredentials?): Pom {
        val pomUrl =
            TextUtils.append(arrayOf(mavenUrl, groupId.replace(".", "/"), module, version, "$module-$version.pom"))

        if (!pomUrl.startsWith("http")) {//增加对本地Maven地址的支持
            return XHelper.parsePomExclude(FileUtils.readText(File(pomUrl)) ?: "")
        }
//
        val pomKey = TextUtils.numOrLetter(pomUrl)
        var pom = pomCache[pomKey]
        if (pom != null) return pom
//
        val pomDir = File(set.gradle.gradleUserHomeDir, "cache/pomCache")
        val pomFile = File(pomDir, pomKey)
        pom = if (pomFile.exists()) XHelper.parsePomExclude(FileUtils.readText(pomFile) ?: "")
        else {
            val ponTxt = XHelper.readUrlTxt(pomUrl, credentials)
            FileUtils.writeText(pomFile, ponTxt)
            XHelper.parsePomExclude(ponTxt)
        }
        pomCache[pomKey] = pom
        return pom
    }

    /**
     * 返回上传的版本号
     */
    fun findUploadVersion(groupId: String, name: String, v: String): String {
        val find = curVersions.findVersion(groupId, name, v) ?: return "$v.0"
        val last = find.substringAfterLast(".").toIntOrNull() ?: 0
        val base = find.substringBeforeLast(".")
        return "$base.${last + 1}"

    }

    fun findCompileVersion(groupId: String, name: String, v: String): String? =
        compileVersion.findVersion(groupId, name, v)
}

/**
 * 版本储存map
 * graoupId:name:version=v
 * etc:com.pqixing:x:1.1.0 -> com.pqixing:x:1.1=0
 */
class VersionModel : HashMap<String, Int>() {
    private var file: File? = null
    private var url: String = ""
    private var credentials: BasicCredentials? = null
    fun setUrl(file: File, url: String, credentials: BasicCredentials? = null): VersionModel {
        this.file = file
        this.url = url
        this.credentials = credentials
        return this
    }

    /**
     * 查找出最合适的版本号
     * 如果没有,则直接返回null
     * @param v 查找版本号,  etc  v = 1 , 则会查出所有版本号里首位是1的数据
     */
    fun findVersion(groupId: String, name: String, v: String): String? {
        val key = "$groupId:$name:"
        //所有匹配规则的版本号
        return filter { it.key.startsWith(key) }
            .map { it.key.substringAfterLast(":") + "." + it.value }
            .filter { v.isEmpty() || it == v || it.startsWith("$v.") }
            //返回所有匹配的版本中,最大的版本号
            .sortedWith { v1, v2 -> TextUtils.compareVersion(v1, v2) }
            .firstOrNull()
    }

    fun check(): VersionModel {
        val f = file ?: return this
        if (!f.exists() && url.isNotEmpty()) {
            //下载文件
            XHelper.urlToFile(url, f, credentials)
        }
        if (f.exists()) {
            this += FileUtils.readProperties(f)
                .mapNotNull { it.value.toString().toIntOrNull()?.let { int -> it.key.toString() to int } }.toMap()
        }

        file = null
        return this
    }
}
