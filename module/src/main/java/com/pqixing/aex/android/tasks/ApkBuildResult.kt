package com.pqixing.aex.android.tasks

import com.pqixing.aex.setting.XSetting
import com.pqixing.tools.FileUtils
import org.gradle.api.Project
import java.io.File

/**
 * AndoridApk文件编辑跟踪
 */
open class ApkBuildResult(val set: XSetting, val pro: Project) {


    fun findAllApk(dir: File, set: MutableSet<File>) {
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()?.forEach {
            if (it.isDirectory) findAllApk(it, set)
            if (it.isFile && it.name.endsWith(".apk")) {
                set.add(it)
            }
        }

    }

    fun onAssemble(task: String) {

        val apks = mutableSetOf<File>()
        findAllApk(File(pro.buildDir, "outputs/apk"), apks)
        if (apks.isEmpty()) return

        val lastCreateFile = apks.maxBy { it.lastModified() } ?: return
        //30秒之前的文件,过时
        if (lastCreateFile.lastModified() - System.currentTimeMillis() >= 1000 * 30) {
            return
        }
        set.println("BuildApk ${pro.name} -> $task -> ${lastCreateFile.absolutePath}")

        val buildApkPath = set.manifest.config.opts.takeIf { it.isNotEmpty() }

        val outPath = buildApkPath ?: return set.writeResult(lastCreateFile.absolutePath)

        val copyFile = File(outPath)
        if (copyFile.name.endsWith(".apk")) {
            FileUtils.copy(lastCreateFile, copyFile)
            set.writeResult(outPath)
        } else if (!copyFile.exists()) {
            copyFile.mkdirs()
        }

        if (copyFile.isDirectory) {
            FileUtils.copy(lastCreateFile, File(copyFile, lastCreateFile.name))
            set.writeResult("${outPath}/${lastCreateFile.name}")
        }
    }
}