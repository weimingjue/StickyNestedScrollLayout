package com.wang.example;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.wang.adapters.adapter.BaseAdapterRvList;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRv = findViewById(R.id.rv_main);
        BaseAdapterRvList<?, String> adapter = BaseAdapterRvList.createAdapter(R.layout.adapter_main);
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            list.add("第" + i);
        }
        adapter.setListAndNotifyDataSetChanged(list);
        mRv.setAdapter(adapter);

        //代码设置
//        StickyNestedScrollLayout snsl = findViewById(R.id.snsl_main);
//        TextView tv = findViewById(R.id.tv_main);
//        snsl.setChildTag(tv, "sticky");
//        snsl.setChildTag(mRv, "match");
    }
}
