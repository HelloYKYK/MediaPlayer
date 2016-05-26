package com.itheima84.moblieplayer.base;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import butterknife.ButterKnife;

/**
 * Created by lxj on 2016/5/12.
 */
public abstract class BaseActivity extends AppCompatActivity  implements UIOperation {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //1.初始化View,由于findViewById我们使用ButterKnife来实现，所以省略
//        initView();
        setContentView(getLayoutId());
        ButterKnife.bind(this);

        //2.设置监听器
        setListener();

        //3.初始化数据
        initData();
    }

    @Override
    public void onClick(View v) {

    }
}
