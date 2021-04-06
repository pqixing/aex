package com.pqixing.intellij.ui.pop

class PopOption<T>(
    val option: T?,
    val title: String,
    val desc: String,
    var selected: Boolean = false,
    var selectable: Boolean = true,
    var onSelectChange: (select: Boolean) -> Unit = {}
) {
    override fun toString(): String {
        return "$option,$title,$desc"
    }
}
