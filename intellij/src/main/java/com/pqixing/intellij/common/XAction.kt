package com.pqixing.intellij.common

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.pqixing.intellij.XApp
import com.pqixing.intellij.ui.weight.XEventDialog
import java.lang.reflect.ParameterizedType

abstract class XEventAction<T : XEventDialog> : XAction() {
    override fun actionPerformed(e: AnActionEvent) {
        ((javaClass.genericSuperclass as? ParameterizedType)?.actualTypeArguments?.firstOrNull() as? Class<XEventDialog>)?.getConstructor(
            AnActionEvent::class.java
        )?.newInstance(e)?.show()
    }
}

abstract class XAction : AnAction() {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = XApp.isExProject(e.project, false)
    }

    override fun actionPerformed(e: AnActionEvent) {

    }
}