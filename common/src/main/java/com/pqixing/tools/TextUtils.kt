package com.pqixing.tools

import com.pqixing.codec.digest.DigestUtils


object TextUtils {
    var count = 0

    val onlyName: String
        get() = "${System.currentTimeMillis()}${count++}"

    fun md5Hex(str: String?) = str?.let { DigestUtils.md5Hex(it) }

    /**
     * 只保留数字和字母
     * @param str
     * @return
     */
    fun numOrLetter(str: String): String {
        return str.trim().replace("[^0-9a-zA-Z]".toRegex(), "")
    }

    /**
     * 只保留数字和字母
     * @param str
     * @return
     */
    fun letter(str: String): String {
        return str.trim().replace("[^a-zA-Z]".toRegex(), "")
    }

    /**
     * 只保留数字和字母
     * @param str
     * @return
     */
    fun letter(str: String, def: String): String {
        return letter(str).takeIf { it.isNotEmpty() } ?: def
    }


    fun firstUp(source: String): String {
        return if (source.isEmpty()) "" else "${source.substring(0, 1).toUpperCase()}${source.substring(1)}"
    }

    fun className(source: String): String {
        return firstUp(numOrLetter(source))
    }

    /**
     * 比较版本号的大小,前者大则返回一个正数,后者大返回一个负数,相等则返回0
     * @param version1
     * @param version2
     * @return
     */
    fun compareVersion(version1: String?, version2: String?): Int {
        if (version1 == null || version2 == null) return 0

        val versionArray1 = version1.trim().split("\\.".toRegex())//注意此处为正则匹配，不能用"."；
        val versionArray2 = version2.trim().split("\\.".toRegex())
        var idx = 0
        val minLength = Math.min(versionArray1.size, versionArray2.size)//取最小长度值
        var diff: Int
        while (idx < minLength) {
            //先比较长度,长度不一致时，较长的数字为大，不考虑0开头的数字
            diff = versionArray1[idx].length - versionArray2[idx].length
            if (diff != 0) return diff

            diff = versionArray1[idx].compareTo(versionArray2[idx])
            if (diff != 0) return diff

            ++idx
        }
        //如果已经分出大小，则直接返回，如果未分出大小，则再比较位数，有子版本的为大；
        return versionArray1.size - versionArray2.size
    }

    /**
     * 拼接url
     * @param urls
     * @return
     */
    fun append(urls: Array<String>, s: String = "/"): String {
        val newUrl = StringBuilder()
        for (url in urls) {
            if (url.isEmpty()) continue
            if (newUrl.isNotEmpty() && !newUrl.endsWith(s) && !url.startsWith(s)) newUrl.append(s)
            newUrl.append(url)
        }
        return newUrl.toString()
    }

    fun removeMark(s: String?): String {
        return s?.replace("\"|'".toRegex(), "") ?: ""
    }

    fun removeLineAndMark(s: String): String {
        return removeMark(s.replace("\r|\n".toRegex(), ""))
    }
}