package com.pqixing.intellij.actions

import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.labels.LinkLabel
import com.pqixing.XHelper
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.getSpList
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.XApp.putSpList
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.gradle.TaskPopParam
import com.pqixing.intellij.ui.autoComplete
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.uitils.UiUtils
import com.pqixing.intellij.uitils.UiUtils.realName
import com.pqixing.model.impl.ModuleX
import git4idea.repo.GitRepository
import java.awt.Point
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class XBuilderAction : XEventAction<XBuilderDialog>() {
    val FAST_PLACE = "NavBarToolbar"
    val FAST_PARAMS = mutableMapOf<Int, BuildParam?>()

    override fun update(e: AnActionEvent) {
        val p = e.project
        if (e.place != FAST_PLACE || p == null) super.update(e) else {
            val param = FAST_PARAMS[p.hashCode()]
            e.presentation.isVisible = param != null
            e.presentation.text = "Run :${param?.module}:${param?.task} ;   ${param?.toTipString()}"
            e.presentation.icon = AllIcons.Actions.Rerun
            e.presentation.isEnabled = param?.building == false
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val last = FAST_PARAMS[project.hashCode()]
        if (e.place != FAST_PLACE || last?.module?.isNotEmpty() != true) XBuilderDialog(this, e).show()
        else startBuild(project, FAST_PARAMS[project.hashCode()])
    }

    fun startBuild(project: Project, param: BuildParam?, finish: (success: Boolean, result: String?) -> Unit = { _, _ -> }) {
        val module = param?.module ?: return finish(false, null)


        val envs = mutableMapOf("include" to module, "local" to param.local.toString())

        if (param.task.startsWith("assemble")) {
            envs["assemble"] = module
        }

        if (param.local) {
            envs["include"] = "${module},${XHelper.readManifest(project.basePath!!)?.config?.include}"
        }

        param.building = true
        GradleRequest(listOf(":${param.module}:${param.task}"), envs, param.gradleParam).executeTasks(project) {
            val success = it.success
            val result = it.getDefaultOrNull()?.trim()?.takeIf { it.isNotEmpty() }

            if (!success) {
                XApp.notify(project, "Build Fail", result ?: "", type = NotificationType.WARNING)
            } else if (result?.endsWith(".apk") == true) XApp.invoke {
                UiUtils.tryInstall(project, null, result, param.install)
            }
            param.building = false
            finish(success, result)
        }
    }
}

class XBuilderDialog(val action: XBuilderAction, e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop
    private lateinit var cbAssemble: JComboBox<String>
    private lateinit var cbLocal: JCheckBox

    val curModule = e.getData(LangDataKeys.MODULE)?.realName()

    val paramHelper = TaskPopParam("builder", project, this)

    override fun getTitleStr(): String = "AEXBuilder"

    override fun initWidget() {
        cbLocal.isSelected = "BUILD_TASK_LOCAL".getSp("N", project) == "Y"

        val buildTasks = buildTasks()
        cbAssemble.autoComplete(project, buildTasks)
        cbAssemble.selectedItem = buildTasks.first()

        //安装拖进来的安装包
        UiUtils.setTransfer(center) { files ->
            val apk = files.find { it.exists() && it.name.endsWith(".apk") }
            if (apk != null && Messages.showOkCancelDialog(project, "$apk", "Install", "Yes", "No", null) == Messages.OK) {
                UiUtils.tryInstall(project, null, "", paramHelper.install)
            }
        }
    }

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        item.select = curModule == module.name
        item.visible = paramHelper.locals.contains(module.name)
        return module.typeX().android()
    }

    fun buildTasks(newTask: String? = null): List<String> {
        val tasks = "BUILD_TASK".getSpList("assembleDebug,assembleRelease", project).toMutableList()
        if (newTask != null) {
            tasks.remove(newTask)
            tasks.add(0, newTask)
            "BUILD_TASK".putSpList(tasks, project)
        }
        return tasks
    }

    override fun doOKAction() {
        val runModule = adapter.datas().filter { it.visible && it.select }.takeIf { it.isNotEmpty() } ?: return
        val task = cbAssemble.selectedItem?.toString()?.takeIf { it.isNotEmpty() } ?: return

        buildTasks(task)
        "BUILD_TASK_LOCAL".putSp(if (cbLocal.isSelected) "Y" else "N", project)

        val param = BuildParam("", task, cbLocal.isSelected, paramHelper.install, paramHelper.getGradleParams(), false)

        startBuild(runModule.first(), param)
    }

    fun startBuild(item: XItem, param: BuildParam) {

        item.state = XItem.KEY_WAIT
        param.module = item.title
        isOKActionEnabled = false
        action.startBuild(project, param) { s, r ->
            if (!s) {
                item.state = XItem.KEY_ERROR
                isOKActionEnabled = true
                return@startBuild
            }

            item.state = XItem.KEY_SUCCESS
            item.select = false

            val next = adapter.datas().find { it.select && it.visible }

            if (next != null) XApp.invoke {
                startBuild(next, param)
            } else {
                isOKActionEnabled = true
                XApp.notify(project, "Build Finish", "")
            }
        }
    }


    override fun onSelect(item: XItem?) {
        super.onSelect(item)
        isOKActionEnabled = adapter.datas().any { it.visible && it.select }
    }

    override fun createMenus(): List<JComponent?> {
        val hasAttach = action.FAST_PARAMS[project.hashCode()] != null
        val attach = createLinkText("toolbar", if (hasAttach) AllIcons.Actions.Selectall else ICON_UNCHECKED) { showAttachPop(it) }
        return listOf(attach) + super.createMenus()
    }

    private fun showAttachPop(linkText: LinkLabel<String>) {

        val param = action.FAST_PARAMS[project.hashCode()]
        val options = adapter.datas().filter { it.visible }.map {
            PopOption(it, it.title, it.content, it.title == param?.module) { attach ->
                if (attach) {
                    val task = cbAssemble.selectedItem?.toString()?.takeIf { it.isNotEmpty() } ?: buildTasks().first()
                    action.FAST_PARAMS[project.hashCode()] =
                        BuildParam(it.title, task, cbLocal.isSelected, paramHelper.install, paramHelper.getGradleParams(), false)
                } else {
                    action.FAST_PARAMS.remove(project.hashCode())
                }
                linkText.icon = if (attach) AllIcons.Actions.Selectall else ICON_UNCHECKED
            }
        }

        XListPopupImpl(project, "Attach To Toolbar", options.sortedBy { !it.selected }).show(
            RelativePoint(morePanel, Point(0, morePanel.height + 10))
        )
    }

    override fun moreActions(): List<PopOption<String>> {
        return paramHelper.getActions() + paramHelper.installOption()
    }
}

class BuildParam(
    var module: String,
    var task: String = "assembleDebug",
    var local: Boolean = true,
    var install: String = "-r -t",
    var gradleParam: String = "",
    var building: Boolean = false
) {
    fun toTipString(): String {
        return "local=$local, install='$install', param='$gradleParam', building=$building"
    }
}