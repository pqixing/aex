package com.pqixing.aex.model.define

import com.pqixing.aex.model.ProjectEx
import com.pqixing.model.define.IConfig
import com.pqixing.model.define.IGit
import com.pqixing.model.define.IMaven
import com.pqixing.model.impl.TypeX
import groovy.lang.Closure

interface IManifestEx {
    fun config(): IConfig
    fun config(closure: Closure<*>): IConfig

    /**
     * 设置maven模型,如果name不存在,则新建模型,否则修改原值
     */
    fun maven(name: String): IMaven
    fun maven(name: String, closure: Closure<*>): IMaven
    fun from(name: String, from: IMaven, closure: Closure<*>): IMaven
    fun mavens(closure: Closure<*>)

    /**
     * 设置git模型,如果name不存在,则新建模型,否则修改原值
     */
    fun git(name: String): IGit
    fun git(name: String, closure: Closure<*>? = null): IGit
    fun from(name: String, from: IGit, closure: Closure<*>): IGit
    fun gits(closure: Closure<*>)

    /**
     * 设置当前跟模块的名称, 此模块的git和maven设置会作为其余模块的默认缺省值
     */
    fun root(name: String, maven: String, git: String, closure: Closure<*>): ProjectEx

    /**
     * 设置当前跟模块的名称, 此模块的git和maven设置会作为其余模块的默认缺省值
     */
    fun root(name: String, maven: IMaven, git: IGit, closure: Closure<*>): ProjectEx
    fun project(name: String): ProjectEx
    fun project(name: String, closure: Closure<*>?): ProjectEx
    fun project(name: String, desc: String, closure: Closure<*>?): ProjectEx
    fun project(name: String, desc: String, url: String, closure: Closure<*>?): ProjectEx
    fun project(name: String, maven: IMaven, git: IGit, closure: Closure<*>?): ProjectEx
    fun project(name: String, desc: String, url: String, maven: IMaven, git: IGit, closure: Closure<*>?): ProjectEx
    fun projects(closure: Closure<*>)

    fun fallbacks():MutableList<String>
    fun fallbacks(values: List<String>)
    fun type(name: String): TypeX
    fun type(name: String, closure: Closure<*>): TypeX
    fun types(closure: Closure<*>)
}