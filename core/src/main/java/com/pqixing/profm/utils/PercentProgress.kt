package com.pqixing.profm.utils

import com.pqixing.profm.setting.XSetting
import org.eclipse.jgit.lib.ProgressMonitor

class PercentProgress @JvmOverloads constructor(val set:XSetting,val space: Float = 1500f) : ProgressMonitor {
    private var title: String? = null
    private var last: Int = 0
    private var total: Int = 0

    private var lastLogTime = 0L;
    private var startTime = 0L
    //百分比间隔

    override fun start(totalTasks: Int) {
    }

    override fun beginTask(title: String, totalWork: Int) {
        this.title = title
        this.total = totalWork
        last = 0
        lastLogTime = 0L
        startTime = System.currentTimeMillis()
        set.println("   $title -> $totalWork")
    }

    override fun update(completed: Int) {
        last += completed
        val t = System.currentTimeMillis()
        if (last == total || t - lastLogTime >= space) {
            lastLogTime = t
//            Tools.println("          -> $last/$total : ${(last.toFloat() * 100 / total).toInt()}%")
        }
    }

    override fun endTask() {
//        Tools.println("   $title  ->  end spend :${System.currentTimeMillis() - startTime}")
    }

    override fun isCancelled(): Boolean {
        return false
    }
}
