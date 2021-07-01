package com.pqixing.intellij.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.SearchTextField
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.awt.RelativePoint
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getOrElse
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.gradle.GradleTaskListener
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.pop.XListPopupImpl
import com.pqixing.intellij.ui.weight.XEventDialog
import java.awt.Point
import javax.swing.*

class XDebugDialog(e: AnActionEvent) : XEventDialog(e) {
    protected lateinit var centerPanal: JPanel
    protected lateinit var panelAction: JPanel
    protected lateinit var btnAction: JButton
    lateinit var tvCustomParam: TextFieldWithAutoCompletion<String>
    override fun createCenterPanel(): JComponent = centerPanal

    companion object {
        private const val SHOW_POP = "debug::pop"
        private const val SHOW_PANEL = "debug::panel"

        /**
         * 处理调试模式命令
         */
        fun handleDebugAction(cmd: String, project: Project, e: AnActionEvent, tvSearch: SearchTextField) {
            var handle = true
            when (cmd) {
                SHOW_POP -> showDebugPop(project, e, tvSearch)
                SHOW_PANEL -> XDebugDialog(e).show()
                else -> {
                    handle = false
                }
            }
            if (handle) {
                tvSearch.text = ""
            }
        }

        private fun showDebugPop(project: Project, e: AnActionEvent, c: SearchTextField) {
            val optins = listOf(
                PopOption("", "build_output", "output log to build panel view", GradleTaskListener.output) { s -> GradleTaskListener.output = s },
                PopOption(
                    "",
                    "build_activate",
                    "auto activite build panel on start task",
                    GradleTaskListener.activate
                ) { s -> GradleTaskListener.activate = s },
            )

            //右键点击,隐藏
            val itemClick = { left: Boolean, pop: XListPopupImpl<*>, o: PopOption<*> ->
                if (!left) {
                    pop.dispose()
                }
            }

            XListPopupImpl(project, "title", optins, itemClick).show(RelativePoint(c, Point(180 - c.x, 23)))
        }
    }

    override fun init() {
        super.init()
        title = "Aex Debug Item"
        initActionDebug()
        isOKActionEnabled = false
    }

    override fun getOKAction(): Action {
        return super.getOKAction()
    }

    private fun initActionDebug() {
        val am = ActionManager.getInstance()
        val key = "LAST_RUN_ACTION"
        tvCustomParam = TextFieldWithAutoCompletion.create(project, am.getActionIds("").toList(), true, key.getSp("", project).toString())
        tvCustomParam.toolTipText = "input action id to run"
        panelAction.add(tvCustomParam)
        btnAction.addActionListener {
            val actionId = tvCustomParam.text.trim()
            if (actionId.isEmpty()) return@addActionListener
            val action = kotlin.runCatching { am.getAction(actionId) }.getOrElse {
                Messages.showMessageDialog(project, it.message, "Not Action : $actionId", null)
                null
            }
            XApp.log("Prepare : $actionId : ${action?.templateText} : ${action?.javaClass?.name} ")
            if (action == null) return@addActionListener
            key.putSp(actionId, project)
            action.actionPerformed(e)
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(cancelAction)
    }

    override fun createMenus(): List<JComponent?> {
        val keep = JCheckBox("Menu", "XDebugAction".getSp("N", project).toString() == "Y")
        keep.addChangeListener { "XDebugAction".putSp(keep.isSelected.getOrElse("Y", "N"), project) }
        return listOf(keep)
    }
}
