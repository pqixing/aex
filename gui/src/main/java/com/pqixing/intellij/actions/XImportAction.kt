package com.pqixing.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.SearchTextField
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.EmptyIcon
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getOrElse
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.getSpList
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.XApp.putSpList
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.compat.AndroidCompat
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.git.GitHelper
import com.pqixing.intellij.common.UiUtils
import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.ModuleX
import com.pqixing.tools.FileUtils
import git4idea.GitUtil
import git4idea.repo.GitRepository
import java.awt.Point
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


open class XImportAction : XEventAction<XImportDialog>() {
    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.icon = if (XApp.isRepoUpdate(e.project, false)) AllIcons.Plugins.Downloads else null
    }
}

class XImportDialog(e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    val VCS_KEY = "Vcs"
    val VCS_SAVE_IMPORTS = "VCS_SAVE_IMPORTS"
    val VCS_SORTED = "Sorted"
    private lateinit var tvSearch: SearchTextField
    private lateinit var cbLocal: JCheckBox
    private lateinit var cbLog: JCheckBox
    private lateinit var cbSorted: JComboBox<String>

    override fun getTitleStr(): String = "Import"
    val imports = manifest.importModules().map { it.name }

    //记住初始化的分支
    val initBranch = manifest.branch

    override fun initWidget() {
        super.initWidget()
        cbSorted.selectedItem = VCS_SORTED.getSp("Topo", project)
        cbSorted.addActionListener { VCS_SORTED.putSp(cbSorted.selectedItem?.toString() ?: "Topo", project);resorted() }
        cbLocal.isSelected = config.local
        cbLog.isSelected = config.log
        tvSearch.textEditor.document.addDocumentListener(object : DocumentListener {
            fun onKeyUpdate() {
                val key: String = tvSearch.text.trim()

                //调试模式
                if (key.startsWith(":")) {
                    XDebugDialog.handleDebugAction(key, project, e, tvSearch)
                    return
                }

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

    override fun afterInit() {
        super.afterInit()
        resorted()
    }

    override fun onCheckout(branch: String) {
        super.onCheckout(branch)
        if (VCS_SAVE_IMPORTS.getSp("N", project) != "Y") {
            return
        }
        val lastImports = "${VCS_SAVE_IMPORTS}${branch}".getSpList(null, project)
        if (lastImports.isEmpty()) {
            return
        }
        adapter.datas().forEach { it.select = lastImports.contains(it.title) }
    }

    private fun resorted() {
        val items = when (cbSorted.selectedItem?.toString()) {
            "Name" -> adapter.datas().sortedBy { it.title }
            "Project" -> {
                val fls = manifest.projects.map { it.modules }.flatten().map { it.name }
                adapter.datas().sortedBy { fls.indexOf(it.title) }
            }
            "Topo" -> {
                val topo = manifest.sorted().map { it.name }
                adapter.datas().sortedBy { topo.indexOf(it.title) }
            }
            else -> {
                val topo = manifest.sorted().map { it.name }
                adapter.datas().sortedBy { topo.indexOf(it.title) }
            }
        }
        adapter.set(items)
    }

    private fun showFetchDialog(update: (s: Boolean) -> Unit = {}) {
        val exitCode = Messages.showCheckboxOkCancelDialog(
            "Fetch index from remote maven repo                                                  ",
            "Fetch", "Pull Code", "PULL_CODE".getSp("true", project) == "true", 0, 0, null
        )

        if (exitCode < 0 || exitCode == Messages.CANCEL) return

        XApp.runAsyn {
            it.text = "Start fetch index from maven : ${manifest.root.project.maven.url}"
            XApp.log(it.text)

            val file = XHelper.reloadRepoMetaFile(true, basePath, manifest.root.name, manifest.root.project.maven)
            XApp.notify(
                project, "End fetch index from maven ", file.absolutePath,
                actions = listOf(XNotifyAction("open") { XApp.openFile(project, file) })
            )
            update(XHelper.checkRepoUpdate(false, basePath, manifest.root.name, manifest.root.project.maven))
        }

        "PULL_CODE".putSp((exitCode == 1).toString(), project)
        if (exitCode == 1) {
            ActionManager.getInstance().getAction("Vcs.UpdateProject").actionPerformed(e)
        }
    }

    override fun doOKAction() {
        super.doOKAction()
        val imports = adapter.datas().filter { it.select }.map { it.title }
        //如果分支没变化,直接sync,否则,先执行一遍空sync,重新生成一次manifest文件
        if (initBranch == manifest.branch) {
            startImport(project, manifest, imports)
        } else GradleRequest(listOf("task"), mapOf("include" to "null", "build" to "default")).executeTasks(project) {
            val newManifest = if (it.success) XHelper.readManifest(basePath) else null
            XApp.invoke { startImport(project, newManifest ?: manifest, imports) }
        }
    }

    /**
     * 开始导入
     */
    private fun startImport(project: Project, manifest: ManifestX, imports: List<String>) = XApp.runAsyn { indictor ->

        indictor.text = "Start Import"

        saveConfig(imports)
        val branch = manifest.branch
        "${VCS_SAVE_IMPORTS}${branch}".putSpList(imports, project)
        //下载代码
        val projects = manifest.sorted().filter { imports.contains(it.name) }.map { it.project }.toSet()
        for (p in projects) {
            val dir = p.absDir()
            if (GitUtil.isGitRoot(dir)) continue
            FileUtils.delete(dir)
            val url = p.getGitUrl()
            indictor.text = "Start Clone ${url} "
            //下载master分支
            GitHelper.clone(project, branch, dir, url, GitHelper.GitIndicatorListener(indictor))
        }
        XApp.syncVcs(project, manifest.projects, VCS_KEY.getSp("Y") == "Y", true)
        indictor.text = "Start Sync Code"
        //如果快速导入不成功,则,同步一次
        //ActionManager.getInstance().getAction("ExternalSystem.RefreshAllProjects").actionPerformed(e)
        val syncAction = if (AndroidCompat.isGradleProject(project)) "Android.SyncProject" else "ExternalSystem.RefreshAllProjects"
        XApp.invoke { ActionManager.getInstance().getAction(syncAction).actionPerformed(e) }
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
        val ICON_EMPTY = EmptyIcon.create(12)

        val sync = LinkLabel<String>("fetch", ICON_EMPTY, null).apply {
            iconTextGap = JBUIScale.scale(1)
            horizontalAlignment = SwingConstants.LEADING
            horizontalTextPosition = SwingConstants.LEADING
        }
        val update = { s: Boolean -> sync.icon = if (s) AllIcons.Plugins.Downloads else ICON_EMPTY }
        val onLinkClick = LinkListener<String> { c, d ->
            showFetchDialog(update)
        }
        sync.setListener(onLinkClick, null)
        update(XApp.isRepoUpdate(project, true, update))

        val depend = LinkLabel<String>("depend", ICON_EMPTY) { _, _ -> listGradleFile("depend.gradle") }
            .apply {
                iconTextGap = JBUIScale.scale(1)
                horizontalAlignment = SwingConstants.LEADING
                horizontalTextPosition = SwingConstants.LEADING
            }

        return listOf(sync, depend) + super.createMenus()
    }

    private fun listGradleFile(name: String) {
        val sorted = manifest.sorted()
        val name1 = manifest.config.build
        val name2 = "ide"

        val selects = adapter.datas().filter { it.select && it.visible }.map { it.title }
        val options = mutableListOf<PopOption<ModuleX>>()
        for (m in sorted) {
            if (File(m.absDir(), "build/$name1/$name").exists()) {
                options += PopOption(m, m.name, name1, selects.contains(m.name))
            }
            if (File(m.absDir(), "build/$name2/$name").exists()) {
                options += PopOption(m, m.name, name2, selects.contains(m.name))
            }
        }


        XListPopupImpl(project, "File : $name", options.sortedBy { !it.selected }) { _, pop, o ->
            pop.dispose()
            XApp.openFile(project, File(o.option!!.absDir(), "build/${o.desc}/$name"))
        }.show(RelativePoint(morePanel, Point(0, morePanel.height + 10)))
    }

    override fun moreActions(): List<PopOption<String>> {
        return listOf(
            PopOption("", "vcs", "add git path to ide auto", VCS_KEY.getSp("Y", project) == "Y") { VCS_KEY.putSp(it.getOrElse("Y", "N"), project) },
            PopOption("", "local.gradle", "open local file", false) { XApp.openFile(project, File(basePath, "local.gradle")) },
            PopOption("", "settings.gradle", "open local file", false) { XApp.openFile(project, File(basePath, "settings.gradle")) },
            PopOption("", "bind_import", "bind imports for branch", VCS_SAVE_IMPORTS.getSp("N", project) == "Y") {
                VCS_SAVE_IMPORTS.putSp(it.getOrElse("Y", "N"), project)
            }
        )
    }
}


