package com.pqixing.intellij.gradle

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.pqixing.intellij.XApp.getSp
import com.pqixing.intellij.XApp.putSp
import com.pqixing.intellij.ui.pop.PopOption
import com.pqixing.intellij.ui.weight.XDialog
import com.pqixing.intellij.uitils.UiUtils.realName

class TaskPopParam(val key: String, val project: Project, val dialog: XDialog) {


    val locals = ModuleManager.getInstance(project).modules.map { it.realName() }

    var params: String = "param_custom_$key"
        get() = field.getSp("", project).toString()
        set(value) {
            field.putSp(value, project)
        }


    var install: String = "param_install_$key"
        get() = field.getSp("-t -r ", project).toString()
        set(value) {
            field.putSp(value, project)
        }

    var outside = false
        set(value) {
            field = value
            dialog.adapter().datas().forEach { it.visible = value || locals.contains(it.title) }
            dialog.onSelect(null)
        }

    fun showInputDialog(title: String, msg: String, value: String): String {
        return Messages.showInputDialog(project, msg, title, null, value, null) ?: value
    }


    fun installOption() = PopOption("", "Install", install.ifEmpty { "input install params" }, install.isNotEmpty()) {
        install = showInputDialog("Apk Install Param", "input adb install param", install)
    }

    fun getGradleParams(): String {
        return params
    }

    fun getActions(): List<PopOption<String>> {
        return listOf(
            PopOption("", "outside", " show outside module", outside) { outside = it },
            PopOption("", "params", params.ifEmpty { "input gradle params" }, params.isNotEmpty()) {
                params = showInputDialog("Gradle Params", "input params for  gradle task, etc: --no-deamon -Dlog=true", params)
            },
        )
    }

}