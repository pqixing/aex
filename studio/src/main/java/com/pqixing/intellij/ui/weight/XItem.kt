package com.pqixing.intellij.ui.weight

import com.pqixing.intellij.XApp
import java.awt.CheckboxMenuItem
import java.awt.Color
import java.awt.Point
import java.awt.PopupMenu
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class XItem {
    companion object {
        const val KEY_SUCCESS = "√ "
        const val KEY_ERROR = "X "
        const val KEY_WAIT = "..."
        const val KEY_IDLE = ""
        val wait = Color(236, 117, 0)
        val error = Color(236, 0, 0)
        val success = Color(0, 187, 18)
    }

    lateinit var jItemRoot: JPanel
    lateinit var cbSelect: JCheckBox
    lateinit var tvTitle: JLabel
    lateinit var tvContent: JLabel
    lateinit var tvTag: JLabel

    var popMenu: List<MyMenuItem<*>>? = null
    var left: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> cbSelect.isSelected = !cbSelect.isSelected }
    var right: (c: JComponent, e: MouseEvent) -> Unit = { c, e ->
        val point = Point(e.x, e.y)
        if (popMenu != null) {
            c.showPop(popMenu!!, point)
        } else {
            var label = c.getComponentAt(e.x, e.y)
            if (label is JPanel) label = label.getComponentAt(e.x, e.y)
            if (label is JLabel) {
                val m = MyMenuItem<String>(label.text, null, false) { XApp.copy(it.label) }
                c.showPop(popMenu ?: listOf(m), Point(e.x, e.y))
            }
        }
    }
    var state: String = KEY_IDLE
        set(value) {
            field = value
            val foreground = when (value) {
                KEY_SUCCESS -> success
                KEY_ERROR -> error
                KEY_WAIT -> wait
                else -> jItemRoot.foreground
            }
            tvTitle.foreground = foreground
            tvTag.foreground = foreground
            tvContent.foreground = foreground
        }

    val normal = tvTag.foreground

    var title: String
        get() = tvTitle.text
        set(value) {
            tvTitle.text = value
        }

    var tag: String
        get() = tvTag.text.trim()
        set(value) {
            tvTag.text = value.trim() + "   "
        }

    var content: String
        get() = tvContent.text
        set(value) {
            tvContent.text = value
        }

    var select: Boolean
        get() = cbSelect.isSelected
        set(value) {
            cbSelect.isSelected = value
        }
    var selectAble: Boolean
        get() = cbSelect.isVisible
        set(value) {
            cbSelect.isVisible = value
        }

    var visible: Boolean
        get() = jItemRoot.isVisible
        set(value) {
            jItemRoot.isVisible = value
        }

    val params = mutableMapOf<String, Any?>()
    fun <T> get(key: String): T? {
        return params[key] as? T
    }

    init {
        jItemRoot.addMouseClick({ c, e -> left(c, e) }, { c, e -> right(c, e) })
    }
}

fun JComponent.showPop(menu: List<String>, point: Point, click: (item: MyMenuItem<String>) -> Unit) = showPop(menu.map { MyMenuItem<String>(it, it, false, click) }, point)

fun JComponent.showPop(menu: List<MyMenuItem<*>>, point: Point) {
    if (menu.isEmpty()) return
    val pop = PopupMenu()
    this.add(pop)
    menu.forEach { pop.add(it) }
    pop.show(this, point.x, point.y)
}

fun JComponent.addMouseClickL(left: (c: JComponent, e: MouseEvent) -> Unit) = addMouseClick(left, { _, _ -> })
fun JComponent.addMouseClickR(right: (c: JComponent, e: MouseEvent) -> Unit) = addMouseClick({ _, _ -> }, right)
fun JComponent.addMouseClick(left: (c: JComponent, e: MouseEvent) -> Unit, right: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> }) = MouseHandle(this, left, right)
fun JComponent.addMouseClick(left: (c: JComponent, e: MouseEvent) -> Unit) = MouseHandle(this, left, left)

class MouseHandle(val component: JComponent, val left: (c: JComponent, e: MouseEvent) -> Unit, val right: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> }) : MouseAdapter() {
    var otherClick: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> }

    init {
        component.addMouseListener(this)
    }

    override fun mouseClicked(e: MouseEvent?) {
        super.mouseClicked(e)
        e ?: return
        when (e.button) {
            MouseEvent.BUTTON3 -> right(component, e)
            MouseEvent.BUTTON1 -> left(component, e)
            else -> otherClick(component, e)
        }
    }
}

class MyMenuItem<T>(label: String, val data: T? = null, val select: Boolean = false, click: (item: MyMenuItem<T>) -> Unit) : CheckboxMenuItem(label, select) {
    init {
        addItemListener { click(this) }
    }
}
