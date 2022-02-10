package com.pqixing.profm.model

import com.pqixing.profm.android.DependManager
import com.pqixing.profm.model.define.IModuleEx
import com.pqixing.profm.utils.AEX
import com.pqixing.model.define.ICompile
import com.pqixing.model.impl.ModuleX
import com.pqixing.model.impl.ProjectX
import groovy.lang.Closure
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

open class ModuleEx(project: ProjectX, name: String) : ModuleX(name), IModuleEx {
    init {
        super.project = project
        super.path = name
    }

    var localEx: LocalEx? = null

    fun localEx(): LocalEx = localEx!!




    fun isAndroid() = typeX().android()
    fun reqDps() = typeX().android() || typeX().java()

    override fun compiles() = compiles
    override fun compiles(closure: Closure<*>) = compiles.forEach { AEX.runClosure(it, closure) }

    override fun compile(scope: String, name: String): ICompile = compile(scope, name, "")
    override fun compile(scope: String, name: String, version: String): ICompile = compile(scope, name, version, null)
    override fun compile(scope: String, name: String, version: String, closure: Closure<*>?): ICompile {
        val c = compiles.find { it.name == name } ?: ICompile(name)
        c.scope = scope
        c.name = name
        c.version = version
        if (!compiles.contains(c)) compiles.add(c)
        return AEX.runClosure(c, closure)
    }


    override fun implementation(name: String): ICompile = implementation(name, "")
    override fun implementation(name: String, version: String): ICompile = implementation(name, version, null)
    override fun implementation(name: String, version: String, closure: Closure<*>?): ICompile = compile("implementation", name, version, closure)


    override fun compileOnly(name: String): ICompile = compileOnly(name, "")
    override fun compileOnly(name: String, version: String): ICompile = compileOnly(name, version, null)
    override fun compileOnly(name: String, version: String, closure: Closure<*>?): ICompile = compile("compileOnly", name, version, closure)


    override fun runtimeOnly(name: String): ICompile = runtimeOnly(name, "")
    override fun runtimeOnly(name: String, version: String): ICompile = runtimeOnly(name, version, null)
    override fun runtimeOnly(name: String, version: String, closure: Closure<*>?): ICompile = compile("runtimeOnly", name, version, closure)


    override fun api(name: String): ICompile = api(name, "")
    override fun api(name: String, version: String): ICompile = api(name, version, null)
    override fun api(name: String, version: String, closure: Closure<*>?): ICompile = compile("api", name, version, closure)


    override fun proxy(name: String): ICompile = proxy(name, "")
    override fun proxy(name: String, version: String): ICompile = proxy(name, version, null)
    override fun proxy(name: String, version: String, closure: Closure<*>?): ICompile = compile("api", name, version, closure).also {
        proxy = it
    }

}

class LocalEx(var git: Git, var branch: String, var forMmaven: MavenEx, var depend: DependManager, var last: RevCommit? = null)