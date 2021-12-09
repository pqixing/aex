package com.pqixing.model.define

open class IConfig {

    /**
     * 导入工程,多个工程之间,用 , 号分隔
     * 前缀含义 E#  exclude 当前工程
     * D#   dpsInclude  导入该工程的所有依赖工程,如需生效,需要对该工程执行一次DPSAnalis
     * ED#  dpsExclude
     * eg: include="demo1,D#demo2,ED#demo3"
     * 如果 include字段为空,或等于Auto,则,获取当前需要执行的任务,来自动确定需要导入的工程
     */
    var include: String = ""

    /**
     * 每次从仓库中同步版本号
     */
    var sync = false

    /**
     * 是否拦截依赖缺少的异常 , 默认true
     * true: 当有依赖模块缺少时 ， 抛出异常 ， 方便查找问题
     * false: 不拦截错误 ， 方便代码导入AS ， 但是缺少依赖构建过程出现的类缺失异常问题很难定位
     */
    var lose = false

    /**
     * 本地依赖连接
     */
    var link = false

    /**
     * 是否使用日志
     * true:打印日志
     */
    var log = false

    /**
     * 模块依赖使用
     * true:允许使用本地代码进行依赖调试
     * false:不使用本地代码调试
     */
    var local = true

    /**
     * 是否支持创建源码
     */
    var create = true

    /**
     * 指定一个版本号的map文件 ， 则会优先使用该文件中的信息进行版本号管理
     */
    var mapping = ""

    /**
     * 工程编译目录，使用不同的编译目录便于同时执行多个编译事件，例如ToMaven和构建
     */
    var build = "default"

    /**
     * 指定library运行
     */
    var assemble = ""

    /**
     * 其他的参数信息
     */
    var opts = ""

    /**
     * toMaven时,忽略检查项目
     * type 1,  不校验是否和上次代码是否相同,允许提交重复
     * type 2,  不检验本地是否存在未提交修改
     * type 3,  不检验分支和root工程是否一致
     * etc "1,2,3"
     */
    var ignore = ""
}