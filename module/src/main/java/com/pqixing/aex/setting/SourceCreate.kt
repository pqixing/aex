package com.pqixing.aex.setting

import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.ModuleX
import com.pqixing.tools.FileUtils
import com.pqixing.tools.TextUtils
import java.io.File
import java.util.*

class SourceCreate(val XSetting: XSetting, val manifest: ManifestX) {

    /**
     * 检查代码生成
     */
    fun tryCreateSrc(module: ModuleX) {
        //非Android工程不生成代码
        if (module.typeX().java()) createJavaSrc(module)
        if (module.typeX().android()) createAndroidSrc(module)
    }

    private fun createJavaSrc(module: ModuleX) {
        //如果build文件存在，不重新生成代码
        val projectDir = module.absDir()
        //代码目录
        val sourceDir = File(projectDir, "src/main")
        val buildFile = File(projectDir, "build.gradle")
        if (buildFile.exists()) return

        val name = TextUtils.className(module.name.split("_").joinToString { TextUtils.firstUp(it) })

        val className = "${name}App"
        val groupName = module.project.maven.group
        val packageName = groupName.replace(".", "/") + "/" + name.toLowerCase(Locale.CHINA)
        FileUtils.writeText(
            File(sourceDir, "java/$packageName/${className}.java").takeIf { !it.exists() },
            "package ${packageName.replace("/", ".")};\nfinal class ${className}{}"
        )

        //如果是application类型，写入build.gradle，并设置applicationId
        FileUtils.writeText(File(projectDir, "build.gradle").takeIf { !it.exists() }, "//java build file")

        XSetting.println("   Create src :${module.name} Java ${sourceDir.absolutePath}")
    }

    private fun createAndroidSrc(module: ModuleX) {
        //如果build文件存在，不重新生成代码
        val projectDir = module.absDir()
        //代码目录
        val sourceDir = File(projectDir, "src/main")
        val manifestFile = File(sourceDir, "AndroidManifest.xml")
        if (manifestFile.exists()) return

        val name = TextUtils.className(module.name.split("_").joinToString { TextUtils.firstUp(it) })
        val groupName = module.project.maven.group
        val emptyManifest =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<manifest package=\"${groupName.toLowerCase()}.${name.toLowerCase()}\" />"
        //写入空清单文件
        FileUtils.writeText(manifestFile, emptyManifest)


        val className = "${name}App"
        val packageName = groupName.replace(".", "/") + "/" + name.toLowerCase(Locale.CHINA)
        FileUtils.writeText(
            File(sourceDir, "resources/values/strings.xml").takeIf { !it.exists() },
            "<resources>\n<string name=\"library_name\">${module.name}</string> \n</resources>"
        )
        FileUtils.writeText(
            File(sourceDir, "java/$packageName/${className}.java").takeIf { !it.exists() },
            "package ${packageName.replace("/", ".")};\nfinal class ${className}{}"
        )

        //如果是application类型，写入build.gradle，并设置applicationId
        if (module.typeX().app()) {
            FileUtils.writeText(
                File(projectDir, "build.gradle").takeIf { !it.exists() },
                "android {\n    defaultConfig {\n        applicationId '${groupName}.${module.name}'\n    }\n}"
            )
        }

        XSetting.println("   Create src :${module.name} ${sourceDir.absolutePath}")
    }

}