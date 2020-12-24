package com.pqixing.intellij.common

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.pqixing.XHelper

class XGroup : DefaultActionGroup() {

    companion object {
        fun isExProject(project: Project?): Boolean = XHelper.ideImportFile(project?.basePath?:"").exists()

        fun isDebug(project: Project?) = isExProject(project)

        fun isCreator(project: Project?) = false
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isVisible = isExProject(e.project)
        e.presentation.isEnabled = true
//        (ActionManager.getInstance().getAction("DeviceAndSnapshotComboBox") as DeviceAndSnapshotComboBoxAction).getSelectedDevice(e.project!!)
        //启动后，尝试打开socket连接，接收gradle插件的通知
//        val oldUpdate = ActionManager.getInstance().getAction("Vcs.UpdateProject")
//        val field = (AbstractCommonUpdateAction::class.java).getDeclaredField("myScopeInfo").also { it.isAccessible = true }
//        val myScopeInfo = field.let { it.get(oldUpdate) }
//        if (myScopeInfo !is ScopeInfoProxy) field.set(oldUpdate, ScopeInfoProxy(myScopeInfo as ScopeInfo))

//        Vcs.UpdateProject
    }
}
