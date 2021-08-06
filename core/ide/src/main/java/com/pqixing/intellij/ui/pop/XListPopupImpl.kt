package com.pqixing.intellij.ui.pop

import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.popup.list.ListPopupImpl
import com.pqixing.intellij.ui.weight.XItem
import java.awt.Point
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.ListCellRenderer

class XListPopupImpl<T>(
    project: Project,
    titile: String,
    val menus: List<PopOption<T>>,
    val onItemSelect: (rightButton: Boolean, pop: XListPopupImpl<T>, optins: PopOption<T>) -> Unit = { _, pop, _ -> pop.dispose() }
) :
    ListPopupImpl(project, ListPopupStepImpl(titile, menus)) {
    init {
        setMaxRowCount(11)
    }

    val mouseAdapter = object : MouseAdapter() {

        override fun mouseReleased(e: MouseEvent?) {
            if (e?.button != MouseEvent.BUTTON3) return
            onSelect(false)
        }
    }

    override fun getListElementRenderer(): ListCellRenderer<PopOption<T>> = ListPopCellRender<T>()

    fun onSelect(left: Boolean) {
        val select = selectedValues.firstOrNull() as? PopOption<T> ?: return
        if (left && select.selectable) {
            select.selected = !select.selected
            onItemSelect(left, this, select)
            select.onSelectChange(select.selected)
        } else onItemSelect(left, this, select)

        list.repaint()
    }

    override fun beforeShow(): Boolean {
        list?.addMouseListener(mouseAdapter)
        return super.beforeShow()
    }

    override fun dispose() {
        list?.removeMouseListener(mouseAdapter)
        super.dispose()
    }

    override fun handleSelect(handleFinalChoices: Boolean) {
        if (handleFinalChoices) onSelect(true)
    }

    override fun handleSelect(handleFinalChoices: Boolean, e: InputEvent?) {
        if (handleFinalChoices) onSelect(true)
    }

}

fun MouseEvent.relativePoint(c: JComponent, x1: Int = 0, y1: Int = 0): RelativePoint {
    return RelativePoint(c, Point(x - c.x + x1, y - c.y + y1))
}

fun JComponent.showItemPopup(project: Project, menu: List<XItem>, titile: String = "") {
    val optins = menu.map { PopOption(it, it.title, it.content, it.select) { select -> it.select = select } }
    showPopup(project, optins, titile)
}

fun <T> JComponent.showPopup(project: Project, optins: List<PopOption<T>>, titile: String = "", point: Point? = null) {
    val relativePoint = RelativePoint(this, point ?: Point(0, 10))
    XListPopupImpl(project, titile, optins).show(relativePoint)
}
