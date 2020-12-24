package com.pqixing.model.impl

import com.pqixing.model.define.IProject
import com.pqixing.tools.TextUtils
import java.io.File

open class ProjectX(name: String) : IProject(name) {
    lateinit var manifest: ManifestX

    fun getGitUrl(): String = if (url.endsWith(".git")) url else TextUtils.append(arrayOf(git.url, "${url}.git"))

    //root工程忽略git group
    fun absDir(): File = if (this == manifest.root.project) File(manifest.dir, path) else File(File(manifest.dir, git.group), path)
}