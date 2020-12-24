package com.pqixing.intellij.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.ui.JBFilterWrapper
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.ui.weight.addMouseClick
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.model.impl.ProjectX
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.repo.GitRepository
import java.awt.event.ActionListener
import java.io.File
import java.lang.reflect.Modifier
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

open class XGitAction : XEventAction<XGitDialog>()
class XGitDialog(e: AnActionEvent) : XModuleDialog(e) {
    val KEY_PRO = "project"
    val KEY_BRS = "BRS"
    val SP_CMDS = "CMDS"
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    private lateinit var cbBrn: JComboBox<String>
    private lateinit var cbOp: JComboBox<IGitCmd>
    private lateinit var cbCustom: JCheckBox
    lateinit var cbCmd: JComboBox<String>
    val listener = GitHelper.GitIndicatorListener(null)

    val lastCmds = SP_CMDS.getSp("", project).toString().split(",").filter { it.isNotEmpty() }.toMutableList()

    //预设命令
    val preSet = mutableListOf(Checkout(this), Clone(this), Merge(this), Create(this), Delete(this))
    val refresh = ActionListener { refresh() }
    val cbBrnWapper = JBFilterWrapper(cbBrn) { refresh.actionPerformed(null) }

    //自定义命令
    val customs = GitCommand::class.java.declaredFields.filter { Modifier.isStatic(it.modifiers) }
        .mapNotNull { it.isAccessible = true;it.get(null) as? GitCommand }.map { Custom(this, it) }

    override fun initWidget() {
        super.initWidget()
        lastCmds.forEach { cbCmd.addItem(it) }
        XApp.syncVcs(project, manifest.projects, true, false)
        cbCustom.addItemListener { onModeChage(cbCustom.isSelected) }
    }

    override fun afterInit() {
        super.afterInit()
        onModeChage(false)
    }

    fun onModeChage(custom: Boolean) {
        cbCmd.isVisible = custom
        cbOp.removeActionListener(refresh)
        cbOp.removeAllItems()
        for (i in if (custom) customs else preSet) cbOp.addItem(i)
        cbOp.addActionListener(refresh)
        refresh()
        onSelect(null)
    }

    override fun getTitleStr(): String = "Git:${manifest.branch}"

    override fun loadList(): List<XItem> {
        val brs = mutableListOf<String>(manifest.branch)
        val items = manifest.projects.map { pro ->
            XItem().also { item ->
                item.title = pro.name
                item.select = true
                item.selectAble = true
                item.content = pro.desc
                item.right = { _, _ -> }
                item.tvTitle.addMouseClick(item.left) { c, e -> }
                item.tvTag.addMouseClick(item.left) { c, e -> onTagClickR(item, c, e, listOf(pro.getGitUrl())) }
                brs += loadRepo(pro, item)
            }
        }
        cbBrnWapper.setDatas(brs)
        return items
    }

    override fun doOKAction() {
        XApp.runAsyn { indicator ->
            listener.indicator = indicator
            btnEnable(false)
            val selectBr = cbBrn.selectedItem?.toString()?.trim()?.takeIf { it.isNotEmpty() } ?: "master"
            val gitOp = cbOp.selectedItem as IGitCmd
            val selects = adapter.datas().filter { it.visible && it.select }
            val cmdStr = cbCmd.selectedItem?.toString() ?: ""
            if (gitOp is Custom && cmdStr.isNotEmpty()) {
                if (!lastCmds.remove(cmdStr)) cbCmd.addItem(cmdStr)
                lastCmds.add(0, cmdStr)
                SP_CMDS.putSp(lastCmds.joinToString(","), project)
            }
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
            btnEnable(true)
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


    fun refresh() {
        val gitOp = cbOp.selectedItem as? IGitCmd ?: return
        val selectBr = cbBrn.selectedItem?.toString() ?: ""
        for (item in adapter.datas()) {
            item.visible = gitOp.visible(selectBr, item)
        }
    }

    fun File?.toRepo() = this?.takeIf { GitUtil.isGitRoot(it) }?.let { GitHelper.getRepo(it, project) }


}

