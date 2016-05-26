package com.itheima84.moblieplayer.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.adapter.MainAdapter;
import com.itheima84.moblieplayer.base.BaseActivity;
import com.itheima84.moblieplayer.fragment.ListFragment;
import com.itheima84.moblieplayer.util.Constant;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
//
    @Bind(R.id.tv_video)
    TextView tvVideo;
    @Bind(R.id.tv_music)
    TextView tvMusic;
    @Bind(R.id.indicator)
    View indicator;
    @Bind(R.id.viewPager)
    ViewPager viewPager;
    private ArrayList<Fragment> fragments;

    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void setListener() {
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            /**
             * 当滑动ViewPager的时候会执行该方法
             * @param position 当前选中的位置
             * @param positionOffset 表示当前手指滑动距离的百分比
             * @param positionOffsetPixels 表示当前手指滑动的距离，单位是像素
             */
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//                Log.e(TAG, "onPageScrolled:  position: "+position  +"  positionOffset:"+positionOffset
//                +"  positionOffsetPixels: "+positionOffsetPixels);

                //1.计算线需要滚动到的距离
                float targetX = indicator.getWidth()*position + positionOffsetPixels/fragments.size();
                //2.让线滚动到指定的距离
                ViewCompat.setTranslationX(indicator,targetX);
            }
            @Override
            public void onPageSelected(int position) {
                updateTitle();
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });


    }


    /**
     * 更新标题的状态
     */
    private void updateTitle() {
        //1.获取当前ViewPager选择的页
        int currentItem = viewPager.getCurrentItem();

        //2.根据当前页来设置标题的颜色
        tvVideo.setSelected(currentItem==0);
        tvMusic.setSelected(currentItem==1);

        //3.根据当前页来让标题缩放
        ViewCompat.animate(tvVideo)
                .scaleX(currentItem==0?1f:0.8f)
                .scaleY(currentItem==0?1f:0.8f)
                .setDuration(400).start();
        ViewCompat.animate(tvMusic)
                .scaleX(currentItem==1?1f:0.8f)
                .scaleY(currentItem==1?1f:0.8f)
                .setDuration(400).start();




    }

    @Override
    public void initData() {
        fragments = new ArrayList<>();
        fragments.add(ListFragment.newInstance(getBundle(Constant.VIDEO)));//添加fragment
        fragments.add(ListFragment.newInstance(getBundle(Constant.MUSIC)));//添加fragment

        //1.给ViewPager填充Fragment
        MainAdapter mainAdapter = new MainAdapter(getSupportFragmentManager(), fragments);
        viewPager.setAdapter(mainAdapter);


        //刚进来需要更新标题
        updateTitle();

        //初始化指示线的宽度
        initIndicatorLine();

    }

    private Bundle getBundle(int type) {
        Bundle bundle = new Bundle();
        bundle.putInt("type",type);
        return bundle;
    }

    private void initIndicatorLine() {
        //1.计算线的宽度
        int width = getWindowManager().getDefaultDisplay().getWidth()/fragments.size();
        //2.将width设置给indicator
        ViewGroup.LayoutParams params = indicator.getLayoutParams();
        params.width = width;
        indicator.setLayoutParams(params);
    }


    @OnClick({R.id.tv_video, R.id.tv_music})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_video:
                viewPager.setCurrentItem(0);
                break;
            case R.id.tv_music:
                viewPager.setCurrentItem(1);
                break;
        }
    }
}
