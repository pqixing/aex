package com.pqixing.intellij.actions

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getOrElse
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.ui.weight.*
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.intellij.uitils.UiUtils
import com.pqixing.model.impl.ModuleX
import com.pqixing.tools.FileUtils
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.awt.Point
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*


open class XImportAction : XEventAction<XImportDialog>()

class XImportDialog(e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    val VCS_KEY = "Vcs"
    val VCS_SORTED = "Sorted"
    private lateinit var tvSearch: JTextField
    private lateinit var btnMore: JButton
    private lateinit var cbLocal: JCheckBox
    private lateinit var cbSorted: JComboBox<String>
    override fun getTitleStr(): String = "Import : ${manifest.branch}"
    val imports = manifest.importModules().map { it.name }

    override fun initWidget() {
        super.initWidget()
        cbSorted.selectedItem = VCS_SORTED.getSp("Topo", project)
        cbSorted.addActionListener { VCS_SORTED.putSp(cbSorted.selectedItem?.toString() ?: "Topo", project);resorted() }
        cbLocal.isSelected = config.local
        btnMore.addActionListener { showMore(btnMore) }
        tvSearch.registerKeyboardAction({ tvSearch.text = "" }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW)
        tvSearch.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent?) {
                val keyCode: Int = e?.keyCode ?: return
                XApp.log("keyReleased -> $keyCode , ${e.keyChar} ,${e.extendedKeyCode} ,${e.source}")
                val key: String = tvSearch.text.trim()
                when (keyCode) {
                    KeyEvent.VK_DOWN, KeyEvent.VK_CONTROL -> showSelectPop(tvSearch,
                        adapter.datas().filter { UiUtils.match(key, listOf(it.title, it.content, it.tag)) }
                    )
                }
                for (it in adapter.datas()) {
                    it.visible = key.isEmpty() || UiUtils.match(key, listOf(it.title, it.content, it.tag))
                }
            }
        })
    }

    override fun afterInit() {
        super.afterInit()
        resorted()
        if(XHelper.checkRepoUpdate(true,project.basePath!!, manifest.root.name, manifest.root.project.maven)){
            btnMore.toolTipText = "Some components have been updated"
            btnMore.icon = AllIcons.Actions.Refresh
        }
    }

    fun showMore(c: JComponent) {
        val vcsItem = MyMenuItem("Vcs", null, VCS_KEY.getSp("Y", project) == "Y") { VCS_KEY.putSp(it.select.getOrElse("N", "Y"), project) }
        val syncItem = MyMenuItem("Sync", null, false) { syncRepo() }
        val localItem = MyMenuItem("open: local.gradle", null, false) { m -> XApp.openFile(project, File(basePath, "local.gradle")) }
        val settingItem = MyMenuItem("open: settings.gradle", null, false) { m -> XApp.openFile(project, File(basePath, "settings.gradle")) }
        c.showPop(listOf(syncItem, vcsItem, localItem, settingItem), Point(0, c.height + 10))
    }

    private fun resorted() {
        val items = when (cbSorted.selectedItem?.toString()) {
            "Name" -> adapter.datas().sortedBy { it.title }
            "Project" -> {
                val fls = manifest.projects.map { it.modules }.flatten().map { it.name }
                adapter.datas().sortedBy { fls.indexOf(it.title) }
            }
            else -> {
                val topo = manifest.sorted().map { it.name }
                adapter.datas().sortedBy { topo.indexOf(it.title) }
            }
        }
        adapter.set(items)
    }

    fun syncRepo() {
        val exitCode = Messages.showOkCancelDialog(project, "Sync the version for all module from maven ?", "", "Ok", "Cancel", null)
        if (exitCode == Messages.OK) XApp.invokeWrite {
            XApp.log("Start Sync From Maven : ${manifest.root.project.maven.url}")
            val file = XHelper.reloadRepoMetaFile(true, project.basePath!!, manifest.root.name, manifest.root.project.maven)
            XApp.notify(project, "End Sync From Maven", file.absolutePath, actions = listOf(XNotifyAction("open") { XApp.openFile(project, file) }))
            btnMore.toolTipText = ""
            btnMore.icon = AllIcons.General.MoreTabs
        }
    }

    override fun doOKAction() {
        super.doOKAction()

        XApp.runAsyn { indictor ->
            indictor.text = "Start Import"

            val imports = adapter.datas().filter { it.select }.map { it.title }
            saveConfig(imports)
            //下载代码
            val projects = manifest.sorted().filter { imports.contains(it.name) }.map { it.project }.toSet()
            for (p in projects) {
                val dir = p.absDir()
                if (GitUtil.isGitRoot(dir)) continue
                FileUtils.delete(dir)
                val url = p.getGitUrl()
                indictor.text = "Start Clone ${url} "
                //下载master分支
                GitHelper.clone(project, manifest.branch, dir, url, GitHelper.GitIndicatorListener(indictor))
            }
            XApp.syncVcs(project, manifest.projects, VCS_KEY.getSp("Y") == "Y", true)
            indictor.text = "Start Sync Code"
            //如果快速导入不成功,则,同步一次
            //ActionManager.getInstance().getAction("ExternalSystem.RefreshAllProjects").actionPerformed(e)
            val syncAction = if (isGradle(e)) "Android.SyncProject" else "ExternalSystem.RefreshAllProjects"
            XApp.invoke { ActionManager.getInstance().getAction(syncAction).actionPerformed(e) }
        }

    }

    private fun saveConfig(imports: Collection<String>) = XApp.invokeWrite {
        val local = cbLocal.isSelected
        val include = imports.filter { it.isNotEmpty() }.joinToString(",")
        XHelper.saveIdeaConfig(basePath, local, include)
    }

    override fun getPreferredFocusedComponent(): JComponent? = tvSearch

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        super.onItemUpdate(item, module, repo)
        item.select = imports.contains(module.name)
        item.tvContent.addMouseClickR { c, e -> onContentClickR(item, module, c, e) }
        return true
    }

    private fun onContentClickR(item: XItem, module: ModuleX, c: JComponent, e: MouseEvent) {
        val default = manifest.config.build
        val cacheDir = File(module.absDir(), "build/")
        val items =
            listOf("${default}/depend.gradle", "${default}/merge.gradle", "ide/depend.gradle", "ide/merge.gradle").map { it to File(cacheDir, it) }

        val menus = items.filter { it.second.exists() }.map {
            MyMenuItem<Any>("open: ${it.first}", null, false) { m -> XApp.openFile(project, it.second) }
        }.toMutableList()
        if (menus.isNotEmpty()) c.showPop(menus, Point(e.x, e.y))
    }

    private fun isGradle(e: AnActionEvent): Boolean = kotlin.runCatching {
        val project = e.project ?: return false
        val info = GradleProjectInfo.getInstance(project)
        return info.isBuildWithGradle && (info.androidModules.isNotEmpty() || IdeInfo.getInstance().isAndroidStudio)
    }.getOrDefault(false)
}


