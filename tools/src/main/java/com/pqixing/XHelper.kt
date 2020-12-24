package com.pqixing

import com.alibaba.fastjson.JSON
import com.pqixing.codec.AexURLDecoder
import com.pqixing.codec.AexURLEncoder
import com.pqixing.codec.digest.DigestUtils
import com.pqixing.model.Metadata
import com.pqixing.model.Pom
import com.pqixing.model.define.IMaven
import com.pqixing.model.impl.ManifestX
import com.pqixing.tools.FileUtils
import com.pqixing.tools.TextUtils
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import javax.xml.parsers.SAXParserFactory

fun String.hash(): String = if (isEmpty()) this else DigestUtils.md5Hex(this)
fun String.envValue(): String? = try {
    System.getProperty(this)
} catch (e: Exception) {
    null
}

//加密groupName,防止报错
val chars: List<Char> = ('0'..'9') + ('a'..'z') + ('A'..'Z')
fun String.aexEncode() = AexURLEncoder.encode(this, "utf-8")
fun String.aexDecode() = AexURLDecoder.decode(this, "utf-8")

fun String.base64Encode() =
    String(Base64.getEncoder().encode(this.toByteArray(Charsets.UTF_8)), Charsets.UTF_8).replace("=", "")//把末尾的等号去掉,不影响解码

