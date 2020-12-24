package com.pqixing.intellij.ui.weight

import com.intellij.openapi.project.Project
import javax.swing.JComponent

class MultiSelectDialog(project: Project?, datas: List<String>, selects: Set<String> = emptySet(), val onSelect: (selects: List<String>?) -> Unit = {}) : XDialog(project) {
    init {
        adapter.set(datas.map {
            XItem().also { item ->
                item.select = selects.contains(it)
                item.title = it
                item.right = { _, _ -> }
            }
        })
        isModal = true
    }

    override fun getTitleStr(): String = "Select"

    override fun doOKAction() {
        super.doOKAction()
        onSelect(adapter.datas().filter { it.select }.map { it.title })
    }

    override fun doCancelAction() {
        super.doCancelAction()
        onSelect(null)
    }

    override fun createDoNotAskCheckbox(): JComponent? = null
}