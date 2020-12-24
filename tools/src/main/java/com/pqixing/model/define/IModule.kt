package com.pqixing.model.define

abstract class IModule (val name:String) {
    var type: String = "LIBRARY"
    var path = ""
    var file = ""

    var desc = ""

    var version = ""

    /**
     * api模块
     */
    var proxy: ICompile? = null

    /**
     * 当依赖了带有proxy设置的模块时,强行把host模块也依赖上
     */
    var forceHost = false
    val compiles = mutableListOf<ICompile>()
}