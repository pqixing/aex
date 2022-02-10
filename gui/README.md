[AEX](https://github.com/pqixing/aex) - [IDEA ( 下载 )](https://plugins.jetbrains.com/plugin/16145-aex) : 使用文档 
---------
> IDEA插件使用操作手册,配合GRADLE插件进行组件化管理<br>
> 更多文档: 配置文档: [AEX_GRADLE](https://github.com/pqixing/aex/blob/main/aex/README.md) ;  <span id="framework">架构文档</span>: [AEX](https://github.com/pqixing/aex) 

--------

此插件所有的集中在AEX菜单中,在集成了AEX的项目中(gradle sync成功后), 在任意文本编辑器, 或者项目列表中右键菜单,均可看到AEX菜单集合.

####  总包含6个模块: **[Import](#Import)** ,**[Builder](#Builder)** ,**[ToMaven](#ToMaven)** ,**[Indexing](#Indexing)** ,**[GitBatch](#GitBatch)** ,**[OpenAEX](#OpenAEX)** ;
其中[OpenAEX](#OpenAEX)菜单在Welcome-Configure或打开项目的File菜单中均有注册.

<br>

---------


零.  列表
---------

6个模块中,其中有4个模块时基于列表的操作菜单, 都具备通用的操作功能,如截图序号
#### 1. 列表标题: 左键选中/取消;  右键显示速选菜单
> project : 同时选中/取消 该模块所在git工程中的其他模块;depend:同时选中/取消 该模块所依赖的其他模块(递归);

#### 2. 速选操作栏:  all = 全选模块 , none = 取消所有选择 , invert = 反选模块 , select:*/* = 已选模块悬浮菜单
>  已选菜单展示中,选中模块排序最前,左键点击选中/取消, 右键点击跳转列表中模块所在位置. 输入任意字符快速搜索过滤

#### 3. 分支显示: 源码的分支名称, 源码未下载时为空. 右键显示切换分支菜单
#### 4. 模块描述: 模块的类型和介绍展示, 右键显示可复制的文本条目,点击可复制到粘贴板
#### 5. 更多:  点击展示更多菜单,不同功能菜单不一样,详细见功能描述

以上所有条目,默认字体颜色跟随系统,执行任务时有三种字体颜色.黄色:正在执行; 绿色:执行成功;红色:执行失败

![img](https://plugins.jetbrains.com/files/16145/screenshot_b9d2c431-ce49-433e-b3b2-9ac403b92371)

<br>

一. <span id="Import">Import</span> : 模块导入
---------
> 按照需求选择需要开发的模块导入,导入的模块间可进行本地源码依赖,也可以互不干扰各自依赖仓库开发

#### 1. 搜索: 输入需要搜索的模块字段进行过滤
> 模糊搜索支持: 例如,模块名称叫 Demo1 , 输入 dm1即可搜索

#### 2.列表排序,支持三种模式
* Topo: 拓扑排序; 按照全局模块的依赖进行排序,依赖层级越高在前
* Project: 项目排序; 按照aex清单中配置好的顺序排序
* Name: 名称排序; 按照模块名称排序


#### 3. <span id="local"> 本地依赖</span>,[本地依赖详细点这里](#framework)
>  勾选Local,对于导入的模块之间使用本地project方式依赖,快速调试开发,不勾选,或者是没有导入的相关模块,则默认使用maven仓库中的组件进行依赖
#### 4. <span id="Log"> 日志显示</span>
> 勾选后,gradle任务执行时,会输出aex插件相关日志

#### 5. 更多菜单:
* Sync: 从远程同步所有组件的版本信息,默认不自动同步
    > 当远程存在版本信息更新时,Sync , More, 和 AEX-Import菜单前,出现刷新图标
* Vcs: 自动给IDE添加当前所有管理并且已经下载的代码git信息记录,IDE能管理全部代码,如果异常,可以不勾选
* local.gradle: 打开本地的local.gradle文件
* settings.gradle: 打开本地 settings.gradle文件
* depend.gradle: 列出所有模块下存在的depend.gradle文件, 该文件编译生成,包含内部模块的所有依赖配置
* merge.gradle: 列出所有模块下存在的depend.gradle文件,该文件编译生成,相当于模块的build.gradle文件(aex插件hook后的产物)

![img](https://plugins.jetbrains.com/files/16145/screenshot_97e79813-d679-4b6f-9acd-2311712fc860)

<br>

二. <span id="Builder">Builder</span>: AEX构建器
---------

> 快速对指定模块编译运行,支持library和application类型,分离build目录,不影响正常的代码开发


#### 1. 构建任务: 选择或者输入需要构建的任务
> 默认支持assembleDebug,assembleRelease,如果有更多buildType和渠道设置,可以自己输入名称

#### 2. 本地依赖: [同Import中Local](#local)

> 此处的Local只会影响本次编译,不会对IDE导入的代码有影响

#### 3. TooBar: 工具栏上固定快速运行的按钮 , 见4
#### 4. 快速运行: 快速运行上次Builder构建的项目

> 构建参数读取上一次构建; 如果没有上一次运行,则打开Builder对话框窗口

#### 5. 更多菜单:
* AllView: 勾选此项后,显示所有aex清单中配置的模块,默认列表只显示导入IDE中的模块
* Params: 输入自定义的gradle参数,etc: key:vaule,key2:value2
*  <span id="version">Mapping</span>: 版本记录文件路径,点击可以自定义输入路径,[版本文件详细作用点这里](../module/README.md#jum1)
* Install: 输入adb安装的参数

![img](https://plugins.jetbrains.com/files/16145/screenshot_83c1d2f8-eee8-44ca-bb90-9c73dce8be49)

<br>

三. <span id="ToMaven">ToMaven</span>: AEX打包器
---------
> 批量对指定模块进行打包上传,分离build目录,不影响正常代码开发


#### 1. 检查条件: 
> 默认进行打包时,插件会进行一些额外的校验来排除误操作,部分情况下,如果需要强行打包,可以勾选指定条件不检测

* repeat: 重复项校验; 插件打包时,会记录当前模块的commit号,下次打包时,如果检测到跟上次打包的commit一致,则不重复打包. 选中即可不做校验
* clean: 代码校验; 默认情况下,本地存在未提交代码进行打包时,插件会抛异常. 选中即可不做校验
* branch: 分支校验; 默认情况下,插件不允许当前模块与主模块分支不同,(打包会以主模块分支为准). 选中即可不做校验

#### 2. 更多菜单: 参考[Builder](#Builder)更多菜单

![img](https://plugins.jetbrains.com/files/16145/screenshot_1315974e-67f7-4008-961c-e82e48f833a6)

<br>

四. <span id="Indexing">Indexing</span>: 索引重建
--------

> AEX插件默认会记录每个组件打包的记录,来达到自动为组件进行版本管理的目的. 具体实现见 [AEX-GRADLE](#framework)
  如果出现版本混乱,或者找不到某个版本的异常问题,可执行此功能重建索引

#### 1. Make Tag: 标签功能
> 对于多分支开发,如果使用了分支隔离maven, 可以用此功能记性版本数据的tag记录
* 未勾选,则默认是重建索引,直接点OK执行即可.
* 选中即为打Tag功能, 同时解锁2,3,4操作

#### 2. Tag名称: 重建索引后,保存为Tag名称的文件
> 版本文件作用在于,可以覆盖当前版本信息. 举例是,A模块在打tag1.0时, 版本是1.0.1; 经过两个月开发,版本号到1.0.9; 默认情况下,插件会自动应用最新版本号; 假如需要构建会两个月前的安装包时,则可以应用tag1.0文件重新构建即可,[如何应用](#version)

* 分支名: 如果名称为实际分支名称时, 在开启分支隔离功能,并且主项目分支名称一致时,会自动应用此文件作为进行版本管理
* 其他名称: 按照实际需要可以填入其他名称,并且在执行完成以后,可手动保存此文件,或者文件url地址(日志会打印出来),下次按需使用

#### 3. Excludes: 标签忽略分支,点击会弹出分支选择菜单
> 默认情况下,建立索引会记录除当前Tag名称对应分支外的所有分支的版本信息,如果需要指定忽略某些分支,可点击选择分支

#### 4. Excludes: 标签忽略分支,手动输入
> (3)中选择的分支也会填入此处,也可手动修改, 格式为逗号分隔,etc:  name1,name2,name3

![img](https://plugins.jetbrains.com/files/16145/screenshot_80ab90f9-47a3-45e1-966f-afa3b8101351)

<br>


五. <span id="GitBatch">GitBatch</span>: Git管理器
-----------

> 默认模式: 提供checkout, clone,merge,create,delete五种常用命令,自定义输入框(3)隐藏不可见,根据命令多条目进行过滤,结果做了异常处理,傻瓜式调用即可

> 自定义模式:提供git支持的所有命令,自定义输入框(3)可见,可自行输入对应参数,见描述(3).所有条目可见,需要自行选择项目执行,关注结果输出



#### 1. Git命令选择: 选择要操作的git命令

#### 2. 分支选择: 可手动输入,或者选择即将使用的目标分支名称

#### 3. 自定义输入框:  自定义模式下可见

> 自定义模式下, 执行的命令为 git cmd(命令选择)  (3).text , 其中输入框中支持三个变量输入

* $branch : 当前选中条目的分支, 比如截图中,选中的第一条aex显示的分支main (5),则在给aex项目执行命令时就是, git add $branch ->  git add main
* $target : 目标分支名称 , 下拉框中选中或者输入的分支名
* $name   : 选中条目的名字

* 使用举例 当选中 checkout , 自定义输入框输入文本 -b $target origin/$branch ->   git checkout -b main origin/main

#### 4. 命令预览:  自定义模式下,可以预览每个模块即将会执行的完整git命令

#### 5. 更多菜单:
* Custom: 切换自定义模式
* Pull: 更新所有本地源码(调用了ide的pull功能)
* Push: Push所有本地源码(调用了ide的push功能)
  
  ![img](https://plugins.jetbrains.com/files/16145/screenshot_a437113e-e7c6-4945-8547-f85acca7fcef)
  
<br>

六. <span id="OpenAEX">OpenAEX</span>: 快速打开
---------

> 此工具会下载主项目,并且打开主项目, 当管理人员集成了aex框架以后, 只需要其他开发人员安装[AEX - IDE](https://plugins.jetbrains.com/plugin/16145-aex) 插件,并提供主项目的git地址即可接入整个项目的开发. 


#### 1. 主项目git地址:  默认填入当前主项目地址

#### 2. 下载分支:  Clone成功后会切换到设定的分支

#### 3. 下载目录:  输入下载到本地的目录

#### 4. 选择目录:  使用本地文件系统选择下载目录地址

#### 5. 说明文档:  点击使用浏览器打开AEX框架说明文档

#### 6. 重命名:   是否需要对下载目录进行重命名

+ 没有勾选重命名, 输入的下载目录为../code/pqx; 下载地址为../aex.git时, 最终主项目的目录地址为../code/pqx/aex
+ 勾选了重命名, 输入的下载目录即为最终的目录, 不进行git名称的拼接

  ![img](https://plugins.jetbrains.com/files/16145/screenshot_86cab52b-3ba3-45c7-88d5-05832f877f11)

----------
### 想要快速体验AEX框架
1. AndroidStudio或者IDE中搜索AEX插件安装,或者[点这里](https://plugins.jetbrains.com/plugin/16145-aex) 下载AEX插件安装
2. File ->Open AEX 新创建AEX项目,默认地址是https://github.com/pqixing/aex.git
3. 右键菜单 -> AEX -> Import 快速导入需要的项目即可体验




