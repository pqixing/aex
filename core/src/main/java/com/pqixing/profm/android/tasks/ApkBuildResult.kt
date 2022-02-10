package com.pqixing.profm.android.tasks

import com.pqixing.profm.setting.XSetting
import com.pqixing.tools.FileUtils
import org.gradle.api.Project
import java.io.File

/**
 * AndoridApk文件编辑跟踪
 */
open class ApkBuildResult(val set: XSetting, val pro: Project) {

    private fun findAllApk(dir: File, set: MutableSet<File>) {
        if (!dir.exists() || !dir.isDirectory) return

        for (file in dir.listFiles() ?: return) {
            if (file.isDirectory) findAllApk(file, set)
            if (file.isFile && file.name.endsWith(".apk")) {
                set.add(file)
            }
        }
    }

    private fun tryCopy(lastCreateFile: File) {
        //待拷贝的目录
        val copyFile = File(set.manifest.config.opts.takeIf { it.isNotEmpty() } ?: return)
        set.println("Start copy result to : $copyFile")
        if (copyFile.name.endsWith(".apk")) {
            FileUtils.copy(lastCreateFile, copyFile)
            return
        }
        if (copyFile.exists() && !copyFile.isDirectory) return
        //拷贝整个目录
        FileUtils.copy(File(pro.buildDir, "outputs"), copyFile)
        FileUtils.copy(lastCreateFile, File(copyFile, "${pro.name}.apk"))
    }

    fun onAssemble(task: String) {
        val apks = mutableSetOf<File>()
        findAllApk(File(pro.buildDir, "outputs/apk"), apks)
        if (apks.isEmpty()) return

        val lastCreateFile = apks.maxByOrNull { it.lastModified() } ?: return
        //30秒之前的文件,过时
        if (lastCreateFile.lastModified() - System.currentTimeMillis() >= 1000 * 30) {
            return
        }
        tryCopy(lastCreateFile)
        set.println("Result [ ${pro.name}:$task ] : ${lastCreateFile.absolutePath}")
        set.writeResult(lastCreateFile.absolutePath)
    }
}