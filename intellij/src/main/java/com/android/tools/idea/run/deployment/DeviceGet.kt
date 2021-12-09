package com.android.tools.idea.run.deployment

import com.android.ddmlib.IDevice
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

object DeviceGet {
    fun getSelectDevices(project: Project): List<IDevice>? {
        val action = ActionManager.getInstance().getAction("DeviceAndSnapshotComboBox") ?: return null
        val devices = invokeSelectDevices(project, action) ?: invokeSelectDevice(project, action) ?: invokeDeviceList(project, action) ?: return null
        return devices.mapNotNull { getDdmlibDevice(it) }
    }

    private fun getDdmlibDevice(device: Device): IDevice? = kotlin.runCatching {
        val ddmlb = Device::class.java.getDeclaredMethod("getDdmlibDevice")
        ddmlb.isAccessible = true
        ddmlb.invoke(device) as? IDevice
    }.getOrNull()

    private fun invokeDeviceList(project: Project, action: AnAction): List<Device>? = kotlin.runCatching {
        val method = action.javaClass.getDeclaredMethod("getDevices", Project::class.java)
        method.isAccessible = true
        method.invoke(action, project) as? List<Device>
    }.getOrNull()


    private fun invokeSelectDevice(project: Project, action: AnAction): List<Device>? = kotlin.runCatching {
        val method = action.javaClass.getDeclaredMethod("getSelectedDevice", Project::class.java)
        method.isAccessible = true
        listOf(method.invoke(action, project) as Device)
    }.getOrNull()

    /**
     * 兼容新版AS获取多个设备
     */
    private fun invokeSelectDevices(project: Project, action: AnAction): List<Device>? = kotlin.runCatching {
        val method = action.javaClass.getDeclaredMethod("getSelectedDevices", Project::class.java)
        method.isAccessible = true
        //List<Devices>
        method.invoke(action, project) as List<Device>
    }.getOrNull()
}