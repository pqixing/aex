package com.pqixing.tools

import com.pqixing.XKeys
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.*

object FileUtils {
    var clazz: Class<*> = XKeys::class.java
    @JvmStatic
    fun fromRes(name: String): String {
        val reader = clazz.getResourceAsStream(name).reader()
        val text = reader.readText()
        reader.close()
        return text
    }

    /**
     * @param checkChange 检查是否有变化，如果没有变化，则不写入
     */
    @JvmStatic
    fun writeText(file: File?, text: String, checkChange: Boolean = false): String {
        file ?: return ""
        if (checkChange && readText(file) == text) return file.path
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        with(file.writer()) {
            write(text)
            flush()
            close()
        }
        return file.path
    }

    @JvmStatic
    fun readText(f: File): String? {
        if (!f.exists()) return null
        return f.readText()
    }

    @JvmStatic
    fun delete(f: File): Boolean {
        if (!f.exists()) return false
        if (f.isDirectory) f.listFiles().forEach { delete(it) }
        if (f.exists()) f.delete()
        return true
    }

    /**
     *拷贝文件
     */
    @JvmStatic
    fun copy(from: File, to: File): Boolean {
        if (!from.exists()) return false
        delete(to)
        if (from.isDirectory) from.listFiles()?.forEach { copy(it, File(to, it.name)) }
        else {
            to.parentFile.mkdirs()
            copy(from.inputStream(),to.outputStream())

        }
        return true
    }

    fun copy(ins:InputStream,out:OutputStream)=kotlin.runCatching {
        ins.copyTo(out)
        ins.close()
        out.flush()
        out.close()
    }.isSuccess

    @JvmStatic
    fun moveDir(from: File, to: File): Boolean {
        if (!from.exists()) return false
        if (from.isDirectory) from.listFiles()?.forEach { moveDir(it, File(to, it.name)) }
        else {
            to.parentFile.mkdirs()
            from.renameTo(to)
        }
        return true
    }

    fun replaceOrInsert(start: String, end: String, content: String, source: String): String {
        val regex = Regex("$start.*?$end", RegexOption.DOT_MATCHES_ALL)
        val targetTx = "$start\n$content\n$end"
        return if (source.contains(regex)) {
            source.replace(regex, targetTx)
        } else "$source$targetTx"
    }

    fun closeSafe(stream: Closeable) = try {
        stream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    fun mergeFile(target: File, mergeFiles: List<File>, replace: (txt: String) -> String = { it }) {
        val txt = StringBuilder()
        mergeFiles.forEach { readText(it)?.let { t -> txt.append("\n$t") } }
        writeText(target, replace(txt.toString()))
//        Tools.println("Merge Gradle::[${mergeFiles.joinToString(",") { it.name }}] >> ${target.absolutePath}")
    }

    fun readProperties(file: File): Properties {
        val properties = Properties()
        if (file.exists()) {
            val stream = file.inputStream()
            properties.load(stream)
            FileUtils.closeSafe(stream)
        }
        return properties
    }

    fun writeProperties(file: File, properties: Properties) {
        if (!file.exists()) file.parentFile.mkdirs()
        val out = file.outputStream()
        properties.store(out, "UTF-8")
        closeSafe(out)
    }

    fun writeProperties(file: File, params: Map<String, Any>) {
        val properties = Properties()
        params.forEach { (t, u) -> properties[t] = u.toString() }
        writeProperties(file, properties)
    }
}