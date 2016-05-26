package com.itheima84.moblieplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.base.BaseActivity;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by lxj on 2016/5/12.
 */
public class SplashActivity extends BaseActivity {


    @Bind(R.id.ll_container)
    LinearLayout llContainer;

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    public void setListener() {

    }

    @Override
    public void initData() {
        //一般在欢迎界面需要做所有的初始化操作，如
        //1.初始化数据库
        //2.初始化程序的文件目录结构
        //3.拷贝文件
        //4.初始化第三方类库，如ImageLoader，ShareSDK,


        int height = getWindowManager().getDefaultDisplay().getHeight();

        //1.初始化的时候让llContainer移动到下面
        ViewCompat.setTranslationY(llContainer,height);
        //2.执行向上移动的动画
        ViewCompat.animate(llContainer).translationY(0f)
                  .setDuration(800)
                  .setStartDelay(500)
                  .setInterpolator(new OvershootInterpolator())//设置弹性
                  .start();


        //当动画结束后，跳转到主界面
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
              startActivity(new Intent(SplashActivity.this,MainActivity.class));
                finish();
            }
        },1500);


    }

    /**
     * 当返回键被按下的时候执行
     */
    @Override
    public void onBackPressed() {
        //禁止当前界面退出，只需要不让父类的实现生效
//        super.onBackPressed();
    }
}
