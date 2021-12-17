package com.pqixing.intellij.compat.impl

import com.pqixing.intellij.XApp
import com.pqixing.intellij.compat.interfaces.IAdbExecute
import com.pqixing.intellij.device.DeviceWrapper
import com.pqixing.invokeMethod

class AdbExecute : IAdbExecute {
    override fun runAdbTask(device: DeviceWrapper, cmd: String): List<String>? {
        return com.android.tools.idea.explorer.adbimpl.AdbShellCommandsUtil.executeCommand(device.device as com.android.ddmlib.IDevice, cmd).output
    }
}

