package com.wang.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.wang.adapters.adapter.BaseAdapterRvList
import com.wang.sticky.StickyNestedScrollLayout
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rv = findViewById<RecyclerView>(R.id.rv_main)
        val adapter = BaseAdapterRvList.createAdapter<String>(R.layout.adapter_main)
        val list = ArrayList<String>()
        for (i in 0..79) {
            list.add("第$i")
        }
        adapter.setListAndNotifyDataSetChanged(list)
        rv.adapter = adapter

        val snsl = findViewById<StickyNestedScrollLayout>(R.id.snsl_main)
        //代码设置
//        val tv = findViewById<TextView>(R.id.tv_main)
//        snsl.setChildTag(tv, StickyNestedScrollLayout.TAG_STICKY)
//        snsl.setChildTag(rv, StickyNestedScrollLayout.TAG_MATCH)
    }
}