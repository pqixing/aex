package com.pqixing.model.define

import com.pqixing.model.impl.ProjectX
import com.pqixing.model.impl.TypeX

/**
 * 配置的清单文件
 */
open class IManifest {
    /**
     * 本地配置
     */
    var config = IConfig()

    /**
     * 模块类型定义集合
     */
    val mTypes = TypeX.items().map { TypeX(it) }.toMutableList()

    //所有的maven配置设置
    val mavens = mutableListOf<IMaven>()

    //所有的git配置
    val gits = mutableListOf<IGit>()

    //所有的项目工程
    val projects = mutableListOf<ProjectX>()

    /**
     * 设置本地分支名称,如果设置了, 则忽略rootProject本身的分支,使用该值作为maven隔离使用
     */
    var branch: String = ""

    /**
     * 使用分支进行仓库隔离
     */
    var usebr = true

    /**
     * 分支依赖的管理
     */
    val fallbacks = mutableListOf("master")
}

