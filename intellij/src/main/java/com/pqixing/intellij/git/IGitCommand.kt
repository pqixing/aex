package com.pqixing.intellij.git

import com.intellij.openapi.ui.Messages
import com.jetbrains.rd.util.string.printToString
import com.pqixing.intellij.XApp
import com.pqixing.intellij.actions.XGitDialog
import com.pqixing.intellij.ui.weight.XItem
import com.pqixing.model.impl.ProjectX
import git4idea.GitUtil
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository


abstract class IGitCmd(val dialog: XGitDialog, val name: String) {
    val KEY_REPO = dialog.KEY_REPO
    val KEY_PRO = dialog.KEY_PRO
    val KEY_BRS = dialog.KEY_BRS
    open fun visible(br: String, item: XItem): Boolean = item.get<GitRepository>(KEY_REPO) != null && containsBr(item, br)
    open fun exe(br: String, item: XItem, cmd: String): Boolean = false
    open fun before(br: String, items: List<XItem>): Boolean = true
    override fun toString(): String = name

    fun containsBr(item: XItem, br: String): Boolean = br.isEmpty() || item.get<Collection<String>>(KEY_BRS)?.contains(br) ?: false
}

class Custom(dialog: XGitDialog, val gitCmd: GitCommand) : IGitCmd(dialog, gitCmd.name()) {
    override fun before(br: String, items: List<XItem>): Boolean {
        var enable = false
        val cmdStr = dialog.tvCommand.text.trim()
        XApp.invoke(true) {

            val cmds = items.map {
                "${it.title}  : \n " + cmdStr.replace("\$target", br).replace("\$name", it.title)
                    .replace("\$branch", it.get<GitRepository>(KEY_REPO)?.currentBranchName ?: "")
            }

            enable = Messages.OK == Messages.showDialog(
                dialog.project, cmds.joinToString("\n"), "git $gitCmd :", arrayOf("OK", "Cancel"),0, null
            )
        }
        return enable
    }

    override fun visible(br: String, item: XItem): Boolean = true
    override fun exe(br: String, item: XItem, cmd: String): Boolean {
        val pro = item.get<ProjectX>(KEY_PRO) ?: return false
        val repo = item.get<GitRepository>(KEY_REPO)
        val handler = GitLineHandler(dialog.project, pro.absDir(), gitCmd)
        handler.setStdoutSuppressed(false)
        val cmdStr = cmd
            .replace("\$target", br)
            .replace("\$name", item.title)
            .replace("\$branch", repo?.currentBranchName ?: "")
        handler.addParameters(cmdStr.split(" ").map { it.trim() }.filter { it.isNotEmpty() })
        handler.endOptions()
        handler.addLineListener(dialog.listener)
        dialog.listener.onLineAvailable("${pro.absDir()}  :  git ${gitCmd.name()} $cmdStr", null)
        val success = kotlin.runCatching { GitHelper.getGit().runCommand(handler).success() }
            .getOrElse { dialog.listener.onLineAvailable(it.printToString(), null);false }
        if (success) dialog.loadRepo(pro, item)
        return success
    }
}


class Clone(dialog: XGitDialog) : IGitCmd(dialog, "clone") {
    override fun visible(br: String, item: XItem): Boolean = item.get<ProjectX>(KEY_PRO)?.absDir()?.exists() == false
    override fun exe(br: String, item: XItem, param: String): Boolean {
        val pro = item.get<ProjectX>(KEY_PRO) ?: return false
        val file = pro.absDir()
        val url = pro.getGitUrl()
        return GitUtil.isGitRoot(file) || GitHelper.clone(dialog.project, br, file, url, dialog.listener)
    }
}

class Merge(dialog: XGitDialog) : IGitCmd(dialog, "merge") {

    override fun before(br: String, items: List<XItem>): Boolean {
        var enable = false
        XApp.invoke(true) {
            enable = Messages.OK == Messages.showOkCancelDialog(
                dialog.project, "Sure merge $br", "Warning", "Merge", "Cancel", null
            )
        }
        return enable
    }

    override fun exe(br: String, item: XItem, cmd: String): Boolean {
        val repo = item.get<GitRepository>(KEY_REPO)
        return GitHelper.checkBranchExists(repo, br) && GitHelper.merge(dialog.project, br, repo, dialog.listener) == "Success"
    }
}

class Delete(dialog: XGitDialog) : IGitCmd(dialog, "delete") {

    override fun before(br: String, items: List<XItem>): Boolean {
        var enable = false
        XApp.invoke(true) {
            enable = Messages.OK == Messages.showOkCancelDialog(
                dialog.project, "Sure delete $br", "Warning", "Delete", "Cancel", null
            )
        }
        return enable
    }

    override fun exe(br: String, item: XItem, cmd: String): Boolean {
        val repo = item.get<GitRepository>(KEY_REPO)
        return GitHelper.delete(dialog.project, br, repo, dialog.listener) == "Success"
    }
}

class Create(dialog: XGitDialog) : IGitCmd(dialog, "create") {
    override fun visible(br: String, item: XItem): Boolean {
        return item.get<GitRepository>(KEY_REPO) != null && !containsBr(item, br)
    }

    override fun exe(br: String, item: XItem, cmd: String): Boolean {
        val branch = if (br.startsWith("origin/")) br.substring(7) else br
        val repo = item.get<GitRepository>(KEY_REPO)
        return GitHelper.create(dialog.project, branch, repo, dialog.listener) == "Success"
    }
}

class Checkout(dialog: XGitDialog) : IGitCmd(dialog, "checkout") {
    override fun exe(br: String, item: XItem, cmd: String): Boolean {
        val repo = item.get<GitRepository>(KEY_REPO) ?: return false
        val branch = if (br.startsWith("origin/")) br.substring(7) else br
        GitHelper.checkoutSync(dialog.project, br, listOf(repo))
        repo.update()
        item.tag = repo.currentBranchName ?: branch
        return branch == repo.currentBranchName ?: ""
    }
}