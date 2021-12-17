package com.pqixing.intellij.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.pqixing.intellij.XApp

class XGroup : DefaultActionGroup() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        e.presentation.isEnabled = true
        e.presentation.isVisible = XApp.isExProject(e.project, true)
        e.presentation.icon = if (XApp.isRepoUpdate(e.project, true)) AllIcons.Plugins.Downloads else null

//        (ActionManager.getInstance().getAction("DeviceAndSnapshotComboBox") as DeviceAndSnapshotComboBoxAction).getSelectedDevice(e.project!!)
        //启动后，尝试打开socket连接，接收gradle插件的通知
//        val oldUpdate = ActionManager.getInstance().getAction("Vcs.UpdateProject")
//        val field = (AbstractCommonUpdateAction::class.java).getDeclaredField("myScopeInfo").also { it.isAccessible = true }
//        val myScopeInfo = field.let { it.get(oldUpdate) }
//        if (myScopeInfo !is ScopeInfoProxy) field.set(oldUpdate,a ScopeInfoProxy(myScopeInfo as ScopeInfo))

//        Vcs.UpdateProject
    }
}

