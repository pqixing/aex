package com.pqixing.intellij.uitils

import com.android.ddmlib.IDevice
import com.android.tools.apk.analyzer.AaptInvoker
import com.android.tools.apk.analyzer.AndroidApplicationInfo
import com.android.tools.idea.explorer.adbimpl.AdbShellCommandsUtil
import com.android.tools.idea.run.deployment.DeviceGet
import com.android.tools.idea.sdk.AndroidSdks
import com.intellij.notification.NotificationType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileListener
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XNotifyAction
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.util.*
import javax.swing.JComponent
import javax.swing.TransferHandler

object UiUtils : VirtualFileListener {

    fun getSelectDevices(project: Project) = DeviceGet.getSelectDevices(project)

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

    fun installApk(device: IDevice, path: String, params: String): Boolean = try {
        val newPath = "/data/local/tmp/${path.hashCode()}.apk"
        device.pushFile(path, newPath)
        val output = AdbShellCommandsUtil.executeCommand(device, "pm  install $params  $newPath").output
        XApp.log("installApk to ${device.name} : $path -> ${output.joinToString(";")}")
        val success = output.findLast { it.trim().isNotEmpty() }?.contains("Success") == true
        if (success) {
            launchApk(path, device)
            adbShellCommon(device, "rm -f $newPath", false)
        } else adbShellCommon(device, "mv $newPath /sdcard/${path.substringAfterLast("/")}", false)
        success
    } catch (e: Exception) {
        XApp.log(e.toString())
        false
    }

    fun adbShellCommon(device: IDevice, cmd: String, firstLine: Boolean): String = try {
        val output = AdbShellCommandsUtil.executeCommand(device, cmd).output
        if (firstLine || output.size == 1) output[0] else output.toString()
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }

    fun getAppInfoFromApk(fileApk: File): AndroidApplicationInfo? = try {
        val invoker = AaptInvoker(AndroidSdks.getInstance().tryToChooseSdkHandler(), LogWrap())
        val xmlTree = invoker.getXmlTree(fileApk, "AndroidManifest.xml")
        AndroidApplicationInfo.parse(xmlTree)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    /**
     * 安装并打开应用
     */
    fun tryInstall(project: Project, ds: List<IDevice>?, path: String, param: String) {
        //获取当前的设备
        val devices = ds ?: getSelectDevices(project)?.filter { it.isOnline }?.takeIf { it.isNotEmpty() }
        ?: return XApp.notify(project, "Device Not Found", path, actions = listOf(XNotifyAction("Copy Path") { XApp.copy(path) }, XNotifyAction("Retry") { tryInstall(project, ds, path, param) }))
        XApp.runAsyn { indicator ->
            val fails = devices.filter { device ->
                indicator.text = "Install to ${device.name} : $path"
                !installApk(device, path, param)
            }
            if (fails.isNotEmpty()) {
                XApp.notify(project, "Install Fail", "${fails.map { it.name }}", NotificationType.WARNING, listOf(XNotifyAction("ReTry") { tryInstall(project, fails, path, param) }))
            }
        }
    }

    private fun launchApk(path: String, device: IDevice): Boolean {
        val packageId = getAppInfoFromApk(File(path))?.packageId
        if (packageId != null) {
            val cmd = "monkey -p $packageId -c android.intent.category.LAUNCHER 1"
            val result = AdbShellCommandsUtil.executeCommand(device, cmd).output
            XApp.log("adb shell $cmd -> $result")
            return result.lastOrNull()?.contains("No activities") == true
        }
        return false
    }

    fun base64Encode(source: String) = String(Base64.getEncoder().encode(source.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)
    fun base64Decode(source: String) = String(Base64.getDecoder().decode(source.toByteArray(Charsets.UTF_8)), Charsets.UTF_8)

    fun Module?.realName(): String = this?.name?.let { it.substringAfterLast(".").substringAfterLast(":") } ?: ""
}

