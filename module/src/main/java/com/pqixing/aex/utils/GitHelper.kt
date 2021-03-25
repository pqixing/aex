package com.pqixing.aex.utils

import com.jcraft.jsch.Session
import com.pqixing.aex.setting.XSetting
import com.pqixing.base64Decode
import com.pqixing.base64Encode
import com.pqixing.hash
import com.pqixing.model.impl.ModuleX
import com.pqixing.real
import com.pqixing.tools.FileUtils
import org.eclipse.jgit.api.*
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.*
import java.io.File
import java.text.SimpleDateFormat

class GitHelper(val set: XSetting) {

    val dateVersion = SimpleDateFormat("yyyyMMddHHmmss")

    fun open(file: File?): Git? = kotlin.runCatching { Git.open(file) }.getOrNull()

    fun clone(module: ModuleX, branch: String = "master"): Git? {
        val gitDir = module.project.absDir()
        if (gitDir.exists()) FileUtils.delete(gitDir)

        val gitInfo = module.project.git
        var url = module.project.getGitUrl()
        set.println("JGIT start clone : $url")
        val git: Git? = Git.cloneRepository().setURI(url).setDirectory(gitDir)
            .setTransportConfigCallback(transportConfigCallback)
            .setCredentialsProvider(UsernamePasswordCredentialsProvider(gitInfo.user, gitInfo.psw.real()))
            .setProgressMonitor(PercentProgress(set))
            .exe("clone $url ${gitDir.name} && cd ${gitDir.name}", gitDir.parentFile) ?: open(gitDir)

        //暂停100毫秒
        Thread.sleep(100)
        checkout(git, branch, true, module)
        return git

    }

    fun runGitCmd(cmd: String, dir: File?): String? = kotlin.runCatching {
        set.println("Shell cmd -> 'git $cmd'  -> ${dir?.absolutePath ?: ""}")
        val p = Runtime.getRuntime().exec("git $cmd", null, dir)
        val input = p.inputStream.bufferedReader()
        val error = p.errorStream.bufferedReader()
        val result = input.readText() + error.readText()
        input.close()
        error.close()
        result
    }.getOrNull() ?: "Exception"
//
//    /**
//     * 删除分支
//     */
//    fun delete(git: Git?, branchName: String): Boolean {
//        git ?: return false
//        return try {
//            val localBranch = "refs/heads/$branchName"
//            git.branchDelete().setBranchNames(localBranch).setForce(true).call()
//
//            val refSpec = RefSpec()
//                .setSource(null)
//                .setDestination("refs/heads/$branchName");
//            git.push().setRefSpecs(refSpec).setRemote("origin").exe("push origin --delete $branchName");
//            Tools.println("Delete ${git.repository.directory.parentFile.name} branch -> $branchName")
//            true
//        } catch (e: Exception) {
//            Tools.println("Delete Exception -> $e")
//
//            false
//        }
//    }
//
//    /**
//     *刷新工程
//     */
//    fun push(git: Git?, force: Boolean = false): Boolean {
//        git ?: return false
//        try {
//            val call = git.push().setForce(force).exe("push ${if (force) "--force" else ""}")
//            Tools.println("Push ${git.repository.directory.parentFile.name} Complete push -> ${call?.map { it.messages }}")
//        } catch (e: Exception) {
//            Tools.println(" Exception push -> $e")
//            return false
//        }
//        return true
//    }

    /**
     *刷新工程
     */
    fun pull(git: Git?, module: ModuleX): Boolean {
        git ?: return false
        return try {
            val gitInfo = module.project.git
            val call = git.pull()
                .setTransportConfigCallback(transportConfigCallback)
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(gitInfo.user, gitInfo.psw.real()))
                .exe("pull")
            val isSuccessful = call?.isSuccessful ?: false
            set.println(
                "Pull ${git.repository.directory.parentFile.name} Complete-> $isSuccessful  ${
                    git.log().setMaxCount(1).call().map { "${it.committerIdent} -> ${it.fullMessage}" }[0]
                }"
            )
            isSuccessful
        } catch (e: Exception) {
            set.println(" Exception pull-> $e")
            false
        }
    }
