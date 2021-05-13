[AEX](https://github.com/pqixing/aex) - [GRADLE](https://plugins.jetbrains.com/plugin/16145-aex) : 配置文档
---------
> GRADLE插件详细配置说明文档<br>
> 更多文档: UI操作文档: [AEX_GUI](https://github.com/pqixing/aex/blob/main/ide/README.md) ;  <span id="framework">架构文档</span>: [AEX](https://github.com/pqixing/aex)


## 集成aex插件

-----

在主项目的settings.gradle文件中,添加 com.pqixing.aex:module插件
* 标准集成, 插件存放在 https://dl.bintray.com/pqixing86/aex ,同时也推到jcenter公共仓库中

```settings.gradle
//在 settings.gradle 设置插件环境
buildscript {
    repositories {
        maven { url 'https://dl.bintray.com/pqixing86/aex' }
        maven { url 'https://maven.aliyun.com/nexus/content/repositories/jcenter' }
    }
    dependencies { classpath 'com.pqixing.aex:module:1.0.10' }
}
//安装组件化模块的脚本
apply plugin: "com.pqixing.aex"

aex{
 //设置当前(主项目)的名称和maven仓库和git仓库地址. 此maven和git会成为其他git项目的缺省配置
 root("name",maven,git){}
 //...更多aex信息配置如下
}

```

在aex插件管理的所有module的sync中,都可以通过获取到aex属性进行数据查询,例如,在任意的build.gradle中
```groovy 
  //遍历所有定义在aex中的模块,此Project非gradle中的Project
 aex.projects {ProjectX pro ->
 
 }
 //得到定义在aex中的模块
 def aexProjects = aex.projects
 
 //更多属性查看  AEX配置

```

## aex配置 
> aex的属性和方法,可以在aex{} 中调用,文档更新不一定及时,可见点开直接查看类定义
-----------

#### 1.可配置的属性 : 参考 [com.pqixing.model.define.IManifest](../libs/tools/src/main/java/com/pqixing/model/define/IManifest.kt)
* usebr(Boolean) :  是否开启maven仓库的分支隔离模式,
  > 开启后,上传到maven仓库的aar的group名会追加分支名称作为隔离. 多分支间引用不可混用. 可参考fallbacks

* fallbacks(List<String>) :  分支隔离引用的传递传递名称集合
  > + 默认情况隔离下, 代码分支只能读取同名分支的maven仓库中的产品. fallback机制提供了一个复用的方案
  > + 例如:fallbacks = [develop,master] , 当前开发分支是feat.  有模块A, B ,C , 其中A -> B -> C ,其中master分支均使用A,B,C打包到Maven仓库
  > + 当 B 模块,在develop打包过, 导入A模块时, 此时会依赖的是   develop.B,master.C 
  > + 当 在feat分支打包提交了C以后,   此时A 模块依赖会是   develop.B,feat.C
  > fallback机制的提供,方便了新建分支开发时,无须全量打包提交到maven仓库,按开发需要打包即可
  
* branch(String) : 设置主分支名称, 默认读取AEX主项目的分支名称
    > 如果当前aex主项目head处于非正常分支时(tag或者commit),由于分支隔离会导致从maven仓库中寻依赖包失败.可以手动设置此参数为实际分支

* [config](#Config) : 个性化配置,如果日志开启,index同步等
* [mTypes](#TypeX)  : 所有模块类型定义集合
* [mavens](#IMaven) : Maven仓库配置集合,可配置多个,不同git可使用不同的maven进行打包管理
* [gits](#IGit)     : Git仓库配置集合,可配置多个,不同git可使用不同的git配置进行源码管理
* [projects](#ProjectX)    :  所有的Git项目配置信息, 每个Git中可包含多个module



#### 2.可调用的方法 : 参考 [com.pqixing.aex.model.define.IManifestEx](src/main/java/com/pqixing/aex/model/define/IManifestEx.kt)
    > 以下方法均为辅助设置 aex 的属性

+    fun root(name: String, maven: String, git: String, closure: Closure<*>): ProjectEx
+    fun root(name: String, maven: IMaven, git: IGit, closure: Closure<*>): ProjectEx

+    fun fallbacks():MutableList<String>
+    fun fallbacks(values: List<String>)

+    fun config(): IConfig
+    fun config(closure: Closure<*>): IConfig

+    fun type(name: String): TypeX
+    fun type(name: String, closure: Closure<*>): TypeX
+    fun types(closure: Closure<*>)
     
+    fun maven(name: String): IMaven
+    fun maven(name: String, closure: Closure<*>): IMaven
+    fun from(name: String, from: IMaven, closure: Closure<*>): IMaven
+    fun mavens(closure: Closure<*>)
     
+    fun git(name: String): IGit
+    fun git(name: String, closure: Closure<*>? = null): IGit
+    fun from(name: String, from: IGit, closure: Closure<*>): IGit
+    fun gits(closure: Closure<*>)

+    fun project(name: String): ProjectEx
+    fun project(name: String, closure: Closure<*>?): ProjectEx
+    fun project(name: String, desc: String, closure: Closure<*>?): ProjectEx
+    fun project(name: String, desc: String, url: String, closure: Closure<*>?): ProjectEx
+    fun project(name: String, maven: IMaven, git: IGit, closure: Closure<*>?): ProjectEx
+    fun project(name: String, desc: String, url: String, maven: IMaven, git: IGit, closure: Closure<*>?): ProjectEx
+    fun projects(closure: Closure<*>)
     
<br>

### <span id="ProjectX">projects</span>: Git项目配置

> projects是aex配置中最主要的集合,定义所有git项目的信息,包含了所有可受aex管理的模块

#### 1.可配置的属性 : 参考 [com.pqixing.model.define.IProject](../libs/tools/src/main/java/com/pqixing/model/define/IProject.kt)
+   name(String)     :  项目名称,设置后,导入AndroidStudio显示的名称
+   desc(String)     :  项目描述,没啥用,设置给人看看就好
+   url(String)      :  项目url地址,默认等同于name , 完成gitUrl = git.url+ url+.git; 如果设置为完成地址,则不会拼接git.url
+   path(String)     :  下载目录名称,默认等同于name
+   [maven(IMaven)](#IMaven)     :  改Git项目中所有模块使用的maven配置信息
+   [git(IGit)](#IGit)           :  git设置配置
+   [modules(ModuleX)](#ModuleX) :  所有的模块集合

#### 2.可调用的方法 : 参考 [com.pqixing.aex.model.define.IProjectEx](src/main/java/com/pqixing/aex/model/define/IProjectEx.kt)
+    fun module(name: String): ModuleX
+    fun module(name: String, desc: String): ModuleX
+    fun module(name: String, desc: String, type: String): ModuleX
+    fun module(name: String, closure: Closure<*>?): ModuleX
+    fun module(name: String, desc: String, closure: Closure<*>?): ModuleX
+    fun module(name: String, desc: String, type: String, closure: Closure<*>?): ModuleX
+    fun asModule(type: String): ModuleX
+    fun asModule(type: String, closure: Closure<*>): ModuleX
+    fun modules(closure: Closure<*>)