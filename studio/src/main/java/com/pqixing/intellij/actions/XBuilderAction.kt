package com.pqixing.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.uitils.UiUtils
import com.pqixing.intellij.uitils.UiUtils.realName
import com.pqixing.model.impl.ModuleX
import git4idea.repo.GitRepository
import java.awt.event.MouseEvent
import javax.swing.*

class XBuilderAction : XEventAction<XBuilderDialog>() {
    val FAST_PLACE = "NavBarToolbar"
    val FAST_PARAMS = mutableMapOf<Int, BuildParam?>()

    override fun update(e: AnActionEvent) {
        if (e.place != FAST_PLACE) super.update(e) else {
            val param = FAST_PARAMS[e.project?.hashCode() ?: 0]
            e.presentation.isVisible = param?.keep == true
            e.presentation.text = "Run '${param?.module}'[${param?.assemble}]"
            e.presentation.icon = AllIcons.Actions.Rerun
            e.presentation.isEnabled = param?.state != XItem.KEY_WAIT
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val last = FAST_PARAMS[project.hashCode()]
        if (e.place != FAST_PLACE || last?.module?.isNotEmpty() != true) XBuilderDialog(this, e).show()
        else tryToAssemble(project, FAST_PARAMS[project.hashCode()]) {}
    }

    fun tryToAssemble(project: Project, param: BuildParam?, finish: (result: String?) -> Unit) {
        val module = param?.module ?: return finish(null)
        param.state = XItem.KEY_WAIT
        val envs = mutableMapOf("include" to module, "assemble" to module, "local" to param.local.toString(), "mapping" to param.mapping)
        if (param.local) {
            envs["include"] = "${module},${XHelper.readManifest(project.basePath!!)?.config?.include}"
        }

        GradleRequest(listOf(":${param.module}:${param.assemble}"), envs).runGradle(project) {
            val success = it.success
            val result = it.getDefaultOrNull() ?: ""
            param.state = if (success) XItem.KEY_SUCCESS else XItem.KEY_ERROR
            if (success) {
                param.result = result
                XApp.invoke { UiUtils.tryInstall(project, null, result, param.install) }
            } else XApp.notify(project, "Build Fail", type = NotificationType.WARNING)
            finish(result.takeIf { it.isNotEmpty() })
            param.buildEnd?.run()
            param.buildEnd = null
        }
    }
}

class XBuilderDialog(val action: XBuilderAction, e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    private lateinit var tvInstall: JTextField
    private lateinit var btnFile: JButton
    private lateinit var btnPick: JButton
    private lateinit var tvMapFile: JTextField
    private lateinit var cbAssemble: JComboBox<String>
    private lateinit var cbLocal: JCheckBox
    private lateinit var cbShowAll: JCheckBox
    private lateinit var cbKeep: JCheckBox
    override fun createDoNotAskCheckbox(): JComponent? = null

    val param: BuildParam = action.FAST_PARAMS[project.hashCode()]
        ?: BuildParam(local = manifest.config.local, mapping = manifest.config.mapping).also { action.FAST_PARAMS[project.hashCode()] = it }
    val locals = ModuleManager.getInstance(project).modules.map { it.realName() }
    val curModule = param.module ?: e.getData(LangDataKeys.MODULE)?.realName()

    fun onShowAllChange(selected: Boolean) {
        adapter.datas().forEach { it.visible = selected || locals.contains(it.title) }
        onSelect(null)
    }

    override fun getTitleStr(): String = "AEXBuilder : ${manifest.branch}"

    override fun initWidget() {
        cbShowAll.addItemListener { onShowAllChange(cbShowAll.isSelected) }
        cbKeep.isSelected = param.keep
        cbLocal.isSelected = param.local
        tvInstall.text = param.install
        cbAssemble.selectedItem = param.assemble
        if (param.state == XItem.KEY_WAIT) {
            param.buildEnd = Runnable { refresh() }
        }
        cbKeep.addActionListener { param.keep = cbKeep.isSelected }
        //安装拖进来的安装包
        UiUtils.setTransfer(content) { files ->
            val apk = files.find { it.exists() && it.name.endsWith(".apk") }
            if (apk != null && Messages.showOkCancelDialog(project, "$apk", "Install", "Yes", "No", null) == Messages.OK) {
                UiUtils.tryInstall(project, null, "", param.install)
            }
        }
        initMapFile()
    }

    private fun initMapFile() {
        tvMapFile.text = param.mapping
        btnFile.toolTipText = tvMapFile.text
        btnFile.addActionListener {
            val inputMap = btnPick.isVisible
            for (component in (tvMapFile.parent as JPanel).components.sortedBy { it.x }) {
                if (component === btnFile) break
                component.isVisible = inputMap
            }
            btnPick.isVisible = !inputMap
            tvMapFile.isVisible = !inputMap
            btnFile.toolTipText = tvMapFile.text
        }

        btnPick.addActionListener { e ->
            FileChooser.chooseFiles(
                FileChooserDescriptor(true, false, false, false, false, false),
                project, project.projectFile
            ) { it.firstOrNull()?.canonicalPath?.let { l -> tvMapFile.text = l } }
        }
    }

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        item.select = curModule == module.name
        item.state = if (param.module == module.name) param.state else XItem.KEY_IDLE
        item.cbSelect.addItemListener { onSelect(item) }
        item.visible = locals.contains(module.name)
        return module.typeX().android()
    }

    override fun onTitleClickR(item: XItem, module: ModuleX?, c: JComponent, e: MouseEvent) {

    }

    private fun refresh() {
        isOKActionEnabled = param.state != XItem.KEY_WAIT && adapter.datas().find { it.select } != null
        val item = adapter.datas().find { it.title == param.module } ?: return
        item.state = param.state
    }

    override fun doOKAction() {
        val runModule = adapter.datas().find { it.select } ?: return
        super.doOKAction()
        param.module = runModule.title
        param.install = tvInstall.text
        param.local = cbLocal.isSelected
        param.keep = cbKeep.isSelected
        param.mapping = tvMapFile.text
        param.state = XItem.KEY_WAIT
        param.assemble = cbAssemble.selectedItem?.toString() ?: "assembleDebug"
        action.tryToAssemble(project, param) { refresh() }
        refresh()
    }

    /**
     * 只允许单选
     */
    override fun onSelect(item: XItem?) {
        super.onSelect(item)
        item ?: return
        val cacel = if (!item.select) null else adapter.datas().filter { it.select && it != item }
        //防止重复刷新
        if (cacel?.isNotEmpty() == true) cacel.forEach { it.select = false } else refresh()
    }
}

data class BuildParam(
    var module: String? = null,
    var assemble: String = "assembleDebug",
    var keep: Boolean = false,
    var local: Boolean = true,
    var install: String = "-r -t",
    var mapping: String = "",
    var result: String = ""
) {
    var state: String = ""
    var buildEnd: Runnable? = null
}
