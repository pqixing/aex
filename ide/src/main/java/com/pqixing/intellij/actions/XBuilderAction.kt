package com.pqixing.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.gradle.TaskPopParam
import com.pqixing.intellij.ui.autoComplete
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.uitils.UiUtils
import com.pqixing.intellij.uitils.UiUtils.realName
import com.pqixing.model.impl.ModuleX
import git4idea.repo.GitRepository
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

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
        val envs = mutableMapOf(
            "include" to module,
            "assemble" to module,
            "local" to param.local.toString(),
            "mapping" to "param_mapping_builder".getSp("", project).toString()
        )

        envs += "param_custom_builder".getSp("", project).toString().split(",").filter { it.contains(":") }
            .associate { it.substringBefore(":") to it.substringAfter(":") }

        if (param.local) {
            envs["include"] = "${module},${XHelper.readManifest(project.basePath!!)?.config?.include}"
        }

        GradleRequest(listOf(":${param.module}:${param.assemble}"), envs).runGradle(project) {
            val success = it.success
            val result = it.getDefaultOrNull() ?: ""
            param.state = if (success) XItem.KEY_SUCCESS else XItem.KEY_ERROR
            if (success) {
                param.result = result
                XApp.invoke { UiUtils.tryInstall(project, null, result, "param_custom_builder".getSp("-t -r", project).toString()) }
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
    private lateinit var cbAssemble: JComboBox<String>
    private lateinit var cbLocal: JCheckBox
    private lateinit var cbKeep: JCheckBox

    val params: BuildParam =
        action.FAST_PARAMS[project.hashCode()] ?: BuildParam(local = manifest.config.local).also { action.FAST_PARAMS[project.hashCode()] = it }
    val curModule = params.module ?: e.getData(LangDataKeys.MODULE)?.realName()

    val paramHelper = TaskPopParam("builder", project, this)

    override fun getTitleStr(): String = "AEXBuilder : ${manifest.branch}"

    override fun initWidget() {
        cbKeep.isSelected = params.keep
        cbLocal.isSelected = params.local

        cbAssemble.autoComplete(project, mutableListOf("assembleDebug","assembleRelease"))
        cbAssemble.selectedItem = params.assemble

        if (params.state == XItem.KEY_WAIT) {
            params.buildEnd = Runnable { refresh() }
        }
        cbKeep.addActionListener { params.keep = cbKeep.isSelected }
        //安装拖进来的安装包
        UiUtils.setTransfer(content) { files ->
            val apk = files.find { it.exists() && it.name.endsWith(".apk") }
            if (apk != null && Messages.showOkCancelDialog(project, "$apk", "Install", "Yes", "No", null) == Messages.OK) {
                UiUtils.tryInstall(project, null, "", paramHelper.install)
            }
        }
    }

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        item.select = curModule == module.name
        item.state = if (params.module == module.name) params.state else XItem.KEY_IDLE
        item.cbSelect.addItemListener { onSelect(item) }
        item.visible = paramHelper.locals.contains(module.name)
        return module.typeX().android()
    }

    override fun onTitleClickR(item: XItem, module: ModuleX?, c: JComponent, e: MouseEvent) {

    }

    private fun refresh() {
        isOKActionEnabled = params.state != XItem.KEY_WAIT && adapter.datas().find { it.select } != null
        val item = adapter.datas().find { it.title == params.module } ?: return
        item.state = params.state
    }

    override fun doOKAction() {
        val runModule = adapter.datas().find { it.select } ?: return
        super.doOKAction()
        params.module = runModule.title
        params.local = cbLocal.isSelected
        params.keep = cbKeep.isSelected
        params.state = XItem.KEY_WAIT
        params.assemble = cbAssemble.selectedItem?.toString() ?: "assembleDebug"
        action.tryToAssemble(project, params) { refresh() }
        refresh()
    }

    override fun moreActions(): List<PopOption<String>> {
        return paramHelper.getActions() + paramHelper.installOption()
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
    var result: String = ""
) {
    var state: String = ""
    var buildEnd: Runnable? = null
}
