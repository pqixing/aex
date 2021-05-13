package com.pqixing.intellij.ui.weight

import java.awt.CheckboxMenuItem
import java.awt.Color
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
    lateinit var tvTitle: JCheckBox
    lateinit var tvContent: JLabel
    lateinit var tvTag: JLabel

    var left: (c: JComponent, e: MouseEvent) -> Unit = { c, e -> if (c != tvTitle) tvTitle.isSelected = !tvTitle.isSelected }
    var right: (c: JComponent, e: MouseEvent) -> Unit = { c, e -> }
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
        get() = tvTitle.isSelected
        set(value) {
            tvTitle.isSelected = value
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

fun JComponent.addMouseClickL(left: (c: JComponent, e: MouseEvent) -> Unit) = addMouseClick(left, { _, _ -> })
fun JComponent.addMouseClickR(right: (c: JComponent, e: MouseEvent) -> Unit) = addMouseClick({ _, _ -> }, right)
fun JComponent.addMouseClick(left: (c: JComponent, e: MouseEvent) -> Unit, right: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> }) =
    MouseHandle(left, right).attach(this)

fun JComponent.addMouseClick(left: (c: JComponent, e: MouseEvent) -> Unit) = MouseHandle(left, left).attach(this)

class MouseHandle(
    val left: (c: JComponent, e: MouseEvent) -> Unit,
    val right: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> }
) : MouseAdapter() {
    var otherClick: (c: JComponent, e: MouseEvent) -> Unit = { _, _ -> }

    fun attach(component: JComponent): MouseHandle {
        component.addMouseListener(this)
        return this
    }

    override fun mouseClicked(e: MouseEvent?) {
        super.mouseClicked(e)
        e ?: return
        val component = e.source as? JComponent ?: return
        when (e.button) {
            MouseEvent.BUTTON3 -> right(component, e)
            MouseEvent.BUTTON1 -> left(component, e)
            else -> otherClick(component, e)
        }
    }
}

class MyMenuItem<T>(label: String, val data: T? = null, val select: Boolean = false, click: (item: MyMenuItem<T>) -> Unit) :
    CheckboxMenuItem(label, select) {
    init {
        addItemListener { click(this) }
    }

    override fun toString(): String {
        return label
    }
}
