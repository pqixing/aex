package com.pqixing.intellij.ui

import com.pqixing.intellij.uitils.UiUtils
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JComboBox
import javax.swing.JTextField
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener


class JBFilterWrapper(val combo: JComboBox<String>, val autoShow: Boolean = true, val onItemSelect: (item: String) -> Unit = {}) {
    val dataSet: MutableList<String> = mutableListOf()
    var editorField = combo.editor.editorComponent as JTextField

    var idle = true

    init {
        combo.isEditable = true
        combo.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent?) {
            }

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                if (idle) {
                    val key = combo.selectedItem?.toString() ?: ""
                    onItemSelect(key)
                    refresh(false, key, false)
                }
            }

            override fun popupMenuCanceled(e: PopupMenuEvent?) {
            }
        })
        editorField.addKeyListener(object : KeyAdapter() {

            override fun keyReleased(e: KeyEvent?) {
                val keyCode = e?.keyCode ?: return
                if (keyCode == KeyEvent.VK_ENTER || keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_UP) return
                if (keyCode == KeyEvent.VK_ESCAPE) {
                    combo.hidePopup()
                    return
                }
                val key = editorField.text?.toString()
                refresh(autoShow, key, true)
            }
        })
    }

    fun setDatas(data: Collection<String>) {
        dataSet.clear()
        dataSet.addAll(data.distinctBy { it }.filter { it.isNotEmpty() })
        refresh(false, dataSet.firstOrNull(), false)
    }

    /**
     * @param filter 是否需要过滤菜单,如果不过滤,添加全部
     */
    fun refresh(show: Boolean, key: String?, filter: Boolean) {
        idle = false
        val showing = combo.isPopupVisible
        combo.hidePopup()
        combo.removeAllItems()
        val searchKey = key?.trim() ?: ""
        val filters = dataSet.filter { !filter || UiUtils.match(searchKey, listOf(it)) }
        if (filters.isEmpty()) {//如果为空,添加当前key到弹窗,防止弹窗是空的,丑
            combo.addItem(searchKey)
        }
        filters.forEach { combo.addItem(it) }
        combo.selectedItem = searchKey
        if (show || showing) combo.showPopup()
        idle = true
    }
}