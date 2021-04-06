package com.pqixing.intellij.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.ui.TextFieldWithAutoCompletion
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XApp.getOrElse
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.ui.weight.XEventDialog
import javax.swing.*

class XDebugAction : XEventAction<XDebugDialog>() {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = "XDebugAction".getSp("N", e.project).toString() == "Y"
    }
}

class XDebugDialog(e: AnActionEvent) : XEventDialog(e) {
    protected lateinit var centerPanal: JPanel
    protected lateinit var panelAction: JPanel
    protected lateinit var btnAction: JButton
    lateinit var tvCustomParam: TextFieldWithAutoCompletion<String>
    override fun createCenterPanel(): JComponent = centerPanal

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
