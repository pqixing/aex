package com.pqixing.profm.model

import com.pqixing.profm.model.define.IProjectEx
import com.pqixing.profm.utils.AEX
import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.ModuleX
import com.pqixing.model.impl.ProjectX
import groovy.lang.Closure
import org.eclipse.jgit.api.Git

open class ProjectEx(manifest: ManifestX, name: String) : ProjectX(name), IProjectEx {

    init {
        super.manifest = manifest
        super.path = name
        super.url = name
    }

    var repo: Git? = null

    override fun module(name: String): ModuleX = module(name, null)
    override fun module(name: String, desc: String): ModuleX = module(name, desc, null)
    override fun module(name: String, desc: String, type: String): ModuleX = module(name, desc, type, null)

    override fun module(name: String, closure: Closure<*>?): ModuleX = AEX.runClosure(module(name), closure)
    override fun module(name: String, desc: String, closure: Closure<*>?): ModuleX = module(name, desc, "LIBRARY", closure)
    override fun module(name: String, desc: String, type: String, closure: Closure<*>?): ModuleX {
        val module = modules.find { it.name == name } ?: ModuleEx(this, name).also { modules.add(it) }
        module.desc = desc
        module.type = type
        if (!modules.contains(module)) {
            modules.add(module)
        }
        return AEX.runClosure(module, closure)
    }

    override fun asModule(type: String): ModuleX = module(name, "", type).also { m ->
        m.path = ""
    }

    override fun asModule(type: String, closure: Closure<*>): ModuleX = module(name, desc, type, closure).also { m ->
        m.path = ""
    }

    override fun modules(closure: Closure<*>) = modules.forEach { AEX.runClosure(it, closure) }
}