//
//    /**
//     * 创建分支
//     */
//    fun createBranch(git: Git?, branchName: String): Boolean {
//        git ?: return false
//        //在同一个分支，不处理
//        if (branchName == git.repository.branch) return true
//        if (!pull(git)) return false
//
//        val end = "/$branchName"
//        val b =
//            git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().firstOrNull { it.name.endsWith(end) }
//        if (b != null) {//如果已经存在分支，则直接切换过去
//            return checkoutBranch(git, branchName, true)
//        }
//        //创建本地分支
//        val call = git.branchCreate().setName(branchName).exe("branch $branchName")
//        //提交远程分支
//        git.push().add(call).exe("push origin $branchName");
//        //删除本地分支（以便于checkout远程分支，方便关联）,
//        git.branchDelete().setBranchNames(branchName).call()
//        //关联本地和远程分支
//        Tools.println("Create Branch ${git.repository.directory.parentFile.name} -> $branchName")
//        //创建分支成功，切换到对应分支
//        return tryCheckOut(
//            git,
//            branchName,
//            git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call(),
//            true
//        )
//    }

    /**
     * 检查git是否clean状态
     */
    fun checkIfClean(git: Git?, path: String? = null): Boolean {
        git ?: return true
        val unClean = mutableSetOf<String>()
        val call = git.status().apply { if (path != null) addPath(path) }.call()
        unClean += call?.untracked ?: emptySet()
        unClean += call?.uncommittedChanges ?: emptySet()
        set.println("${git.repository.directory.parentFile.name} -> checkIfClean :untracked -> $unClean")

        return unClean.isEmpty()
    }

    /**
     * 检查分支是否一致
     */
    fun checkout(git: Git?, branchName: String, focusCheckOut: Boolean, module: ModuleX): Boolean {
        git ?: return false
        //在同一个分支，不处理
        if (branchName == git.repository.branch) return true
        val isClean = checkIfClean(git)
        //将本地修改文件存到暂存区
        if (!isClean) git.stashCreate().exe("stash")

        var remote = false
        var tryCheckOut = tryCheckOut(git, branchName, git.branchList().call(), remote)
        if (!tryCheckOut) {
            remote = true
            //本地没有分支时，先尝试更新一下，然后再进行处理
            pull(git, module)
            tryCheckOut = tryCheckOut(
                git,
                branchName,
                git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call(),
                remote
            )
        }
        //还原本地的代码
        if (!isClean && !focusCheckOut) git.stashApply().exe("stash pop")

        if (!tryCheckOut) set.println("Can not find branch: $branchName ")


        return tryCheckOut
    }

    /**
     * 查找对应的分支
     */
    fun findBranchRef(git: Git?, branchName: String, remote: Boolean): Ref? {
        git ?: return null
        val end = "/$branchName"
        val list = git.branchList()
        if (remote) list.setListMode(ListBranchCommand.ListMode.REMOTE)
        for (c in list.call()) {
            if (c.name.endsWith(end)) return c
        }
        return null
    }

    /**
     * 尝试切换分支
     */
    fun tryCheckOut(git: Git, branchName: String, ls: List<Ref>, remote: Boolean): Boolean {
        val end = "/$branchName"
        for (c in ls) {
            if (c.name.endsWith(end)) {
                val command = git.checkout().setName(branchName)
                if (remote) {
                    command.setCreateBranch(true)
                        .setStartPoint(c.name)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                }
                command.exe("checkout $branchName")
                set.println("Checkout ${git.repository.directory.parentFile.name}-> ${if (remote) "remote" else "local"} branch $branchName")
                return git.repository.branch == branchName//是否切换成功
            }
        }
        return false
    }

    /**
     * 查找出对应的git根目录
     * @param dir
     * @return
     */
    fun findGitDir(dir: File?): File? {
        var p = dir
        var g: File?
        while (p != null) {
            if (isGitDir(p)) return p
            p = p.parentFile
        }
        return null
    }

    inline fun isGitDir(dir: File): Boolean {
        val g = File(dir, ".git")
        return g.isDirectory && g.exists()
    }

//    @JvmStatic
//    fun getGitNameFromUrl(url: String?): String {
//        url ?: return ""
//        val s = url.lastIndexOf("/") + 1
//        val e = url.indexOf(".", s)
//        return url.substring(s, if (e == -1) url.length else e)
//    }

    fun close(git: Git?) {
        git ?: return
        val gitLock = File(git.repository.directory, "index.lock")
        git.close()
        if (gitLock.exists())//执行完成后，删除index.lock文件，防止其他集成无法操作
            FileUtils.delete(gitLock)
    }

    fun <T> GitCommand<T>.exe(cmd: String, dir: File? = repository?.directory): T? = kotlin.runCatching {
        set.println("JGit START ${this.javaClass.simpleName}")
        this.call()
    }.getOrNull() ?: kotlin.run {
        set.println("JGit Fail -> try config user or password or set ssh-key use -> ssh-keygen -m PEM -t rsa -b 2048")
        set.println(runGitCmd(cmd, dir) ?: "")
        null
    }
}

//fun <T> GitCommand<T>.exe(cmd: String, dir: File? = repository?.directory): T? = kotlin.runCatching {
//    Tools.println("JGit START ${this.javaClass.simpleName}")
//    if (this is TransportCommand<*, *>) {
//        setTransportConfigCallback(transportConfigCallback)
//        setCredentialsProvider(UsernamePasswordCredentialsProvider(GitHelper.gitUser, GitHelper.gitPsw))
//    }
//    if (this is CloneCommand) this.setProgressMonitor(PercentProgress())
//    this.call()
//}.getOrNull() ?: kotlin.run {
//    Tools.println("JGit Fail -> try config user or password or set ssh-key use -> ssh-keygen -m PEM -t rsa -b 2048")
//    Tools.println(runGitCmd(cmd, dir))
//    null
//}

private var sshSessionFactory: SshSessionFactory = object : JschConfigSessionFactory() {
    override fun configure(hc: OpenSshConfig.Host, session: Session) {}
}

private var transportConfigCallback = TransportConfigCallback { transport: Transport? ->
    if (transport is SshTransport) {
        transport.sshSessionFactory = sshSessionFactory
    }
}


