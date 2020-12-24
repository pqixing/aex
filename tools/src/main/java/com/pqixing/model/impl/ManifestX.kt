package com.pqixing.model.impl

import com.pqixing.TopNode
import com.pqixing.XHelper
import com.pqixing.model.define.IManifest

/**
 * 配置的清单文件
 */
open class ManifestX(val dir: String) : IManifest() {

    //根目录对应模块
    lateinit var root: ModuleX

    private var _modules: List<ModuleX>? = null
    fun sorted(): List<ModuleX> {
        if (_modules != null) return _modules!!
        val modules = projects.map { it.modules }.flatten().map { it.name to it }.toMap()
        val topNodes = modules.map { it.key to TopNode(it.value.name) }.toMap()
        for (topNode in topNodes.values) {
            val module = modules[topNode.name] ?: continue
            topNode.nodes += module.compiles.mapNotNull { modules[it.name] }.map {
                var m = it
                //如果模块设置了代理,默认将依赖重定向到代理模块
                while (m.proxy != null) {
                    m = modules[m.proxy!!.name] ?: error("")
                }
                topNodes[m.name] ?: error("")
            }
        }
        _modules = XHelper.topSort(topNodes.values).mapNotNull { modules[it] }
        return _modules!!
    }

    fun findModule(name: String): ModuleX? {
        for (project in projects) {
            return project.modules.find { it?.name == name } ?: continue
        }
        return null
    }

    fun importModules(): List<ModuleX> {

        val imports = config.include.split(",").mapNotNull { it.trim().takeIf { i -> i.isNotEmpty() } }

        val results = imports.filter { !it.contains("#") }.toMutableSet()

        results += imports.filter { it.startsWith("D#") }
                .mapNotNull { findModule(it.substring(2))?.dps() }.flatten()
        results -= imports.filter { it.startsWith("E#") }.map { it.substring(2) }
        results -= imports.filter { it.startsWith("ED#") }
                .mapNotNull { findModule(it.substring(3))?.dps() }.flatten()

        return results.mapNotNull { findModule(it) }
    }
}

