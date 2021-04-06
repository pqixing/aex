package com.pqixing.intellij.ui

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.ui.TextFieldWithAutoCompletion
import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.ComboBoxEditor
import javax.swing.ComboBoxModel
import javax.swing.JComboBox
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class AutoCompleteEditor(
    myProject: Project, val myComboBox: JComboBox<String>, items: MutableList<String> = mutableListOf(), val onSelect: (item: String) -> Unit = {}
) : ComboBoxEditor, DocumentListener {
    private val editor = TextFieldWithAutoCompletion.create(myProject, items, true, myComboBox.selectedItem?.toString() ?: "")
    private val autoPopupController = AutoPopupController.getInstance(myProject)
    private var selectingItem = false

    init {
        this.editor.toolTipText = myComboBox.toolTipText
        myComboBox.editor = this
        myComboBox.isEditable = true
        setVariants(items)
        subscribeForModelChange(myComboBox.model)
        editor.addDocumentListener(this)
    }

    override fun documentChanged(event: DocumentEvent) {
        super.documentChanged(event)
        if (selectingItem) return

        myComboBox.selectedItem = editor.text
        autoPopupController.scheduleAutoPopup(editor.editor!!, CompletionType.BASIC) { true }
    }

    fun setVariants(variants: Collection<String>) {
        myComboBox.removeAllItems()
        variants.forEach { myComboBox.addItem(it) }
        editor.setVariants(variants)
    }

    override fun addActionListener(l: ActionListener) {}
    override fun removeActionListener(l: ActionListener) {}
    override fun getEditorComponent(): Component {
        return editor
    }

    override fun getItem(): Any? {
        return editor?.text
    }

    override fun selectAll() {
        editor.selectAll()
    }

    override fun setItem(obj: Any?) {
        selectingItem = true
        val newText = obj?.toString() ?: ""
        onSelect(newText)
        editor.text = newText
//        XApp.log("setItem -> $newText -> ${myComboBox.selectedItem}")
        selectingItem = false
    }


    private fun subscribeForModelChange(model: ComboBoxModel<String>) {
        model.addListDataListener(object : ListDataListener {
            override fun intervalAdded(e: ListDataEvent?) {
                editor.setVariants(collectItems())
            }

            override fun intervalRemoved(e: ListDataEvent?) {
                editor.setVariants(collectItems())
            }

            override fun contentsChanged(e: ListDataEvent?) {
                editor.setVariants(collectItems())
            }

            private fun collectItems(): List<String> {
                val items = mutableListOf<String>()
                if (model.size != 0) {
                    for (i in 0 until model.size) {
                        items += model.getElementAt(i)
                    }
                }
                return items
            }
        })
    }
}

fun JComboBox<String>.autoComplete(project: Project, items: List<String>, onSelect: (item: String) -> Unit = {}): AutoCompleteEditor {
    return AutoCompleteEditor(project, this, items.distinctBy { it }.toMutableList(), onSelect)
}
