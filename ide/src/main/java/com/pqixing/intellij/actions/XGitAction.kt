package com.pqixing.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getOrElse
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.ui.autoComplete
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.ui.weight.addMouseClick
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.model.impl.ProjectX
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.repo.GitRepository
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Point
import java.awt.event.ActionListener
import java.io.File
import java.lang.reflect.Modifier
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

open class XGitAction : XEventAction<XGitDialog>()
class XGitDialog(e: AnActionEvent) : XModuleDialog(e) {
    val KEY_PRO = "project"
    val KEY_BRS = "BRS"
    val SP_CMDS = "CMDS"
    val SP_LAST_GIT_OP = "SP_LAST_GIT_OP"
    val SP_LAST_GIT_MODEL = "SP_LAST_GIT_MODEL"
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    private lateinit var cbBrn: JComboBox<String>
    private lateinit var cbOp: JComboBox<IGitCmd>
    private lateinit var customPanel: JPanel
    private lateinit var jlGit: JLabel
    lateinit var tvCommand: TextFieldWithAutoCompletion<String>

    var customModel = SP_LAST_GIT_MODEL.getSp("N", project).toString() == "Y"
    val listener = GitHelper.GitIndicatorListener(null)

    val lastCmds = SP_CMDS.getSp("", project).toString().split(",").filter { it.isNotEmpty() }.toMutableList()

    //预设命令
    val preSet = mutableListOf(Checkout(this), Clone(this), Merge(this), Create(this), Delete(this))
    val refresh = ActionListener { refresh() }

    //自定义命令
    val customs = GitCommand::class.java.declaredFields.filter { Modifier.isStatic(it.modifiers) }
        .mapNotNull { it.isAccessible = true;it.get(null) as? GitCommand }.map { Custom(this, it) }

    override fun initWidget() {
        super.initWidget()
        tvCommand = TextFieldWithAutoCompletion.create(project, lastCmds, true, lastCmds.firstOrNull() ?: "")
        tvCommand.setPreferredWidth(180)
        tvCommand.toolTipText = "Env:\$target , \$name , \$branch"

        customPanel.add(tvCommand, BorderLayout.CENTER)

        val preview = createLinkText("view", AllIcons.General.LinkDropTriangle) { customPreview(customPanel) }

        customPanel.add(preview, BorderLayout.EAST)

        XApp.syncVcs(project, manifest.projects, true, false)
    }

    private fun customPreview(btnPreview: Component) {
        val cmdParam = tvCommand.text.trim()
        val gitOp = cbOp.selectedItem as IGitCmd
        val selectBr = cbBrn.selectedItem?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "master"
        val options = adapter.datas().filter { it.visible && it.select }.map {
            val cmdStr = "   git $gitOp " + cmdParam
                .replace("\$target", selectBr)
                .replace("\$name", it.title)
                .replace("\$branch", it.tag ?: "")
            PopOption(it, cmdStr, "  ->  ${it.title}", true) { s -> it.select = s }
        }
        val pop = XListPopupImpl(project, "", options) { _, _, _ -> }
        pop.setMinimumSize(Dimension(btnPreview.width - 8, 0))
        pop.show(RelativePoint(btnPreview, Point(3, btnPreview.height)))
    }

    override fun afterInit() {
        super.afterInit()
        XApp.invoke { onModeChage(customModel) }
    }

    fun onModeChage(custom: Boolean) {
        customModel = custom
        customPanel.isVisible = custom
        cbOp.removeActionListener(refresh)
        cbOp.removeAllItems()

        for (i in if (custom) customs else preSet) cbOp.addItem(i)
        if (custom) {
            val lastOp = SP_LAST_GIT_OP.getSp("", project)?.toString()
            customs.find { it.name == lastOp }?.let { cbOp.selectedItem = it }
        }
        cbOp.addActionListener(refresh)
        refresh()
        onSelect(null)
    }

    override fun getTitleStr(): String = "Git:${manifest.branch}"

