package com.pqixing.bumblebee.compat

import com.android.ddmlib.IDevice
import com.android.tools.idea.run.deployment.Device
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.pqixing.access
import com.pqixing.intellij.XApp
import com.pqixing.intellij.compat.interfaces.IDeviceProvide
import com.pqixing.intellij.device.DeviceWrapper
import com.pqixing.invokeMethod

class DeviceProvide : IDeviceProvide {

    override fun getSelectDevices(project: Project): List<DeviceWrapper>? {
        val action = ActionManager.getInstance().getAction("DeviceAndSnapshotComboBox") ?: return null
        return (invokeSelectDevices(project, action)
            ?: invokeSelectDevice(project, action)
            ?: invokeDeviceList(project, action))
            ?.mapNotNull { getDdmlibDevice(it) }?.filter { it.isOnline }
            ?.takeIf { it.isNotEmpty() }?.map { DeviceWrapper(it.name, it) }
    }

    private fun getDdmlibDevice(device: Device): IDevice? = kotlin.runCatching {
        Device::class.java.getDeclaredMethod("getDdmlibDevice").access().invoke(device) as? IDevice
    }.onFailure { XApp.log(it.toString()) }.getOrNull()

    private fun invokeDeviceList(project: Project, action: AnAction): List<Device>? = kotlin.runCatching {
        action.invokeMethod("getDevices", arrayOf(project), arrayOf(Project::class.java)) as? List<Device>
    }.onFailure { XApp.log(it.toString()) }.getOrNull()


    private fun invokeSelectDevice(project: Project, action: AnAction): List<Device>? = kotlin.runCatching {
        listOf(action.invokeMethod("getSelectedDevice", arrayOf(project), arrayOf(Project::class.java)) as Device)
    }.onFailure { XApp.log(it.toString()) }.getOrNull()

    /**
     * 兼容新版AS获取多个设备
     */
    private fun invokeSelectDevices(project: Project, action: AnAction): List<Device>? = kotlin.runCatching {
        action.invokeMethod("getSelectedDevices", arrayOf(project), arrayOf(Project::class.java)) as? List<Device>
    }.onFailure { XApp.log(it.toString()) }.getOrNull()
}