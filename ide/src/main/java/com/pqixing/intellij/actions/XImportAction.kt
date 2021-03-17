package com.pqixing.intellij.actions

import com.android.tools.idea.IdeInfo
import com.android.tools.idea.gradle.project.GradleProjectInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.ui.SearchTextField
import com.intellij.ui.awt.RelativePoint
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getOrElse
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.pop.showPopup
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.ui.weight.addMouseClick
import com.pqixing.intellij.ui.weight.addMouseClickR
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.intellij.uitils.UiUtils
import com.pqixing.model.impl.ModuleX
import com.pqixing.tools.FileUtils
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.awt.Point
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


open class XImportAction : XEventAction<XImportDialog>() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.icon = if (XApp.isRepoUpdate(e.project, false)) AllIcons.Actions.Refresh else null
    }
}

class XImportDialog(e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    val VCS_KEY = "Vcs"
    val VCS_SORTED = "Sorted"
    private lateinit var tvSearch: SearchTextField
    private lateinit var cbLocal: JCheckBox
    private lateinit var cbLog: JCheckBox
    private lateinit var jlTips: JLabel
    private lateinit var cbSorted: JComboBox<String>
    val sync = JLabel(AllIcons.Actions.Refresh).also {
        it.toolTipText = "update version from remote"
        it.addMouseClick { c, e -> showSyncDialog() }
    }

    override fun getTitleStr(): String = "Import : ${manifest.branch}"
    val imports = manifest.importModules().map { it.name }

    override fun initWidget() {
        super.initWidget()
        cbSorted.selectedItem = VCS_SORTED.getSp("Topo", project)
        cbSorted.addActionListener { VCS_SORTED.putSp(cbSorted.selectedItem?.toString() ?: "Topo", project);resorted() }
        cbLocal.isSelected = config.local
        cbLog.isSelected = config.log
        initDebug()
        tvSearch.textEditor.document.addDocumentListener(object : DocumentListener {
            fun onKeyUpdate() {
                val key: String = tvSearch.text.trim()
                for (it in adapter.datas()) {
                    it.visible = key.isEmpty() || UiUtils.match(key, listOf(it.title, it.content, it.tag))
                }
                onSelect(null)
            }

            override fun insertUpdate(e: DocumentEvent?) {
                onKeyUpdate()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                onKeyUpdate()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                onKeyUpdate()
            }
        })
    }

    private fun initDebug() {
        var debugClick = 0
        var lastTime = 0L
        jlTips.addMouseClick { _, _ ->
            if (System.currentTimeMillis() - lastTime > 500L) {
                debugClick = 0
            }
            lastTime = System.currentTimeMillis()
            if (debugClick++ % 5 == 0) {
                XDebugDialog(e).show()
                this.dispose()
            }
        }
    }

    override fun afterInit() {
        super.afterInit()
        resorted()
        sync.isVisible = XApp.isRepoUpdate(project, true) {
            sync.isVisible = it
        }
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

    fun showSyncDialog() {
        val exitCode = Messages.showOkCancelDialog(project, "Sync the version for all module from maven ?", "", "Ok", "Cancel", null)
        if (exitCode == Messages.OK) XApp.invokeWrite {
            XApp.log("Start Sync From Maven : ${manifest.root.project.maven.url}")
            val file = XHelper.reloadRepoMetaFile(true, basePath, manifest.root.name, manifest.root.project.maven)
            XApp.notify(project, "End Sync From Maven", file.absolutePath, actions = listOf(XNotifyAction("open") { XApp.openFile(project, file) }))
            sync.isVisible = XHelper.checkRepoUpdate(false, basePath, manifest.root.name, manifest.root.project.maven)
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
        val include = imports.filter { it.isNotEmpty() }.joinToString(",")
        val file = File(basePath, ".idea/local.gradle")
        val txt = "aex{ config{ include='$include' ; local = ${cbLocal.isSelected} ; log = ${cbLog.isSelected}}}"
        FileUtils.writeText(file, txt)
    }

    override fun getPreferredFocusedComponent(): JComponent? = tvSearch

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        super.onItemUpdate(item, module, repo)
        item.select = imports.contains(module.name)
        return true
    }

    override fun createMenus(): List<JComponent?> {
        return listOf(sync) + super.createMenus()
    }

    private fun listGradleFile(name: String) {
        val sorted = manifest.sorted()
        val name1 = manifest.config.build
        val name2 = "ide"
        val options = mutableListOf<PopOption<ModuleX>>()
        for (m in sorted) {
            if (File(m.absDir(), "build/$name1/$name").exists()) {
                options += PopOption(m, m.name, name1)
            }
            if (File(m.absDir(), "build/$name2/$name").exists()) {
                options += PopOption(m, m.name, name2)
            }
        }

        XListPopupImpl(project, "File : $name", options) { _, pop, o ->
            pop.dispose()
            XApp.openFile(project, File(o.option!!.absDir(), "build/${o.desc}/$name"))
        }.show(RelativePoint(morePanel, Point(0, morePanel.height + 10)))
    }

    override fun moreActions(): List<PopOption<String>> {
        return listOf(
            PopOption("", "Sync", "update version from remote") { showSyncDialog() },
            PopOption("", "Vcs", "add git path to ide auto", VCS_KEY.getSp("Y", project) == "Y") { VCS_KEY.putSp(it.getOrElse("Y", "N"), project) },
            PopOption("", "local.gradle", "open local file", false) { XApp.openFile(project, File(basePath, "local.gradle")) },
            PopOption("", "settings.gradle", "open local file", false) { XApp.openFile(project, File(basePath, "settings.gradle")) },
            PopOption("", "depend.gradle", "list all file", false) { listGradleFile("depend.gradle") },
            PopOption("", "merge.gradle", "list all file", false) { listGradleFile("merge.gradle") }
        )
    }


    private fun isGradle(e: AnActionEvent): Boolean = kotlin.runCatching {
        val project = e.project ?: return false
        val info = GradleProjectInfo.getInstance(project)
        return info.isBuildWithGradle && (info.androidModules.isNotEmpty() || IdeInfo.getInstance().isAndroidStudio)
    }.getOrDefault(false)
}


