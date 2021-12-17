package com.pqixing.intellij.compat.impl

import com.android.ddmlib.IDevice
import com.android.tools.apk.analyzer.AaptInvoker
import com.android.tools.apk.analyzer.AndroidApplicationInfo
import com.android.tools.idea.sdk.AndroidSdks
import com.android.utils.NullLogger
import com.pqixing.intellij.XApp
import com.pqixing.intellij.compat.AndroidCompat
import com.pqixing.intellij.compat.interfaces.IAdbInstall
import com.pqixing.intellij.device.DeviceWrapper
import java.io.File

class AdbInstall : IAdbInstall {

    override fun install(wrapper: DeviceWrapper, path: String, param: String): Boolean = runCatching {
        val device = wrapper.device as IDevice
        val newPath = "/data/local/tmp/${path.hashCode()}.apk"
        device.pushFile(path, newPath)
        val output = AndroidCompat.runAdbTask(wrapper, "pm  install $param  $newPath") ?: emptyList()
        XApp.log("installApk to ${device.name} : $path -> ${output.joinToString(";")}")
        val success = output.findLast { it.trim().isNotEmpty() }?.contains("Success") == true
        if (success) {
            launchApk(path, wrapper)
            AndroidCompat.runAdbTask(wrapper, "rm -f $newPath")
        } else AndroidCompat.runAdbTask(wrapper, "mv $newPath /sdcard/${path.substringAfterLast("/")}")
    }.onFailure {
        XApp.log(it.toString())
    }.isSuccess


    fun getAppInfoFromApk(fileApk: File): AndroidApplicationInfo? = try {
        val invoker = AaptInvoker(AndroidSdks.getInstance().tryToChooseSdkHandler(), NullLogger())
        val xmlTree = invoker.getXmlTree(fileApk, "AndroidManifest.xml")
        AndroidApplicationInfo.parse(xmlTree)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }


    private fun launchApk(path: String, device: DeviceWrapper): Boolean {
        val packageId = getAppInfoFromApk(File(path))?.packageId
        if (packageId != null) {
            val cmd = "monkey -p $packageId -c android.intent.category.LAUNCHER 1"
            val result = AndroidCompat.runAdbTask(device, cmd)
            XApp.log("adb shell $cmd -> $result")
            return result?.lastOrNull()?.contains("No activities") == true
        }
        return false
    }
}