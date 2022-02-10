package com.pqixing.intellij.ui.weight

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.EmptyIcon
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.ui.adapter.XBaseAdapter
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.pop.showPopup
import com.pqixing.intellij.git.GitHelper
import com.pqixing.model.impl.ManifestX
import com.pqixing.model.impl.ModuleX
import com.pqixing.model.impl.ProjectX
import git4idea.repo.GitRepository
import java.awt.FlowLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*

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
            tBranch.text = manifest.branch
        }
    }

    override fun onCheckoutModel(c: JComponent, e: MouseEvent) {
        val model = tCheckout.text
        val options = listOf("root", "select", "all").map { m ->
            PopOption(m, m, "", m == model) {
                tCheckout.text = m
                VCS_MODEL_CHECKOUT.putSp(m, project)
            }
        }
        c.showPopup(project, options, "checkout model", Point(0, c.height))
    }

    protected open fun onCheckout(branch: String) {

    }

    override fun onBranchClick(c: JComponent, e: MouseEvent) {

        val projectDir = File(project.basePath ?: return)

        val repo = GitHelper.getRepo(projectDir, project) ?: return

        val branch = manifest.branch

        val model = tCheckout.text

        val brs = repo.branches.let { brs -> brs.localBranches.map { it.name }.plus(brs.remoteBranches.map { it.name.substringAfter("/") }) }.toSet()

        val menus = brs.sorted().map { br -> PopOption(br, br, "", br == branch) }.sortedBy { !it.selected }

        if (menus.isNotEmpty()) XListPopupImpl(project, "$model : checkout branches", menus) { r, p, i ->
            p.dispose()

            //根据不同的模型设置切换的条目
            val items = when (model) {
                "all" -> adapter.datas()
                "select" -> adapter.datas().filter { it.visible && it.select }
                else -> emptyList()
            }

            startCheckouts(repo, i.title, items)
        }.show(RelativePoint(c, Point(0, c.height)))
    }

    private fun startCheckouts(root: GitRepository, branch: String, items: List<XItem>) = XApp.runAsyn {

        it.text = "checkout root to $branch"
        tBranch.text = "checkout ..."
        GitHelper.checkoutSync(project, branch, listOf(root))
        root.update()
        manifest.branch = branch
        tBranch.text = branch

        for (item in items) kotlin.runCatching {
            it.text = "checkout ${item.title} to $branch"
            item.state = XItem.KEY_WAIT
            val repo = item.get<GitRepository>(KEY_REPO)
            if (repo == null) item.state = XItem.KEY_ERROR
            else {
                GitHelper.checkoutSync(project, branch, listOf(repo))
                repo.update()
                item.tag = repo.currentBranchName ?: ""
                item.state = if (item.tag == branch) XItem.KEY_SUCCESS else XItem.KEY_ERROR
            }
        }
        onCheckout(root.currentBranchName ?: branch)
    }

    open fun afterInit() {
        adapter.datas().forEach { item -> item.tvTitle.addItemListener { onSelect(item) } }
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
                item.tvContent.addMouseClick(item.left) { c, e -> onContentClickR(item, c, e, onCopyItems(item)) }
                item.tvTag.addMouseClick { c, e -> onTagClickR(item, c, e) }
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
        val branch = item.tag
        val brs = repo.branches.let { brs -> brs.localBranches.map { it.name }.plus(brs.remoteBranches.map { it.name.substringAfter("/") }) }.toSet()
        val menus = brs.sorted().map { br -> PopOption("", br, "", branch == br) { checkout(br, repo, item) } }.sortedBy { !it.selected }
        if (menus.isNotEmpty()) c.showPopup(project, menus, "checkout", Point(e.x, e.y))
    }

    protected open fun onContentClickR(item: XItem, c: JComponent, e: MouseEvent, copys: List<String>) {
        val menus = copys.map { PopOption("", it, "", false) { _ -> XApp.copy(it) } }.toMutableList()
        if (menus.isNotEmpty()) c.showPopup(project, menus, "copy", Point(e.x, e.y))
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
        c.showPopup(project, items, "select", Point(e.x, e.y))
    }


}

