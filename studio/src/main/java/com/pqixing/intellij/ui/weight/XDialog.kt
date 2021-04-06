package com.pqixing.intellij.ui.weight

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.scale.JBUIScale
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.ui.adapter.XBaseAdapter
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.pop.showPopup
import com.pqixing.intellij.uitils.GitHelper
import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.ModuleX
import com.pqixing.model.impl.ProjectX
import git4idea.repo.GitRepository
import java.awt.FlowLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.TitledBorder

open class XModuleDialog(e: AnActionEvent) : XEventDialog(e) {
    val manifest = XHelper.readManifest(basePath) ?: ManifestX(basePath)
    val config = manifest.config
    val KEY_REPO = "repo"
    val KEY_DATA = "data"


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
                item.content = m.desc + "-" + m.type
                item.tag = repo?.currentBranchName ?: ""
                item.tvTitle.addMouseClick(item.left) { c, e -> onTitleClickR(item, m, c, e) }
                item.tvTag.addMouseClick(item.left) { c, e -> onTagClickR(item, c, e) }
                item.tvContent.addMouseClick(item.left) { c, e -> onContentClickR(item, c, e, onCopyItems(item)) }
                item.params[KEY_REPO] = repo
                item.params[KEY_DATA] = m
                onItemUpdate(item, m, repo)
            }.takeIf { onItemUpdate(it, m, repo) }
        }
    }

    open fun onCopyItems(item: XItem): List<String> {
        val data = item.params[KEY_DATA] ?: return emptyList()
        val list = mutableListOf(item.title, item.content, item.tag)
        list += if (data is ModuleX) {
            listOf(
                data.project.getGitUrl(),
                data.maven().url,
                "${data.group()}:${data.name}:${data.version}"
            )
        } else if (data is ProjectX) {
            listOf(
                data.getGitUrl(),
                data.maven.url
            )
        } else emptyList()
        return list.filter { it.isNotEmpty() }
    }

    open fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean = true

    protected open fun onTagClickR(item: XItem, c: JComponent, e: MouseEvent) {
        val repo = item.get<GitRepository>(KEY_REPO) ?: return
        val brs = repo.branches.let { brs -> brs.localBranches.map { it.name }.plus(brs.remoteBranches.map { it.name.substringAfter("/") }) }.toSet()

        val menus = brs.sorted().map { br -> PopOption("", br, "", false) { checkout(br, repo, item) } }
        if (menus.isNotEmpty()) c.showPopup(project, menus, "Checkout Branch", Point(e.x, e.y))
    }

    protected open fun onContentClickR(item: XItem, c: JComponent, e: MouseEvent, copys: List<String>) {
        val menus = copys.map { PopOption("", it, "", false) { _ -> XApp.copy(it) } }.toMutableList()
        if (menus.isNotEmpty()) c.showPopup(project, menus, "Copy Item", Point(e.x, e.y))
    }

    protected open fun checkout(branch: String, repo: GitRepository, item: XItem) {
        item.state = XItem.KEY_WAIT
        GitHelper.checkout(project, branch, listOf(repo)) {
            repo.update();
            item.tag = repo.currentBranchName ?: ""
            item.state = if (item.tag == branch) XItem.KEY_SUCCESS else XItem.KEY_ERROR
        }
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
            PopOption(m.second, m.first, " (${m.second.size})", select) { _ -> m.second.forEach { it.select = !select } }
        }
        c.showPopup(project, items, "Select Item", Point(e.x, e.y))
    }


}

open class XEventDialog(val e: AnActionEvent, project: Project = e.project!!, val module: Module? = e.getData(DataKey.create<Module>("module"))) :
    XDialog(project) {
    protected var basePath = project.basePath ?: System.getProperty("user.home")
}

open class XDialog(var project: Project) : DialogWrapper(project, true, false) {

    protected var adapter: XBaseAdapter
    protected lateinit var content: JScrollPane
    protected lateinit var center: JPanel
    protected val border = content.border as TitledBorder
    protected val morePanel = JPanel(FlowLayout())

    init {
        adapter = XBaseAdapter(center)
        content.addMouseClick { c, e -> showOperator(c, e) }
        isModal = false
    }

    fun adapter() = adapter
    protected open fun checkClickValid(e: MouseEvent) = e.x <= 300 && e.y <= 20
    protected open fun showOperator(c: JComponent, e: MouseEvent) {
        if (!checkClickValid(e)) return
        when (e.x) {
            in 10..55 -> changeSelect { true }//ALL
            in 56..118 -> changeSelect { false }//NONE
            in 119..184 -> changeSelect { !it.select } // INVERT
            in 185..380 -> showSelectPop(c, e)//SELECT
        }
    }

    private fun changeSelect(call: (item: XItem) -> Boolean) {
        adapter.datas().forEach { it.select = call(it) }
    }

    fun scrollToTarget(key: XItem?) {
        val find = adapter.datas().find { it == key } ?: return
        center.scrollRectToVisible(find.jItemRoot.bounds)
    }

    protected open fun moreActions(): List<PopOption<String>> = emptyList()

    protected open fun showSelectPop(c: JComponent, e: MouseEvent) {

        val title = "Select Item "

        val optins = adapter.datas().filter { it.visible }
            .map { PopOption(it, it.title, it.content, it.select) { s -> it.select = s } }
            .sortedBy { !it.selected }

        val itemClick = { left: Boolean, pop: XListPopupImpl<XItem>, o: PopOption<XItem> ->
            if (!left) {
                scrollToTarget(o.option)
                pop.dispose()
            }
        }

        XListPopupImpl(project, title, optins, itemClick).show(RelativePoint(c, Point(180 - c.x, 23)))
    }

    protected open fun getTitleStr(): String = ""

    open fun onSelect(item: XItem?) {
        val visible = adapter.datas().count { it.visible }
        val selects = adapter.datas().count { it.select && it.visible }
        val font = { key: String -> "&nbsp&nbsp&nbsp&nbsp<u><b>$key</b></u>&nbsp&nbsp&nbsp&nbsp" }
        border.title = "<html>  |${font("all")}|${font("none")}|${font("invert")}|${font("select : $selects/$visible")}|  </html>"
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

    protected open fun createMenus(): List<JComponent?> {

        val onLinkClick = LinkListener<String> { c, d ->
            val moreActions = moreActions()
            if (moreActions.isEmpty()) return@LinkListener
            XListPopupImpl(project, "", moreActions).show(RelativePoint(c, Point(0, c.height + 10)))
        }
        val more = LinkLabel("more", AllIcons.General.LinkDropTriangle, onLinkClick).apply {
            iconTextGap = JBUIScale.scale(1)
            horizontalAlignment = SwingConstants.LEADING
            horizontalTextPosition = SwingConstants.LEADING
        }
        return listOf(more)
    }

    final override fun createDoNotAskCheckbox(): JComponent? {
        val menus = createMenus().mapNotNull { it }
        if (menus.isEmpty()) return null
        menus.forEach { morePanel.add(it) }
        return morePanel
    }

    override fun createCenterPanel(): JComponent? = content

}