package com.pqixing.mock

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import com.pqixing.aex.door.R
import java.lang.reflect.Method
import java.util.*

class StartActivity : Activity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = packageName
        setContentView(R.layout.activity_door)

        val lvClass = findViewById<ListView>(R.id.lvClass)
        val etSearch = findViewById<EditText>(R.id.etSearch)


        val sp = getSharedPreferences("config", 0)
        val activitys = packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES).activities
            .map { Triple(it.name.substringAfterLast("."), it.name.substringBeforeLast("."), it.name) }
            .sortedBy { it.third }
        //置顶类
        val pins = sp.getStringSet("pin", emptySet())!!.toMutableSet()
        //过滤的group名字
        val groups = sp.getStringSet("group", emptySet())!!.toMutableSet()

        //最近点击，默认排最上面
        val recents = sp.getString("recent", "")!!.split(",").toMutableList()


        val callBack = object : DoorCallBack {
            override fun reLoadFilters(): List<Triple<String, String, String>> {
                val searchKey = etSearch.text.toString().trim()
                val size = recents.size
                return activitys.filter { groups.isEmpty() || groups.contains(it.second) }
                    .filter { match(searchKey, it.third) }
                    //排序，先按照类型排一遍，再按照置顶和最近列表排上去
                    .sortedBy { (pins.indexOf(it.third) * size) + recents.indexOf(it.third) }
            }

            override fun isTop(clazz: String): Boolean = pins.contains(clazz)


            override fun onTopClick(clazz: String, position: Int) {
                if (isTop(clazz)) pins.remove(clazz) else pins.add(clazz)
                sp.edit().putStringSet("pin", pins).apply()
            }
        }

        val adapter = StartAdapter(callBack)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                adapter.notifyDataSetChanged()
            }

        })

        lvClass.adapter = adapter

        lvClass.setOnItemClickListener { parent, view, position, id ->
            val className = adapter.getItem(position).third

            tryStartActivity(className)

            recents.remove(className)
            recents.add(className)
            sp.edit().putString("recent", recents.joinToString(",")).apply()
            adapter.notifyDataSetChanged()
        }
        lvClass.setOnItemLongClickListener { parent, view, position, id ->
            val className = adapter.getItem(position).third
            tryStartActivityCustom(className) {
                recents.remove(className)
                recents.add(className)
                sp.edit().putString("recent", recents.joinToString(",")).apply()
                adapter.notifyDataSetChanged()
            }
            true
        }



        findViewById<View>(R.id.ivFilter).setOnClickListener {
            val allGroups = activitys.map { it.second }.distinctBy { it }.sortedBy { groups.indexOf(it) }
            val selects = allGroups.map { groups.contains(it) }.toBooleanArray()
            AlertDialog.Builder(this).setTitle("Select Filter Group")
                .setMultiChoiceItems(allGroups.toTypedArray(), selects) { d, i, s -> selects[i] = s }
                .setNegativeButton(android.R.string.cancel) { d, i -> d.dismiss() }
                .setPositiveButton(android.R.string.ok) { d, i ->
                    d.dismiss()
                    groups.clear()
                    groups += allGroups.filterIndexed { index, s -> selects[index] }
                    sp.edit().putStringSet("group", groups).apply()
                    adapter.notifyDataSetChanged()

                }
                .show()
        }

    }


    private fun tryStartActivityCustom(className: String, start: () -> Unit) {
        val methods = Class.forName(className).declaredMethods.filter {
            it.startActivity() || it.getIntent()
        }
        if (methods.isEmpty()) {
            tryStartActivity(className)
            start()
            return
        }

        AlertDialog.Builder(this).setTitle("Select Start Method ")
            .setSingleChoiceItems(methods.map { "${it.returnType.simpleName} ${it.name} ( ${it.parameterTypes.firstOrNull()?.simpleName ?: ""} )" }
                .toTypedArray(), -1) { d, i ->
                d.dismiss()
                startActivityByMethod(methods[i], className)
            }.show()

    }

    private fun startActivityByMethod(method: Method, className: String) = safeRun {

        val forName = Class.forName(className)

        if (method.startActivity()) {
            method.isAccessible = true
            method.invoke(forName.newInstance(), this)
        } else if (method.getIntent()) {
            val intent = method.invoke(forName.newInstance()) as Intent
            intent.setClass(this, Class.forName(className))
            startActivity(intent)
        } else {
            tryStartActivity(className)
        }
    }

    fun Method.startActivity(): Boolean {
        val types = this.parameterTypes
        return types.size == 1 && types[0].extends(Activity::class.java)
    }

    fun Method.getIntent(): Boolean {
        return this.returnType.extends(Intent::class.java)
    }


    fun Class<*>?.extends(clazz: Class<*>): Boolean {
        if (this == null) return false
        if (this == clazz) return true
        if (this.superclass.extends(clazz)) return true
        return this.interfaces.find { it.extends(clazz) } != null
    }


    inline fun match(searchKey: String, match: String): Boolean = match(searchKey, Collections.singletonList(match))

    fun match(searchKey: String, matchs: List<String>): Boolean {
        if (searchKey.isEmpty()) return true
        val key = searchKey.trim().lowercase()
        for (m in matchs) {
            var k = 0
            var l = -1
            val line = m.lowercase()
            while (++l < line.length) if (key[k] == line[l] && ++k == key.length) return true
        }
        return false
    }


    private fun tryStartActivity(className: String) = safeRun {
        startActivity(Intent(this, Class.forName(className)))
    }

    fun safeRun(block: () -> Unit) = kotlin.runCatching { block() }.onFailure {
        Toast.makeText(this, "Start Fail : ${it.message}", Toast.LENGTH_SHORT).show()
        Log.w("DoorActivity", "tryStartActivity: ", it)
    }
}