package com.pqixing.bumblebee.compat

import com.android.ddmlib.IDevice
import com.pqixing.intellij.compat.interfaces.IAdbExecute
import com.pqixing.intellij.device.DeviceWrapper

class AdbExecuteNew : IAdbExecute {
    override fun runAdbTask(device: DeviceWrapper, cmd: String): List<String>? {
        return com.android.tools.idea.adb.AdbShellCommandsUtil.executeCommand(device.device as IDevice, cmd).output
    }
}