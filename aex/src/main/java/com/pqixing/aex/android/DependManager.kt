package com.pqixing.aex.android

import com.pqixing.aex.model.ModuleEx
import com.pqixing.aex.setting.XSetting
import com.pqixing.model.define.ICompile
import com.pqixing.tools.TextUtils

//组件工程
class DependManager(var set: XSetting, val module: ModuleEx) {
    val manifest = set.manifest
    val config = manifest.config
    val loses = mutableListOf<String>()
    val locals = mutableSetOf<String>()
    val localPros: Set<String> by lazy { set.allProjects() }
    val excludes = hashSetOf<String>()
    val includes = mutableListOf<String>()

    /**
     * 当前依赖的分支信息
     */
    val fallbacks = manifest.fallbacks.let {
        val results = mutableListOf<String>()
        val br = manifest.branch
        it.forEach { f ->
            results.add(f)
            if (f == br) results.clear()
        }
        results.add(0, br)
        results
    }

    fun include(txt: String) = includes.add(txt)

    //处理依赖
    fun resolveDps(): String {
        val compiles = module.compiles.toMutableSet()
        //如果是本地调试模式,并且允许添加依赖连接
        if (config.local && config.link) {
            val dps = module.dps().toMutableSet()
            dps.remove(module.name)
            dps.removeAll(compiles.map { it.name })
            //有间接依赖,并且本地导入了,添加直接依赖
            compiles += dps.filter { localPros.contains(it) }.map { ICompile(it) }
        }

        //处理proxy的模块
        val proxy = compiles.filter { manifest.findModule(it.name)?.proxy != null }
        compiles.removeAll(proxy)

        val forceHost = module.forceHost || module.name == set.manifest.config.assemble || module.typeX().app()

        for (it in proxy) {
            var c = it
            var p: ICompile? = manifest.findModule(c.name)?.proxy
            while (p != null) {
                if (forceHost) {//如果强行需要依赖宿主,则用runtimeOnly的方式导入宿主
                    c.scope = "runtimeOnly"
                    compiles.add(c)
                }
                c = p
                p = manifest.findModule(c.name)?.proxy
            }
            compiles.add(c)
        }


        loses += compiles.filter { !((config.local && onLocalCompile(it)) || onMavenCompile(it)) }.map { it.name }
        /**
         * 缺失了部分依赖
         */
        if (loses.isNotEmpty()) {
            set.writeResult("ResolveDps -> lose dps -> $loses", !config.lose)
        }

        val sb = java.lang.StringBuilder("dependencies { \n")
        includes.forEach { sb.append(it).append("\n") }
        sb.append("}\n")
            .append("configurations { \n")
            .append(excludeStr("all*.exclude ", excludes.toSet()))
            .append("}\n")
        return sb.toString()
    }

    /**
     * 重新凭借scope
     */
    private fun getScope(prefix: String, scope: String): String {
        if (prefix.isEmpty()) return "    $scope"
        return "    $prefix${TextUtils.firstUp(scope)}"
    }

    /**
     * 添加一个仓库依赖
     */
    private fun onMavenCompile(dpc: ICompile): Boolean {
        val module = manifest.findModule(dpc.name) as? ModuleEx ?: return false
        val version = dpc.version.takeIf { it.isNotEmpty() } ?: module.version
        val groupIds = findGroupIds(module)

        var compile = false
        for (groupId in groupIds) {
            if (compile) {
                addBranchExclude(groupId, dpc.name)
                continue
            }
            val v = set.vm.findCompileVersion(groupId, dpc.name, version) ?: continue
            val c = " ;force = true ;".takeIf { version == v } ?: ""
            includes += "${getScope("", dpc.scope)} ('${groupId}:${dpc.name}:${v}') " +
                    "{ ${excludeStr(excludes = dpc.excludes)} $c }"
            excludes.addAll(set.vm.getPom(module.project.maven.url, groupId, dpc.name, v,module.project.maven.asCredentials()).allExclude)
            compile = true
        }
        return compile
    }

    /**
     * 返回需要查找的依赖分支groupId
     */
    fun findGroupIds(module: ModuleEx): List<String> = if (!manifest.usebr) listOf(module.project.maven.group) else fallbacks.map { module.group(it) }

//
//    /**
//     * 自动匹配版本号
//     */
//    private fun resolveVersion(dpc: Compile) {
//        if (dpc.version.isNotEmpty()) return
//
//        //没有依附模块,则读取当前配置的版本
//        dpc.version = dpc.attach?.also { resolveVersion(it) }?.let { attach ->
//            kotlin.runCatching {
//                getPom(project, attach.branch, attach.name,
//                        args.versions.getVersion(attach.branch, attach.name, attach.version).second).dependency.find { it.contains(":${dpc.name}:") }
//            }.getOrNull()?.let { it.replace("\"", "").substringAfterLast(":") }
//        } ?: dpc.version.let { "*$it" } ?: "+"
//    }

    /**
     * 检查是否存在其他分支的版本，如果存在，添加到exclude中
     */
    private fun addBranchExclude(groupId: String, name: String) {
        if (set.vm.checkBranchVersion(groupId, name)) {
            excludes.add("${groupId}:${name}")
        }
    }

    /**
     * 生成exlude字符串
     */
    private fun excludeStr(prefix: String = "exclude", excludes: Set<String>): String {
        val sb = StringBuilder()
        excludes.forEach {
            sb.append("    $prefix (")
            val group = it.substringBefore(":")
            val module = it.substringAfter(":", "")
            if (group.isNotEmpty()) sb.append("group : '${group}',")
            if (module.isNotEmpty()) sb.append("module : '${module}',")
            sb.deleteCharAt(sb.length - 1)
            sb.append(") \n")
        }
        return sb.toString()
    }

    /**
     * 本地进行工程依赖
     */
    private fun onLocalCompile(dpc: ICompile): Boolean {
        if (!localPros.contains(dpc.name)) {

            return false
        }

        val module = manifest.findModule(dpc.name) as? ModuleEx ?: return false
        if (module.localEx().branch != manifest.branch) {
            set.println(" branch diff ${dpc.name} -> ${module.localEx().branch}")
        }
        includes +=
            "${getScope("", dpc.scope)} ( project(':${dpc.name}'))  { ${excludeStr(excludes = dpc.excludes)} }"
        locals.add(dpc.name)
        findGroupIds(module).forEach { addBranchExclude(it, module.name) }
        return true
    }
}