fun String.base64Decode() =
    String(Base64.getDecoder().decode(this.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)

fun String.pure() = TextUtils.numOrLetter(this).toLowerCase(Locale.CHINA)

object XHelper {
    val executors: ScheduledExecutorService by lazy { Executors.newScheduledThreadPool(1) }
    fun testEncode(str: String) = str.aexEncode()
    fun testDecode(str: String) = str.aexDecode()
    fun post(delay: Long = 0L, cmd: () -> Unit) = executors.schedule(cmd, delay, TimeUnit.MILLISECONDS)

    @JvmStatic
    fun post(delay: Long, cmd: Runnable) = executors.schedule(cmd, delay, TimeUnit.MILLISECONDS)


    /**
     * 解析出pom文件的all exclude依赖
     */
    fun parsePomExclude(pomText: String): Pom = if (pomText.isEmpty()) Pom() else parsePomBySax(pomText.byteInputStream())

    private fun parsePomBySax(ins: InputStream): Pom {
        val pom = Pom()
        val exludes: HashMap<String, Int> = hashMapOf()

        SAXParserFactory.newInstance().newSAXParser().parse(ins, object : DefaultHandler() {
            val cache = hashMapOf<String, String>()
            var loadDepend = false
            var nodeName: String? = null
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                nodeName = qName
                if (qName == "dependency" || qName == "exclusion") {
                    loadDepend = true
                    cache.clear()
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                if (nodeName == null || ch == null) return
                val value = StringBuilder().append(ch, start, length).toString()
                if (loadDepend) cache[nodeName!!] = value else when (nodeName) {//加载pom文件的信息
                    "groupId" -> pom.groupId = value
                    "artifactId" -> pom.artifactId = value
                    "name" -> pom.name = value
                    "version" -> pom.version = value
                    "packaging" -> pom.packaging = value
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                nodeName = null
                when (qName) {
                    "dependency" -> pom.dependency.add("${cache["groupId"]}:${cache["artifactId"]}:${cache["version"]}")
                    "exclusion" -> {
                        val key = "${cache["groupId"]}:${cache["artifactId"]}"
                        exludes[key] = ((exludes[key] ?: 0) + 1)
                    }
                    "dependencies" -> loadDepend = false
                }
            }
        })

        pom.allExclude += exludes.filter { it.value == pom.dependency.size }.keys
        return pom
    }


    fun parseMetadata(txt: String?): Metadata = if (txt?.isNotEmpty() == true) parseMetaBySax(txt.byteInputStream()) else Metadata()

    fun parseMetadata(file: File?): Metadata = if (file?.exists() == true) parseMetaBySax(file.inputStream()) else Metadata()

    fun parseMetaBySax(ins: InputStream): Metadata {
        val meta = Metadata()
        SAXParserFactory.newInstance().newSAXParser().parse(ins, object : DefaultHandler() {
            var nodeName: String? = null
            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                nodeName = qName
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                if (nodeName == null || ch == null) return
                val value = StringBuilder().append(ch, start, length).toString()
                when (nodeName) {
                    "groupId" -> meta.groupId = value
                    "artifactId" -> meta.artifactId = value
                    "release" -> meta.release = value
                    "version" -> meta.versions.add(value)
                }

            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                nodeName = null
            }
        })
        return meta
    }

    fun strToPair(s: String): Pair<String?, String?> {
        val i = s.indexOf(",")
        if (i < 0) return Pair(s, null)
        return Pair(s.substring(0, i), s.substring(i + 1))
    }

    /**
     *
     */
    fun readUrlTxt(url: String) = kotlin.runCatching {
        if (url.startsWith("http")) URL(url).readText() else FileUtils.readText(File(url))
    }.getOrNull() ?: ""

    fun urlToFile(url: String, file: File) {
        kotlin.runCatching {
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            val ins = if (url.startsWith("http")) URL(url).openStream() else File(url).inputStream()
            FileUtils.copy(ins, file.outputStream())
        }
    }


    fun topSort(sources: Collection<TopNode>): List<String> {
        sources.forEach { it.degree = 0 }
        for (node in sources) node.nodes.forEach { it.degree++ }

        //查出入度为0的顶点
        val queue = LinkedList(sources.filter { it.degree == 0 })

        val tops = LinkedList<String>()
        while (queue.isNotEmpty()) {
            val t = queue.poll()
            tops.add(t.name)
            for (node in t.nodes) {
                while (--node.degree == 0) {
                    queue.offer(node)
                }
            }
        }

        return tops
    }

    /**
     * 写入清单文件到本地
     */
    fun writeManifest(manifest: ManifestX) {
        FileUtils.writeText(ideImportFile(manifest.dir), JSON.toJSONString(manifest))
    }

    /**
     * 从本地读取清单文件
     */
    fun readManifest(dir: String): ManifestX? = kotlin.runCatching {
        JSON.parseObject(FileUtils.readText(ideImportFile(dir)), ManifestX::class.java)
    }.getOrElse {
        it.printStackTrace()
        null
    }

    fun saveIdeaConfig(basePath: String, local: Boolean, include: String) {
        val file = File(basePath, ".idea/local.gradle")
        val txt = "aex{ config{ include='$include' ; local = $local }}"
        FileUtils.writeText(file, txt)
    }

    fun ideImportFile(basePath: String) = File(basePath, ".idea/import.json")

    fun reloadRepoMetaFile(forceLoad: Boolean, baseDir: String, name: String, maven: IMaven): File {
        val repoMetaFile = File(baseDir, TextUtils.append(arrayOf("build/meta/", (maven.url + maven.group + name).hash(), XKeys.XML_META)))
        //缓存文件
        val cacheFile = File(baseDir, TextUtils.append(arrayOf("build/meta/cache/", (maven.url + maven.group + name).hash(), XKeys.XML_META)))
        if (forceLoad || !repoMetaFile.exists()) {
            val metaUrl = TextUtils.append(arrayOf(maven.url, maven.group.replace(".", "/"), name, XKeys.XML_META))
            urlToFile(metaUrl, repoMetaFile)
            FileUtils.copy(repoMetaFile, cacheFile)
        }
        return repoMetaFile
    }

    /**
     * 检查尝试是否更新了版本数据
     */
    fun checkRepoUpdate(forceLoad: Boolean, baseDir: String, name: String, maven: IMaven): Boolean {
        //正在使用的文件
        val repoMetaFile = File(baseDir, TextUtils.append(arrayOf("build/meta/", (maven.url + maven.group + name).hash(), XKeys.XML_META)))
        //缓存文件
        val cacheFile = File(baseDir, TextUtils.append(arrayOf("build/meta/cache/", (maven.url + maven.group + name).hash(), XKeys.XML_META)))
        //如果比repo meta文件更旧,或者超过10分钟没更新,重新从网络更新
        if (cacheFile.length() <= repoMetaFile.length() && (forceLoad || System.currentTimeMillis() - cacheFile.lastModified() > 10 * 60 * 1000)) {
            urlToFile(TextUtils.append(arrayOf(maven.url, maven.group.replace(".", "/"), name, XKeys.XML_META)), cacheFile)
        }
        return cacheFile.length() > repoMetaFile.length()
    }
}

class TopNode(var name: String) {
    var degree: Int = 0
    val nodes: HashSet<TopNode> = hashSetOf()
}
