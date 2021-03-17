package com.pqixing.model.impl

import com.pqixing.aexEncode
import com.pqixing.model.define.IModule
import java.io.File
import java.util.*

open class ModuleX(name: String) : IModule(name) {
    lateinit var project: ProjectX

    private var _dps: Set<String>? = null
    fun typeX(): TypeX {
        return project.manifest.mTypes.find { it.name == type }!!
    }

    fun absDir(): File = File(project.absDir(), path)

    fun sorted(): List<ModuleX> {
        val dps = dps()
        return project.manifest.sorted().filter { dps.contains(it.name) }
    }

    fun manifest() = project.manifest
    fun maven() = project.maven
    fun git() = project.git
    fun usebr() = manifest().usebr
    fun group(branch: String = manifest().branch): String = if (usebr()) "${maven().group}.${branch.aexEncode()}" else maven().group

    fun dps(): Set<String> {
        if (_dps != null) return _dps!!
        val dps = hashSetOf<String>()
        val manifest = project.manifest
        val waitHandles = LinkedList<String>()
        waitHandles.add(name)
        while (waitHandles.isNotEmpty()) {
            val first = waitHandles.poll()
            if (!dps.add(first)) continue
            waitHandles += (manifest.findModule(first)?.compiles?.map { it.name }) ?: continue
        }
        _dps = dps
        return dps
    }
}