    override fun loadList(): List<XItem> {
        val brs = mutableListOf<String>(manifest.branch)
        val items = manifest.projects.map { pro ->
            XItem().also { item ->
                item.visible = false
                item.title = pro.name
                item.select = true
                item.selectAble = true
                item.content = pro.desc
                item.right = { _, _ -> }
                item.tvTitle.addMouseClick(item.left) { c, e -> }
                item.tvTag.addMouseClick(item.left) { c, e -> onTagClickR(item, c, e) }
                item.tvContent.addMouseClick(item.left) { c, e -> onContentClickR(item, c, e, onCopyItems(item)) }
                item.params[KEY_DATA] = pro
                brs += loadRepo(pro, item)
            }
        }
        XApp.invoke { cbBrn.autoComplete(project, brs) { refresh.actionPerformed(null) } }
        return items
    }

    override fun doOKAction() {
        btnEnable(false)
        XApp.runAsyn { indicator ->
            listener.indicator = indicator
            val selectBr = cbBrn.selectedItem?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "master"
            val gitOp = cbOp.selectedItem as IGitCmd
            val selects = adapter.datas().filter { it.visible && it.select }
            val cmdStr = tvCommand.text.trim()
            if (gitOp is Custom && cmdStr.isNotEmpty()) {
                lastCmds.remove(cmdStr)
                lastCmds.add(0, cmdStr)
                SP_CMDS.putSp(lastCmds.joinToString(","), project)
                SP_LAST_GIT_OP.putSp(gitOp.name, project)
            }

            SP_LAST_GIT_MODEL.putSp(customModel.getOrElse("Y", "N"), project)
            adapter.datas().forEach { it.state = XItem.KEY_IDLE }
            indicator.text = "Start : $gitOp"
            if (gitOp.before(selectBr, selects)) {
                //打开日志面板
                XApp.activateWindow(project, "Event Log")
                selects.forEach {
                    listener.setCache(true)

                    it.state = XItem.KEY_WAIT
                    indicator.text = "$gitOp -> ${it.title} : ${it.tag}"
                    listener.onLineAvailable("exe $gitOp -> ${it.title} : ${it.tag}", null)

                    it.state = if (kotlin.runCatching { gitOp.exe(selectBr, it, cmdStr) }.getOrElse { false }) XItem.KEY_SUCCESS else XItem.KEY_ERROR
                    listener.setCache(false)
                }
            }
            //先同步
            XApp.syncVcs(project, manifest.projects, true, true)
            //执行完,尝试更新分支信息
            selects.forEach { loadRepo(it.get<ProjectX>(KEY_PRO)!!, it) }
            listener.indicator = null
            XApp.invoke { btnEnable(true) }
        }
    }

    override fun btnEnable(enable: Boolean) {
        super.btnEnable(enable)
        cbOp.isEnabled = enable
        cbBrn.isEnabled = enable
    }

    fun loadRepo(pro: ProjectX, item: XItem): Collection<String> {
        val brs = mutableSetOf<String>()
        val repo = item.get<GitRepository>(KEY_REPO) ?: pro.absDir().toRepo()
        repo?.update()
        item.params[KEY_REPO] = repo
        item.params[KEY_PRO] = pro
        if (repo != null) {
            brs += repo.branches.localBranches.map { it.name }
            brs += repo.branches.remoteBranches.map { it.name.substringAfter("/") }
            item.params[KEY_BRS] = brs
        }
        item.tag = repo?.currentBranchName ?: ""
        return brs
    }

    override fun createMenus(): List<JComponent?> {

        val updateModel = { model: Boolean, c: LinkLabel<String> ->
            onModeChage(model)
            c.icon = if (customModel) AllIcons.Actions.Selectall else ICON_UNCHECKED
        }

        val custom = createLinkText("custom", ICON_UNCHECKED) { updateModel(!customModel, it) }
        jlGit.addMouseClick { c, e -> updateModel(!customModel, custom) }

        updateModel(customModel, custom)

        val callAction = { id: String -> ActionManager.getInstance().getAction(id).actionPerformed(e) }

        val pull = createLinkText("pull", ICON_UNCHECKED) { callAction("Vcs.UpdateProject") }
        val push = createLinkText("push", ICON_UNCHECKED) { callAction("Vcs.Push") }
        return listOf(custom, pull, push)
    }


    fun refresh() {
        val gitOp = cbOp.selectedItem as? IGitCmd ?: return
        val selectBr = cbBrn.selectedItem?.toString() ?: ""
        for (item in adapter.datas()) {
            item.visible = gitOp.visible(selectBr, item)
        }
    }

    fun File?.toRepo() = this?.takeIf { GitUtil.isGitRoot(it) }?.let { GitHelper.getRepo(it, project) }


}

