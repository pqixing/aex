package com.pqixing.intellij.ui.pop

import com.intellij.icons.AllIcons
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.pqixing.intellij.ui.weight.XDialog
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class ListPopCellRender<T> : ListCellRenderer<PopOption<T>> {

    private val iconLabel = JLabel()
    private val optionLabel = JLabel()
    private val optionDescriptionLabel = JLabel()

    private val panel = createPanel()

    override fun getListCellRendererComponent(
        pop: JList<out PopOption<T>>?,
        value: PopOption<T>,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        val optionSelected = value.selected
        val optionSelectable = value.selectable

        panel.apply {
            background = UIUtil.getListBackground(isSelected, true)
            border = if (isSelected && isEnabled) border else null
        }

        iconLabel.icon = if (optionSelected) AllIcons.Actions.Checked else XDialog.ICON_UNCHECKED

        optionLabel.apply {
            text = value.title
            foreground = when {
                isSelected -> pop?.selectionForeground
                optionSelectable -> pop?.foreground
                else -> JBUI.CurrentTheme.Label.disabledForeground()
            }
        }

        optionDescriptionLabel.apply {
            text = value.desc
            foreground = if (isSelected)
                pop?.selectionForeground
            else
                JBUI.CurrentTheme.Label.disabledForeground()
        }

        return panel
    }

    private fun createPanel() = JPanel().apply {
        layout = FlowLayout().apply { alignment = FlowLayout.LEFT }
        add(iconLabel)
        add(optionLabel)
        add(optionDescriptionLabel)
    }
}

