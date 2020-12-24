package com.pqixing.aex.model

import com.pqixing.aex.model.define.IManifestEx
import com.pqixing.aex.setting.XSetting
import com.pqixing.aex.utils.AEX.runClosure
import com.pqixing.envValue
import com.pqixing.model.define.IGit
import com.pqixing.model.define.IMaven
import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.TypeX
import groovy.lang.Closure
import java.lang.reflect.Field

/**
 * 清单文件拓展,用于setting.gradle 中文件配置, 具体属性设置 @see com.pqixing.model.XManifest
 */
open class ManifestEx(dir: String) : ManifestX(dir), IManifestEx {

    /**
     * 设置本地config模型, config中的属性,均可通过 gradle -Dname=value方式动态赋值
     */
    override fun config() = config
    override fun config(closure: Closure<*>) = runClosure(config, closure)

    /**
     * 设置maven模型,如果name不存在,则新建模型,否则修改原值
     */
    override fun maven(name: String): IMaven = mavens.find { it.name == name } ?: IMaven(name).also { mavens.add(it) }
    override fun maven(name: String, closure: Closure<*>): IMaven = runClosure(maven(name), closure)
    override fun from(name: String, from: IMaven, closure: Closure<*>): IMaven {
        val maven = maven(name)
        maven.url = from.url
        maven.user = from.user
        maven.psw = from.psw
        maven.group = from.group
        maven.version = from.version

        return runClosure(maven, closure)
    }

    override fun mavens(closure: Closure<*>) = mavens.forEach { runClosure(it, closure) }

    /**
     * 设置git模型,如果name不存在,则新建模型,否则修改原值
     */
    override fun git(name: String): IGit = gits.find { it.name == name } ?: IGit(name).also { gits.add(it) }
    override fun git(name: String, closure: Closure<*>?): IGit = runClosure(git(name), closure)
    override fun from(name: String, from: IGit, closure: Closure<*>): IGit {
        val git = git(name)
        git.url = from.url
        git.user = from.user
        git.psw = from.psw
        git.group = from.group
        return runClosure(git, closure)
    }

    override fun gits(closure: Closure<*>) = gits.forEach { runClosure(it, closure) }


    /**
     * 设置当前跟模块的名称, 此模块的git和maven设置会作为其余模块的默认缺省值
     */
    override fun root(name: String, maven: String, git: String, closure: Closure<*>): ProjectEx = root(name, maven(maven), git(git), closure)

    /**
     * 设置当前跟模块的名称, 此模块的git和maven设置会作为其余模块的默认缺省值
     */
    override fun root(name: String, maven: IMaven, git: IGit, closure: Closure<*>): ProjectEx {
        val p = project(name, "", name, maven, git, closure)
        p.path = ""
        root = p.asModule("ROOT")
        return p
    }

    override fun project(name: String): ProjectEx = project(name, null)

    override fun project(name: String, closure: Closure<*>?): ProjectEx = project(name, "", closure)
    override fun project(name: String, desc: String, closure: Closure<*>?): ProjectEx = project(name, desc, name, closure)

    override fun project(name: String, desc: String, url: String, closure: Closure<*>?): ProjectEx = project(name, desc, url, root.project.maven, root.project.git, closure)

    override fun project(name: String, maven: IMaven, git: IGit, closure: Closure<*>?): ProjectEx = project(name, "", name, maven, git, closure)


    override fun project(name: String, desc: String, url: String, maven: IMaven, git: IGit, closure: Closure<*>?): ProjectEx {
        val p = projects.find { it.name == name } as? ProjectEx ?: ProjectEx(this, name)
        p.maven = maven
        p.git = git
        p.desc = desc
        p.url = url
        if (!projects.contains(p)) {
            projects.add(p)
        }
        return runClosure(p, closure)
    }

    override fun projects(closure: Closure<*>) = projects.mapNotNull { it as? ProjectEx }.forEach { runClosure(it, closure) }
    override fun fallbacks(): MutableList<String> = fallbacks

    override fun fallbacks(values: List<String>) {
        fallbacks.clear()
        fallbacks += values.toMutableList()
    }

    override fun type(name: String): TypeX {
        return mTypes.find { it.name == name }!!
    }

    override fun type(name: String, closure: Closure<*>): TypeX {
        return runClosure(type(name), closure)
    }

    override fun types(closure: Closure<*>) = mTypes.forEach { runClosure(it, closure) }

    /**
     * 从系统配置中加载对应的变量
     */
    fun afterEvaluated(set: XSetting) {
        for (field in config.javaClass.declaredFields) readEnvValue(field, config)
        projects.map { it.modules }.flatten().filter { it.version.isEmpty() }
                .forEach { it.version = it.project.maven.version }
    }

    private fun readEnvValue(field: Field, target: Any) {
        val value = field.name.envValue() ?: return

        field.isAccessible = true
        when (field.type) {
            Boolean::class.java -> field.setBoolean(target, value.toBoolean())
            String::class.java -> field.set(target, value)
        }
    }
}

