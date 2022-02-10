package com.pqixing.intellij.device

import com.pqixing.intellij.compat.AndroidCompat

/**
 * 包装一层，防止兼容问题
 */
class DeviceWrapper(val name: String, val device: Any) {
    fun installAndLaunch(path: String, param: String): Boolean {
        return AndroidCompat.install(this, path, param)
    }
}