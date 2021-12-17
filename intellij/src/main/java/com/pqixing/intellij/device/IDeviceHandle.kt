package com.pqixing.intellij.compat.interfaces

import com.intellij.openapi.project.Project
import com.pqixing.intellij.device.DeviceWrapper

interface IDeviceProvide {
    fun getSelectDevices(project: Project): List<DeviceWrapper>?
}

interface IAdbExecute {
    fun runAdbTask(device: DeviceWrapper, cmd: String): List<String>?
}

interface IAdbInstall {
    fun install(device: DeviceWrapper, path: String, param: String): Boolean
}


