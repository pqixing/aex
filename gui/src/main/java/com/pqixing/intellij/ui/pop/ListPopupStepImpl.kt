package com.pqixing.intellij.ui.pop

import com.intellij.openapi.ui.popup.util.BaseListPopupStep

class ListPopupStepImpl<T>(title: String = "", menu: List<T>) : BaseListPopupStep<T>(title, menu) {
    override fun isSpeedSearchEnabled(): Boolean {
        return true
    }

    override fun getIndexedString(value: T): String {
        return super.getIndexedString(value)
    }
}