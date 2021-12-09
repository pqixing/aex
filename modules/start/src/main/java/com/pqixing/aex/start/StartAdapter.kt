package com.pqixing.aex.start

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.pqixing.aex.door.R

class StartAdapter(val call: DoorCallBack) : BaseAdapter() {

    private var filters: List<Triple<String, String, String>> = call.reLoadFilters()


    override fun notifyDataSetChanged() {
        filters = call.reLoadFilters()
        super.notifyDataSetChanged()
    }

    override fun getCount(): Int = filters.size

    override fun getItem(position: Int): Triple<String, String, String> = filters[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view =
            convertView ?: LayoutInflater.from(parent.context).inflate(R.layout.item_activity, parent, false)
        val item = getItem(position)
        view.findViewById<TextView>(R.id.tvClassName).text = item.first
        view.findViewById<TextView>(R.id.tvClassPkg).text = item.second

        val className = item.third

        val isTop = call.isTop(className)
        val top = view.findViewById<ImageView>(R.id.ivTop)
        top.setImageResource(if (isTop) R.drawable.ic_menu_pin else R.drawable.ic_menu_top)
        top.setOnClickListener {
            call.onTopClick(className, position);
            notifyDataSetChanged()
        }
        return view
    }
}

interface DoorCallBack {
    fun reLoadFilters(): List<Triple<String, String, String>>
    fun isTop(clazz: String): Boolean
    fun onTopClick(clazz: String, position: Int)
}