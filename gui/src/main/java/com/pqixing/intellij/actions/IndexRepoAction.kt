package com.pqixing.intellij.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.awt.RelativePoint
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.ui.autoComplete
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.git.GitHelper
import com.pqixing.model.BrOpts
import java.awt.Point
import java.io.File
import javax.swing.*


class IndexRepoAction : XEventAction<IndexRepoDialog>()

open class IndexRepoDialog(e: AnActionEvent) : XModuleDialog(e) {
    lateinit var centerPanal: JPanel
    lateinit var cbTarget: JCheckBox
    lateinit var btnExcludes: JButton
    lateinit var tvExcludes: JTextField
    lateinit var jbTarget: JComboBox<String>

    override fun initWidget() {
        super.initWidget()
        cbTarget.addActionListener { refresh() }
        val repo = GitHelper.getRepo(File(project.basePath), project)
        val brs = mutableListOf(manifest.branch)
        brs += repo.branches.localBranches.map { it.name }
        brs += repo.branches.remoteBranches.map { it.name.substringAfter("/") }

        jbTarget.autoComplete(project, brs)

        //混略分支选择
        val datas = brs.distinctBy { it }.filter { it.isNotEmpty() }
        btnExcludes.addActionListener { showExcludeSelect(btnExcludes, datas) }
        refresh()
    }

    override fun loadList(): List<XItem> = emptyList()

    private fun showExcludeSelect(c: JComponent, datas: List<String>) {
        val curs = tvExcludes.text.split(",").toSet()
        val options = datas.map { PopOption(it, it, "", curs.contains(it)) }

        XListPopupImpl(project, "Select Branches", options) { _, pop, _ ->
            tvExcludes.text = options.filter { it.selected }.map { it.title }.joinToString(",")
        }.show(RelativePoint(c, Point(0, c.height + 10)))
    }

    private fun refresh() {
        val enable = cbTarget.isSelected
        btnExcludes.isEnabled = enable
        tvExcludes.isEnabled = enable
        jbTarget.isEnabled = enable
    }

    override fun getTitleStr(): String = "Indexing"
    override fun doOKAction() {
        super.doOKAction()
        val opts = BrOpts()
        if (cbTarget.isSelected) {
            opts.target = jbTarget.selectedItem?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            opts.brs = tvExcludes.text.split(",").plus(opts.target ?: "").map { it.trim() }.filter { it.isNotEmpty() }
        }
        toIndex(opts)
    }

    private fun toIndex(opts: BrOpts) {
        val envs = mutableMapOf("include" to "null", "opts" to opts.toString())
        GradleRequest(listOf("IndexRepo"), envs).executeTasks(project) {
            if (it.success) XApp.invoke {
                FileEditorManager.getInstance(project).openFile(VfsUtil.findFileByIoFile(File(it.getDefaultOrNull() ?: ""), true)!!, true)
            }
            else XApp.notify(
                project,
                "IndexRepo",
                "Gradle Task Error ${it.getDefaultOrNull()}",
                NotificationType.WARNING,
                listOf(XNotifyAction("Retry") { toIndex(opts) })
            )

        }
    }

    override fun createMenus(): List<JComponent?> {
        return emptyList()
    }

    override fun createCenterPanel(): JComponent = centerPanal
}

