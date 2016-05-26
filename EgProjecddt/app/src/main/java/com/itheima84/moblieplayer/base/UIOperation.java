package com.itheima84.moblieplayer.base;

import android.view.View;

/**
 * Created by lxj on 2016/5/12.
 */
public interface UIOperation extends View.OnClickListener{

    int getLayoutId();

    void setListener();

    void initData();
}
