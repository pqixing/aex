package com.pqixing.intellij.actions

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.impl.ProjectManagerImpl
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.ui.autoComplete
import com.pqixing.intellij.ui.weight.XEventDialog
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.tools.FileUtils
import java.awt.Desktop
import java.awt.Rectangle
import java.io.File
import java.net.URI
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

open class XProjectAction : XEventAction<XProjectDialog>() {
    override fun update(e: AnActionEvent) {
    }
}

open class XProjectDialog(e: AnActionEvent) : XEventDialog(e, e.project ?: ProjectManagerImpl.getInstance().defaultProject) {
    lateinit var tvDir: JTextField
    lateinit var centerPanal: JPanel
    lateinit var tvFilePick: JButton
    lateinit var btnDoc: JButton
    lateinit var tvGitUrl: JTextField
    lateinit var tvDirName: JLabel
    lateinit var cbBrs: JComboBox<String>
    val rename: JCheckBox = JCheckBox("rename", null, false).also {
        it.addChangeListener { e ->
            tvDirName.isVisible = !it.isSelected
            val r: Rectangle = content.bounds
            centerPanal.repaint(r.x, r.y, r.width, r.height)
        }
    }
    val rootDir = LocalFileSystem.getInstance().findFileByPath(basePath)

    override fun init() {
        super.init()
        val manifest = XHelper.readManifest(basePath)
        tvFilePick.addActionListener {
            FileChooser.chooseFiles(
                FileChooserDescriptor(false, true, false, false, false, false),
                project,
                rootDir?.parent
            ) { files: List<VirtualFile> ->
                files.firstOrNull()?.let { tvDir.text = it.canonicalPath }
            }
        }
        tvGitUrl.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                updateUrlName()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                updateUrlName()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                updateUrlName()
            }
        })

        tvDir.text = rootDir?.parent?.canonicalPath ?: ""
        tvGitUrl.text = manifest?.root?.project?.getGitUrl()?.takeIf { it.isNotEmpty() } ?: "https://github.com/pqixing/aex.git"
        val repo = kotlin.runCatching { GitHelper.getRepo(File(project.basePath ?: ""), project) }.getOrNull()
        val brs = mutableListOf<String>(manifest?.branch ?: "master")
        if (repo != null) {
            brs += repo.branches.localBranches.map { it.name }
            brs += repo.branches.remoteBranches.map { it.name.substringAfter("/") }
        }
        cbBrs.autoComplete(project, brs)
        btnDoc.addActionListener { Desktop.getDesktop().browse(URI("https://github.com/pqixing/aex")) }


    }

    private fun updateUrlName() {
        tvDirName.text = "/" + tvGitUrl.text.trim().substringAfterLast("/").substringBefore(".")
    }

    override fun getTitleStr(): String = "Open AEX Project"
    override fun doOKAction() {
        val path = tvDir.text.trim()
        val url = tvGitUrl.text.trim()
        if (path.isEmpty() || url.isEmpty()) return
        super.doOKAction()
        downloadNewProject(project, cbBrs.selectedItem?.toString(), File(path), url)
    }

    private fun downloadNewProject(target: Project, branch: String?, dir: File, url: String) = XApp.invoke {
        val cloneDir = dir.takeIf { rename.isSelected } ?: File(dir, url.substringAfterLast("/").substringBeforeLast("."))
//        if (Messages.showOkCancelDialog(project, " clone : $url \n to $cloneDir", "Start Clone", "OK", "Cancel", null) != Messages.OK) return@invoke
//        dispose()
        if (cloneDir.exists()) {
            val exitCode = Messages.showOkCancelDialog(target, "Path ${cloneDir.absolutePath} exists!!!", "DELETE", "DEL", "CANCEL", null)
            if (exitCode != Messages.OK) return@invoke
            FileUtils.delete(cloneDir)
        }
        XApp.runAsyn(target, "Start clone ") { indicator ->
            indicator.text = "clone $url"
            val success = GitHelper.clone(target, branch?.takeIf { it.isNotEmpty() }
                ?: "master", cloneDir, url, GitHelper.GitIndicatorListener(indicator))
            if (success) ProjectUtil.openOrImport(cloneDir.absolutePath, project, true)
            else XApp.invoke { Messages.showMessageDialog("See Event Log Panel", "Clone Fail", null) }
        }
    }

    override fun createMenus(): List<JComponent?> {
        return listOf(rename)
    }


    override fun createCenterPanel(): JComponent = centerPanal
}