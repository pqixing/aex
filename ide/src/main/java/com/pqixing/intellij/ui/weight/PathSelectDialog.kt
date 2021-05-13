//package com.pqixing.intellij.ui.weight
//
//import com.intellij.openapi.fileChooser.FileChooser
//import com.intellij.openapi.fileChooser.FileChooserDescriptor
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VirtualFile
//import javax.swing.JButton
//import javax.swing.JComponent
//import javax.swing.JPanel
//import javax.swing.JTextField
//
//class PathSelectDialog(project: Project, path: String, val onSelect: (selects: String?) -> Unit = {}) : XDialog(project) {
//    private var pTop: JPanel? = null
//    override fun createNorthPanel(): JComponent? = pTop
//    override fun createCenterPanel(): JComponent? = null
//    private lateinit var tvPath: JTextField
//    private lateinit var btnPath: JButton
//
//    init {
//        tvPath.text = path
//        btnPath.addActionListener {
//            FileChooser.chooseFiles(FileChooserDescriptor(true, false, false, false, false, false), project, project.projectFile) { files: List<VirtualFile> ->
//                tvPath.text = files.firstOrNull()?.canonicalPath
//            }
//        }
//    }
//
//    override fun getTitleStr(): String = "Input Path"
//
//    override fun doOKAction() {
//        super.doOKAction()
//        onSelect(tvPath.text)
//    }
//
//    override fun doCancelAction() {
//        super.doCancelAction()
//        onSelect(null)
//    }
//
//    override fun createMenus(): List<JComponent?> {
//        return emptyList()
//    }
//}