open class XEventDialog(val e: AnActionEvent, project: Project = e.project!!, val module: Module? = e.getData(DataKey.create<Module>("module"))) :
    XDialog(project) {
    protected var basePath = project.basePath ?: System.getProperty("user.home")
}

open class XDialog(var project: Project) : DialogWrapper(project, true, false) {

    companion object {
        val ICON_UNCHECKED = EmptyIcon.create(16)
    }

    protected val VCS_MODEL_CHECKOUT = "VCS_MODEL_CHECKOUT"
    protected var adapter: XBaseAdapter
    protected lateinit var center: JScrollPane
    protected lateinit var top: JPanel
    protected lateinit var data: JPanel

    protected lateinit var root: JPanel

    protected lateinit var tSelect: JLabel
    protected lateinit var tAll: JLabel
    protected lateinit var tNone: JLabel
    protected lateinit var tInvert: JLabel
    protected lateinit var tBranch: JLabel
    protected lateinit var tCheckout: JLabel
    protected lateinit var tRoot: JLabel

    protected val morePanel = JPanel(FlowLayout())

    init {
        adapter = XBaseAdapter(data)
        center.verticalScrollBar.unitIncrement = 10
        isModal = false
        tSelect.addMouseClick { c, e -> showSelectPop(tSelect, e) }
        tAll.addMouseClick { c, e -> changeSelect { true } }
        tNone.addMouseClick { c, e -> changeSelect { false } }
        tInvert.addMouseClick { c, e -> changeSelect { !it.select } }
        tBranch.addMouseClick { c, e -> onBranchClick(c, e) }
        tCheckout.addMouseClick { c, e -> onCheckoutModel(c, e) }
        tCheckout.text = VCS_MODEL_CHECKOUT.getSp("all", project).toString()
    }

    protected open fun onCheckoutModel(c: JComponent, e: MouseEvent) {

    }

    protected open fun onBranchClick(c: JComponent, e: MouseEvent) {

    }

    fun adapter() = adapter

    private fun changeSelect(call: (item: XItem) -> Boolean) {
        adapter.datas().forEach { it.select = call(it) }
    }

    fun scrollToTarget(key: XItem?) {
        val find = adapter.datas().find { it == key } ?: return
        data.scrollRectToVisible(find.jItemRoot.bounds)
    }

    protected open fun moreActions(): List<PopOption<String>> = emptyList()

    protected open fun showSelectPop(c: JComponent, e: MouseEvent) {
        val title = ""

        val optins = adapter.datas().filter { it.visible }
            .map { PopOption(it, it.title, it.content, it.select) { s -> it.select = s } }
            .sortedBy { !it.selected }

        val itemClick = { left: Boolean, pop: XListPopupImpl<XItem>, o: PopOption<XItem> ->
            if (!left) {
                scrollToTarget(o.option)
                pop.dispose()
            }
        }

        XListPopupImpl(project, title, optins, itemClick).show(RelativePoint(c, Point(0, 23)))
    }

    protected open fun getTitleStr(): String = ""

    open fun onSelect(item: XItem?) {
        val visible = adapter.datas().count { it.visible }
        val selects = adapter.datas().count { it.select && it.visible }
        tSelect.text = "   $selects  /  $visible   |"
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
        val more = createLinkText("more", AllIcons.General.LinkDropTriangle) { c ->
            val moreActions = moreActions()
            if (moreActions.isNotEmpty()) {
                XListPopupImpl(project, "", moreActions).show(RelativePoint(c, Point(0, c.height + 10)))
            }
        }
        return listOf(more)
    }

    protected open fun createLinkText(text: String, icon: Icon?,alignment:Int = SwingConstants.LEADING, click: (c: LinkLabel<String>) -> Unit): LinkLabel<String> {
        return LinkLabel<String>(text, icon) { c, d -> click(c as LinkLabel<String>) }.apply {
            iconTextGap = JBUIScale.scale(1)
            horizontalAlignment = alignment
            horizontalTextPosition = alignment
        }
    }

    final override fun createDoNotAskCheckbox(): JComponent? {
        val menus = createMenus().mapNotNull { it }
        if (menus.isEmpty()) return null
        menus.forEach { morePanel.add(it) }
        return morePanel
    }

    override fun createCenterPanel(): JComponent? = root

}