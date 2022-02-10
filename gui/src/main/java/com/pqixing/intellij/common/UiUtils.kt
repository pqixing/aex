package com.pqixing.intellij.common

import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileListener
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.compat.AndroidCompat
import com.pqixing.intellij.device.DeviceWrapper
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.util.*
import javax.swing.JComponent
import javax.swing.TransferHandler

object UiUtils : VirtualFileListener {

    fun setTransfer(component: JComponent, block: (files: List<File>) -> Unit) {
        component.transferHandler = object : TransferHandler() {
            override fun importData(p0: JComponent?, t: Transferable): Boolean {
                try {
                    val o = t.getTransferData(DataFlavor.javaFileListFlavor)
                    block(o as List<File>)
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }

            override fun canImport(p0: JComponent?, flavors: Array<out DataFlavor>?): Boolean {
                return flavors?.find { DataFlavor.javaFileListFlavor.equals(it) } != null
            }
        }
    }

    fun match(searchKey: String, matchs: List<String>): Boolean {
        if (searchKey.trim().isEmpty()) return true
        val key = searchKey.trim().toLowerCase(Locale.CHINA)
        for (m in matchs) {
            var k = 0
            var l = -1
            val line = m.toLowerCase(Locale.CHINA)
            while (++l < line.length) if (key[k] == line[l] && ++k == key.length) return true
        }
        return false
    }


    /**
     * 安装并打开应用
     */
    fun tryInstall(project: Project, path: String, param: String, list: List<DeviceWrapper>? = null) {
        //获取当前的设备
        val devices = list ?: AndroidCompat.getSelectDevices(project) ?: return XApp.notify(
            project, "Device Not Found", path,
            actions = listOf(XNotifyAction("Copy Path") { XApp.copy(path) }, XNotifyAction("Retry") { tryInstall(project, path, param, list) })
        )

        XApp.runAsyn { indicator ->
            val fails = devices.filter {
                indicator.text = "Install to ${it.name} : $path"
                !it.installAndLaunch(path, param)
            }
            if (fails.isNotEmpty()) {
                XApp.notify(
                    project, "Install Fail", "${fails.map { it.name }}", NotificationType.WARNING,
                    listOf(XNotifyAction("ReTry") { tryInstall(project, path, param, fails) })
                )
            }
        }
    }


    fun base64Encode(source: String) = String(Base64.getEncoder().encode(source.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
    fun base64Decode(source: String) = String(Base64.getDecoder().decode(source.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)

    fun Module?.realName(): String = this?.name?.let { it.substringAfterLast(".").substringAfterLast(":") } ?: ""
}



