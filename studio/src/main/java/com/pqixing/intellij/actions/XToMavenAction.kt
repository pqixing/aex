package com.pqixing.intellij.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.ui.PopupMenuListenerAdapter
import com.intellij.ui.awt.RelativePoint
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.gradle.TaskPopParam
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.intellij.ui.weight.XModuleDialog
import com.pqixing.intellij.uitils.UiUtils.realName
import com.pqixing.model.impl.ModuleX
import git4idea.repo.GitRepository
import java.awt.Point
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.PopupMenuEvent


open class XToMavenAction : XEventAction<XToMavenDialog>()

class XToMavenDialog(e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    val KEY_CONDITION = "KEY_CONDITION"
    private lateinit var cbCondition: JComboBox<String>
    override fun createNorthPanel(): JComponent? = pTop
    val curModule = e.getData(LangDataKeys.MODULE)?.realName()
    private val helper = TaskPopParam("maven", project, this)
    val conditions = listOf(
        PopOption("2", "clean", "      allow code not clean "),
        PopOption("1", "repeat", "    allow code not change "),
        PopOption("3", "branch", "   allow branch different ")
    )

    override fun getTitleStr(): String = "ToMaven : ${manifest.branch}"

    override fun initWidget() {
        super.initWidget()
        initConditions()
    }

    private fun initConditions() {
        val onSelectChange = { s: Boolean ->
            var newItem = conditions.filter { it.selected }.joinToString(" , ") { it.title }
            if(newItem.isEmpty()){
                newItem = "   ------  "
            }
            cbCondition.removeAllItems()
            cbCondition.addItem(newItem)
            cbCondition.selectedItem = newItem
        }

        val condition = KEY_CONDITION.getSp(manifest.config.ignore, project).toString()
        conditions.forEach {
            it.selected = condition.contains(it.option!!)
            it.onSelectChange = onSelectChange
        }
        cbCondition.addPopupMenuListener(object : PopupMenuListenerAdapter() {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
                cbCondition.hidePopup()
                XListPopupImpl(project, "", conditions) { _, _, _ -> }.show(RelativePoint(cbCondition, Point(3, cbCondition.height)))
            }
        })
        onSelectChange(false)
    }

    override fun onSelect(item: XItem?) {
        super.onSelect(item)
        isOKActionEnabled = adapter.datas().any { it.visible && it.select }
    }

    override fun loadList(): List<XItem> {
        return super.loadList().reversed()//反转
    }

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        item.select = curModule == module.name
//        item.content = "${module.maven().group}:${module.version}"
        item.visible = helper.locals.contains(module.name)
        return super.onItemUpdate(item, module, repo)
    }

    /**
     * 开始上传代码
     */
    private fun startToMaven(item: XItem, ignore: String, mapping: String, gradleParam: Map<String, String>, activate: Boolean = false) {
        isOKActionEnabled = false
        item.state = XItem.KEY_WAIT
        GradleRequest(
            listOf(":${item.title}:ToMaven"),
            mapOf("include" to item.title, "local" to "false", "ignore" to ignore, "mapping" to mapping) + gradleParam,
            activate = activate
        ).runGradle(project) { result ->
            if (!result.success) {
                item.state = XItem.KEY_ERROR
                isOKActionEnabled = true
                val msg = result.getDefaultOrNull() ?: ""
                XApp.notify(
                    project,
                    "ToMaven Fail",
                    msg,
                    NotificationType.WARNING,
                    listOf(XNotifyAction("Retry") { startToMaven(item, ignore, mapping, gradleParam) })
                )
                return@runGradle
            }
            item.state = XItem.KEY_SUCCESS
            item.content = "${item.content.split(":").firstOrNull()}:${result.getDefaultOrNull()?.substringAfterLast(":")}"
            item.select = false

            val next = adapter.datas().find { it.select && it.visible }
            if (next != null) return@runGradle XApp.invoke { startToMaven(next, ignore, mapping, gradleParam) }

            isOKActionEnabled = true
            XApp.notify(project, "ToMaven Finish", "")
            return@runGradle
        }
    }

    override fun moreActions(): List<PopOption<String>> {
        return helper.getActions()
    }

    override fun doOKAction() {
        val next = adapter.datas().find { it.select && it.visible } ?: return
        isOKActionEnabled = false
        val condition = conditions.filter { it.selected }.joinToString(",") { it.option + "" }
        KEY_CONDITION.putSp(condition, project)
        startToMaven(next, condition, helper.mapping, helper.getGradleParams(), true)
    }
}


