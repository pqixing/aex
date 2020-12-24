package com.pqixing.intellij.ui.weight

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.ui.adapter.XBaseAdapter
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.ModuleX
import git4idea.repo.GitRepository
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.TitledBorder

open class XModuleDialog(e: AnActionEvent) : XEventDialog(e) {
    val manifest = XHelper.readManifest(basePath) ?: ManifestX(basePath)
    val config = manifest.config
    val KEY_REPO = "repo"


    override fun init() {
        super.init()
        initWidget()
        XApp.runAsyn {
            adapter.set(loadList())
            afterInit()
        }
    }

    open fun afterInit() {
        adapter.datas().forEach { item -> item.cbSelect.addItemListener { onSelect(item) } }
        onSelect(null)
    }

    open fun initWidget() {}
    open fun loadList(): List<XItem> {
        val repos = manifest.projects.map { it to GitHelper.getRepo(it.absDir(), project) }.toMap()
        return manifest.sorted().filter { !it.typeX().root() }.mapNotNull { m ->
            val repo = repos[m.project]
            XItem().also { item ->
                item.title = m.name
                item.content = "${m.desc} - ${m.project.name} - ${m.type}"
                item.tag = repo?.currentBranchName ?: ""
                item.tvTitle.addMouseClick(item.left) { c, e -> onTitleClickR(item, m, c, e) }
                item.tvTag.addMouseClick(item.left) { c, e -> onTagClickR(item, c, e) }
                item.tvContent.addMouseClick(item.left) { _, _ -> }
                item.params[KEY_REPO] = repo
                onItemUpdate(item, m, repo)
            }.takeIf { onItemUpdate(it, m, repo) }
        }
    }

    open fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean = true

    protected open fun onTagClickR(item: XItem, c: JComponent, e: MouseEvent, copys: List<String> = emptyList()) {
        val repo = item.get<GitRepository>(KEY_REPO)
        val menus = copys.map { MyMenuItem<Any>("cp: $it", null, false) { m -> XApp.copy(it) } }.toMutableList()
        if (repo != null) {
            val brs =
                repo.branches.let { brs -> brs.localBranches.map { it.name }.plus(brs.remoteBranches.map { it.name.substringAfter("/") }) }.toSet()
            menus += brs.sorted().map { br -> MyMenuItem("co: $br", null, false) { checkout(br, repo, item) } }
        }
        if (menus.isNotEmpty()) c.showPop(menus, Point(e.x, e.y))
    }

    protected open fun onCopyClickR(item: XItem, c: JComponent, e: MouseEvent, copys: List<String> = emptyList()) {
        val menus = copys.map { MyMenuItem<Any>("copy: $it", null, false) { m -> XApp.copy(it) } }.toMutableList()
        if (menus.isNotEmpty()) c.showPop(menus, Point(e.x, e.y))
    }

    protected open fun checkout(branch: String, repo: GitRepository, item: XItem): Unit = GitHelper.checkout(project, branch, listOf(repo)) {
        repo.update();
        item.tag = repo.currentBranchName ?: ""
    }

    /**
     * 点击Item右键
     */
    protected open fun onTitleClickR(
        item: XItem,
        module: ModuleX?,
        c: JComponent,
        e: MouseEvent
    ) {
        val datas = adapter.datas()
        val projects = module?.project?.modules?.map { it.name }?.let { l -> datas.filter { l.contains(it.title) } } ?: emptyList()
        val depends = module?.dps()?.let { l -> datas.filter { l.contains(it.title) } } ?: emptyList()

        val items = listOf("project" to projects, "depend" to depends).map { m ->
            val select = m.second.all { it.select }
            MyMenuItem(m.first + " (${m.second.size})", m.second, select) { menu -> m.second.forEach { it.select = !select } }
        }
        c.showPop(items, Point(e.x, e.y))
    }


}

open class XEventDialog(val e: AnActionEvent, val project: Project = e.project!!, val module: Module? = e.getData(DataKey.create<Module>("module"))) :
    XDialog(project) {
    protected var basePath = project.basePath ?: System.getProperty("user.home")
}

open class XDialog(project: Project?) : DialogWrapper(project, true, false) {

    protected var adapter: XBaseAdapter
    protected lateinit var content: JScrollPane
    protected lateinit var center: JPanel
    protected val cbAll: JCheckBox by lazy { getAllCheckBox().also { c -> c.addActionListener { doOnAllChange(c.isSelected) } } }
    protected val border = content.border as TitledBorder
    protected open fun doOnAllChange(selected: Boolean) {
        adapter.datas().forEach { it.select = selected }
    }

    protected open fun getAllCheckBox(): JCheckBox = JCheckBox("All", null, false)

    init {
        adapter = XBaseAdapter(center)
        content.addMouseClick { c, e -> if (e.x <= 150 && e.y <= 30) showSelectPop(c, point = Point(e.x, e.y)) }
        isModal = false
    }


    protected open fun showSelectPop(
        c: JComponent,
        datas: List<XItem> = adapter.datas().filter { it.visible && it.select },
        point: Point = Point(0, c.height + 10)
    ) {
        if (datas.isEmpty()) return
        val click = { i: MyMenuItem<XItem> -> i.data?.let { it.select = !it.select } ?: Unit }
        c.showPop(datas.map { m -> MyMenuItem(m.title, m, m.select, click) }, point)
    }

    protected open fun getTitleStr(): String = ""

    protected open fun onSelect(item: XItem?) {
        val selects = adapter.datas().count { it.select && it.visible }
        border.title = "  Select: $selects  ▼  "
        content.repaint()
    }

    override fun show() {
        title = getTitleStr()
        // call onCancel() on ESCAPE
        contentPanel.registerKeyboardAction(
            { doCancelAction() },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        )
        init()
        super.show()
    }

    protected open fun btnEnable(enable: Boolean) {
//        myCancelAction.isEnabled = enable
        myOKAction.isEnabled = enable
    }

    fun repaint(component: JComponent) {
        component.parent?.let { parent ->
            val r: Rectangle = component.bounds
            parent.repaint(r.x, r.y, r.width, r.height)
        }
        component.revalidate()
    }

    override fun createDoNotAskCheckbox(): JComponent? = cbAll
    override fun createCenterPanel(): JComponent? = content

}