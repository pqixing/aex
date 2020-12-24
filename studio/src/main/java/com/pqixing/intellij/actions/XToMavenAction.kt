package com.pqixing.intellij.actions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.module.ModuleManager
import com.pqixing.intellij.XApp
import com.pqixing.intellij.XNotifyAction
import com.pqixing.intellij.common.XEventAction
import com.pqixing.intellij.gradle.GradleRequest
import com.pqixing.intellij.ui.weight.*
import com.pqixing.intellij.uitils.UiUtils.realName
import com.pqixing.model.impl.ModuleX
import git4idea.repo.GitRepository
import javax.swing.*


open class XToMavenAction : XEventAction<XToMavenDialog>()

class XToMavenDialog(e: AnActionEvent) : XModuleDialog(e) {
    private var pTop: JPanel? = null
    override fun createNorthPanel(): JComponent? = pTop

    private lateinit var btnFile: JButton
    private lateinit var cbRepeat: JCheckBox
    private lateinit var cbBranch: JCheckBox
    private lateinit var cbClean: JCheckBox
    private lateinit var cbShowAll: JCheckBox
    private lateinit var jlTopTitle: JLabel

    private lateinit var btnPick: JButton
    private lateinit var tvMapFile: JTextField

    val locals = ModuleManager.getInstance(project).modules.map { it.realName() }
    val curModule = e.getData(LangDataKeys.MODULE)?.realName()
    override fun getTitleStr(): String = "ToMaven : ${manifest.branch}"


    override fun initWidget() {
        super.initWidget()
        jlTopTitle.text = manifest.root.name + "    "
        cbShowAll.addItemListener { onShowAllChange(cbShowAll.isSelected) }
        val ignore = manifest.config.ignore
        cbRepeat.isSelected = ignore.contains("1")
        cbClean.isSelected = ignore.contains("2")
        cbBranch.isSelected = ignore.contains("3")
        initMapFile()

    }

    private fun initMapFile() {
        tvMapFile.text = manifest.config.mapping
        btnFile.toolTipText = tvMapFile.text
        btnFile.addActionListener {
            val inputMap = btnPick.isVisible
            for (component in (tvMapFile.parent as JPanel).components.sortedBy { it.x }) {
                if (component === btnFile) break
                component.isVisible = inputMap
            }
            btnPick.isVisible = !inputMap
            tvMapFile.isVisible = !inputMap
            btnFile.toolTipText = tvMapFile.text
        }

        btnPick.addActionListener { e ->
            FileChooser.chooseFiles(
                FileChooserDescriptor(true, false, false, false, false, false),
                project, project.projectFile
            ) { it.firstOrNull()?.canonicalPath?.let { l -> tvMapFile.text = l } }
        }
    }

    fun onShowAllChange(selected: Boolean) {
        adapter.datas().forEach { it.visible = selected || locals.contains(it.title) }
        onSelect(null)
    }

    override fun onSelect(item: XItem?) {
        super.onSelect(item)
        isOKActionEnabled = adapter.datas().any { it.visible && it.select }
    }

    override fun loadList(): List<XItem> {
        return super.loadList().reversed()//反转
    }

    override fun onItemUpdate(item: XItem, module: ModuleX, repo: GitRepository?): Boolean {
        item.select = curModule == module.name
        val compileStr = "${module.group()}:${module.name}:${module.version}"
        item.content = "${module.group()}:${module.version}"
        item.tvContent.toolTipText = compileStr
        item.tvContent.addMouseClickR { c, e -> onCopyClickR(item, c, e, listOf(compileStr, module.maven().url)) }
        item.visible = locals.contains(module.name)
        return super.onItemUpdate(item, module, repo)
    }

    /**
     * 开始上传代码
     */
    private fun startToMaven(item: XItem, ignore: String, mapping: String, activate: Boolean = false) {
        isOKActionEnabled = false
        item.state = XItem.KEY_WAIT
        GradleRequest(
            listOf(":${item.title}:ToMaven"),
            mapOf("include" to item.title, "local" to "false", "ignore" to ignore, "mapping" to mapping),
            activate = activate
        ).runGradle(project) { result ->
            if (!result.success) {
                item.state = XItem.KEY_ERROR
                isOKActionEnabled = true
                val msg = result.getDefaultOrNull() ?: ""
                XApp.notify(
                    project,
                    "ToMaven Fail",
                    msg,
                    NotificationType.WARNING,
                    listOf(XNotifyAction("Retry") { startToMaven(item, ignore, mapping) })
                )
                return@runGradle
            }
            item.state = XItem.KEY_SUCCESS
            item.content = "${item.content.split(":").firstOrNull()}:${result.getDefaultOrNull()?.substringAfterLast(":")}"
            item.select = false

            val next = adapter.datas().find { it.select && it.visible }
            if (next != null) return@runGradle XApp.invoke { startToMaven(next, ignore, mapping) }

            isOKActionEnabled = true
            XApp.notify(project, "ToMaven Finish", "")
            return@runGradle
        }
    }

    override fun doOKAction() {
        val next = adapter.datas().find { it.select && it.visible } ?: return
        isOKActionEnabled = false
        val ignore = arrayOf(cbRepeat.isSelected, cbClean.isSelected, cbBranch.isSelected)
            .mapIndexedNotNull { index: Int, b: Boolean -> (index + 1).takeIf { b } }
            .joinToString(",")
        startToMaven(next, ignore, tvMapFile.text.trim(), true)
    }
}


