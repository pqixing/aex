package com.pqixing.intellij.ui.adapter

import com.intellij.openapi.ui.VerticalFlowLayout
import com.pqixing.intellij.ui.weight.XItem
import java.awt.Rectangle
import javax.swing.JPanel

class XBaseAdapter(val content: JPanel) : VerticalFlowLayout() {
    private val datas = mutableListOf<XItem>()

    init {
        content.layout = this
    }

    fun clear() {
        datas.clear()
        content.removeAll()
        notifyUi()
    }

    fun add(datas: List<XItem>) {
        this.datas += datas
        datas.forEach { content.add(it.jItemRoot) }
        notifyUi()
    }

    fun set(datas: List<XItem>) {
        clear()
        add(datas)
        notifyUi()
    }

    fun remove(datas: List<XItem>) {
        this.datas.removeAll(datas)
        datas.forEach { content.remove(it.jItemRoot) }
        notifyUi()
    }

    fun notifyUi() {
        content.parent?.let { parent ->
            val r: Rectangle = content.bounds
            parent.repaint(r.x, r.y, r.width, r.height)
        }
        content.revalidate()
    }

    fun datas() = datas.toList()

    fun getSize(): Int = datas.size
}

