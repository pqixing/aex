package com.pqixing.profm.utils

import com.pqixing.profm.setting.XSetting
import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.lang.ref.WeakReference


fun Project.setting() = AEX.cache[this.gradle.hashCode()]?.get()!!
fun <T : Plugin<*>> Project.plugin(type: Class<T>): T? {
    return this.setting().plugins.get(this)?.find { it.javaClass == type } as? T
}

fun Project.register(plugin: Plugin<*>) {
    val setting = this.setting() ?: return
    val setOf = setting.plugins[this] ?: mutableSetOf()
    setOf.add(plugin)
    setting.plugins[this] = setOf
}

object AEX {
    val cache = mutableMapOf<Int, WeakReference<XSetting>>()
    fun register(x: XSetting) {
        val key = x.gradle.hashCode()
        cache[key] = WeakReference(x)
    }

    fun unregister(x: XSetting) {
        cache.remove(x.gradle.hashCode())
    }

    fun <T> runClosure(delegate: T, closure: Closure<*>?): T {
        if (closure == null) return delegate
        if (delegate != null) {
            closure.delegate = delegate
            closure.resolveStrategy = Closure.DELEGATE_FIRST
        }
        closure.call(delegate)
        return delegate
    }
}