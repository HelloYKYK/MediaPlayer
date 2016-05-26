package com.itheima84.moblieplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.base.BaseActivity;
import com.itheima84.moblieplayer.bean.VideoItem;
import com.itheima84.moblieplayer.util.Utils;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.VideoView;

/**
 * Created by lxj on 2016/5/12.
 */
public class VideoPlayerActivity extends BaseActivity {
    private static final String TAG = "VideoPlayerActivity";
    @Bind(R.id.videoView)
    VideoView videoView;
    @Bind(R.id.tv_title)
    TextView tvTitle;
    @Bind(R.id.iv_battery)
    ImageView ivBattery;
    @Bind(R.id.tv_system_time)
    TextView tvSystemTime;
    @Bind(R.id.btn_voice)
    Button btnVoice;
    @Bind(R.id.sb_voice)
    SeekBar sbVoice;
    @Bind(R.id.fl_overlay)
    FrameLayout flOverlay;
    @Bind(R.id.sb_video)
    SeekBar sbVideo;
    @Bind(R.id.tv_duration)
    TextView tvDuration;
    @Bind(R.id.btn_exit)
    Button btnExit;
    @Bind(R.id.btn_pre)
    Button btnPre;
    @Bind(R.id.btn_play)
    Button btnPlay;
    @Bind(R.id.btn_next)
    Button btnNext;
    @Bind(R.id.btn_screen)
    Button btnScreen;
    @Bind(R.id.tv_progress)
    TextView tvProgress;
    @Bind(R.id.ll_top)
    LinearLayout ll_top;
    @Bind(R.id.fl_bottom)
    FrameLayout fl_bottom;

    private int currentVideo;
    private ArrayList<VideoItem> videoList;

    private final int MSG_UPDATE_SYSTEM_TIME = 1;//更新系统时间
    private final int MSG_UPDATE_VIDEO = 2;//更新视频播放进度
    private final int MSG_HIDE_CONTROL = 3;//延时隐藏控制面板

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE_SYSTEM_TIME:
                    updateSystemTime();
                    break;
                case MSG_UPDATE_VIDEO:
                    updateVideoTimeAndProgress();
                    break;
                case MSG_HIDE_CONTROL:
                    animHideControl();
                    break;
            }
        }
    };
    private BatterChangeReceiver batterChangeReceiver;
    private AudioManager audioManager;
    private int currentVolume;
    private int maxVolume;
    private boolean isMute = false;//是否是静音
    private int screenWidth;

    @Override
    public int getLayoutId() {
        return R.layout.activity_video_player;
    }

    @Override
    public void setListener() {
        //设置音量进度改变的监听器
        sbVoice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * 当前进度改变的时候
             * @param seekBar
             * @param progress
             * @param fromUser
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //fromUser：表示是否是用户手指拖动改变的
                //setProgress并不让它影响currentVolume的值
                if (fromUser) {
                    currentVolume = progress;
                    updateVolume();
                }
            }

            /**
             * 当手指按下
             * @param seekBar
             */
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isMute = false;//只要手指开始滑动Seekbar就变为非静音模式
                //开始滑动的时候应该移除消息
                handler.removeMessages(MSG_HIDE_CONTROL);
            }

            /**
             * 当手指抬起
             * @param seekBar
             */
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //停止滑动的时候，再发送消息
                handler.sendEmptyMessageDelayed(MSG_HIDE_CONTROL,4000);
            }
        });

        //设置视频播放完成的监听器
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //视频播放完成了，应该去移除更新视频进度的消息
                handler.removeMessages(MSG_UPDATE_VIDEO);

                //播放完成，应该给播放按钮设置播放的背景图片
                btnPlay.setBackgroundResource(R.drawable.selector_btn_play);

                //由于更新视频的播放进度有延时，并且我们把延时消息移除了，所以需要手动设置时间
                tvProgress.setText(Utils.formatDuration(videoView.getDuration()));
                sbVideo.setProgress((int)videoView.getDuration());
            }
        });

        //给视频的SeekBar添加改变的监听器
        sbVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    videoView.seekTo(progress);
                    //手动更新视频播放时间
                    tvProgress.setText(Utils.formatDuration(videoView.getCurrentPosition()));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //开始滑动的时候应该移除消息
                handler.removeMessages(MSG_HIDE_CONTROL);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //停止滑动的时候，再发送消息
                handler.sendEmptyMessageDelayed(MSG_HIDE_CONTROL,4000);
            }
        });

        //设置缓冲进度监听
        videoView.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                Log.e(TAG, "onBufferingUpdate: percent: "+percent );
                //percent: 0-100
                //计算缓冲的进度
                int bufferProgress = (int) (percent*videoView.getDuration()/100f);
                sbVideo.setSecondaryProgress(bufferProgress);
            }
        });

        //设置视频播放卡顿的监听器
        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    switch (what){
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START://表示开始卡顿
                            Toast.makeText(VideoPlayerActivity.this,"开始卡顿",Toast.LENGTH_SHORT).show();
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END://表示卡顿结束
                            Toast.makeText(VideoPlayerActivity.this,"卡顿结束了，大大大",Toast.LENGTH_SHORT).show();
                            break;
                    }
                return false;
            }
        });
        //设置播放失败的处理
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what){
                    //视频文件损坏
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        Toast.makeText(VideoPlayerActivity.this,"视频文件错误",Toast.LENGTH_SHORT).show();
                        finish();//结束当前界面
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void initData() {
        //初始化屏幕的宽度
        screenWidth = getWindowManager().getDefaultDisplay().getWidth();


        //显示系统时间
        updateSystemTime();
        //注册电量变化的广播接收者
        registerBatteryChangeReceiver();
        //初始化系统音量
        initVolume();
        //刚进来则隐藏控制面板
        hideControlLayout();

        //判断Vitamio有没有初始化
        if(!Vitamio.isInitialized(getApplicationContext())){
            return;
        }


        //获取Intent，判断是否是第三方文件发起的请求
        Uri uri = getIntent().getData();
        if(uri!=null){
            //说明是第三方传的
            videoView.setVideoURI(uri);
            //设置准备的监听器
            videoView.setOnPreparedListener(mOnPreparedListener);
            //设置标题
            tvTitle.setText(uri.getPath());

            //禁用上一个和下一个
            btnPre.setEnabled(false);
            btnNext.setEnabled(false);


        }else {
            //从视频列表进入的
            //1.取出当前的位置和视频列表数据
            currentVideo = getIntent().getIntExtra("currentVideo", 0);
            videoList = ((ArrayList<VideoItem>) getIntent().getSerializableExtra("videoList"));

            //2.使用videoView播放视频
            playVideo();
        }

    }

    /**
     * 隐藏控制面板
     */
    private void hideControlLayout() {
        ll_top.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ll_top.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                ll_top.setTranslationY(-ll_top.getHeight());
            }
        });
        fl_bottom.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                fl_bottom.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                fl_bottom.setTranslationY(fl_bottom.getHeight());
            }
        });
    }

    /**
     * 初始化系统的音量
     */
    private void initVolume() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //获取music类型的音量,就是获取当前音量
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //获取最大的音量，系统的最大音量13
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

//        Log.e(TAG, "maxVolume: "+maxVolume);
        sbVoice.setMax(maxVolume);//设置进度条的最大值
        sbVoice.setProgress(currentVolume);//设置进度条的当前进度

    }

    /**
     * 更新系统音量
     */
    private void updateVolume() {
        if (isMute) {
            //将系统音量置为0，并且将SeekBar的进度置为0
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            sbVoice.setProgress(0);
        } else {
            //非静音模式，应该恢复音量
            //改变系统音量
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            sbVoice.setProgress(currentVolume);//恢复进度
        }
    }

    /**
     * 注册电量变化的广播接收者
     */
    private void registerBatteryChangeReceiver() {
        batterChangeReceiver = new BatterChangeReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batterChangeReceiver, filter);
    }

    /**
     * 更新系统时间
     */
    private void updateSystemTime() {
        tvSystemTime.setText(Utils.formatSystemTime());
        //定时更新，那么需要定时任务
        handler.sendEmptyMessageDelayed(MSG_UPDATE_SYSTEM_TIME, 1000);
    }

    /**
     * 播放视频
     */
    private void playVideo() {
        //判断当前播放的是第一个，然后决定让上一个和下一个按钮禁用
//        if(currentVideo==0){
//            //说明当前播放的是首个视频，禁用上一个按钮
//            btnPre.setEnabled(false);
//        }else if(currentVideo==(videoList.size()-1)){
//            //说明是最后一个视频，那么禁用下一个
//            btnNext.setEnabled(false);
//        }else {
//            //应该恢复可用
//            btnPre.setEnabled(true);
//            btnNext.setEnabled(true);
//        }

        //高级写法
        btnPre.setEnabled(currentVideo!=0);
        btnNext.setEnabled(currentVideo!=(videoList.size()-1));

        final VideoItem videoItem = videoList.get(currentVideo);

        //显示视频标题
        tvTitle.setText(videoItem.title);

        videoView.setVideoPath(videoItem.path);//设置视频路径,mediaPlayer进入Init状态，并且开始进行Prepare

        //设置准备完成的监听器，不推荐直接调用start方法
        videoView.setOnPreparedListener(mOnPreparedListener);

//      videoView.setMediaController(new MediaController(this));
    }

    MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            videoView.start();//MediaPlayer变成started状态

            //给播放按钮设置暂停的背景图片
            btnPlay.setBackgroundResource(R.drawable.selector_btn_pause);

            //更新视频的时间和进度
            updateVideoTimeAndProgress();
        }
    };

    /**
     * 更新视频的时间和进度
     */
    private void updateVideoTimeAndProgress() {
        //更新播放时间
        tvProgress.setText(Utils.formatDuration(videoView.getCurrentPosition()));
        tvDuration.setText(Utils.formatDuration(videoView.getDuration()));
        //更新进度条
        sbVideo.setMax((int) videoView.getDuration());
        sbVideo.setProgress((int) videoView.getCurrentPosition());

        handler.sendEmptyMessageDelayed(MSG_UPDATE_VIDEO,200);
    }

    private float downX, downY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                toggleControlLayout();
                break;
            case MotionEvent.ACTION_MOVE:
                //1.计算移动的坐标
                float moveX = event.getX();
                float moveY = event.getY();
                //2.计算移动的距离
                float deltaY = moveY - downY;

                if (moveX < screenWidth / 2) {
                    //改变亮度
                    changeLight(deltaY);
                } else {
                    //改变音量
                    changeVolumeByTouch(deltaY);
                }

                downX = moveX;
                downY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                //延时隐藏控制面板
                handler.sendEmptyMessageDelayed(MSG_HIDE_CONTROL,5000);
                break;
        }
        return super.onTouchEvent(event);
    }
    private boolean isShowControl = false;//默认是隐藏的
    private void toggleControlLayout() {
        //如果是隐藏的，则显示出来，否则就隐藏
        if(isShowControl){
            animHideControl();
        }else {
            //显示出来
            animShowControl();
        }
    }

    private void animShowControl() {
        isShowControl = true;
        ViewCompat.animate(ll_top).translationY(0).setDuration(400).start();
        ViewCompat.animate(fl_bottom).translationY(0).setDuration(400).start();
    }

    private void animHideControl() {
        //先移除之前的消息
        handler.removeMessages(MSG_HIDE_CONTROL);
        isShowControl = false;
        ViewCompat.animate(ll_top).translationY(-ll_top.getHeight()).setDuration(400).start();
        ViewCompat.animate(fl_bottom).translationY(fl_bottom.getHeight()).setDuration(400).start();
    }

    /**
     * 根据手指触摸改变音量
     *
     * @param deltaY
     */
    private void changeVolumeByTouch(float deltaY) {
        isMute = false;//只要是手指触摸改变音量，则强制变为非静音模式

        if (deltaY > 0) {
            //向下，应该缩小
            currentVolume -= 1;//每次递减1个音量,0-15
            if (currentVolume < 0) currentVolume = 0;
        } else {
            //向上，应该增大音量
            currentVolume += 1;//每次递增1个音量,0-15
            if (currentVolume > maxVolume) currentVolume = maxVolume;
        }
        updateVolume();
    }

    /**
     * 改变播放界面的亮度
     *
     * @param deltaY
     */
    private void changeLight(float deltaY) {
        //说明在左边，需要改变明暗
        float aplha = flOverlay.getAlpha();
        //往下是变暗
        if (deltaY > 0) {
            aplha += 0.02;
            if (aplha > 0.8) aplha = 0.8f;
            flOverlay.setAlpha(aplha);
        } else if (deltaY < 0) {
            //变亮
            aplha -= 0.02;
            if (aplha < 0) aplha = 0f;
            flOverlay.setAlpha(aplha);
        }
    }

    @OnClick({R.id.btn_voice,R.id.btn_exit, R.id.btn_pre, R.id.btn_play, R.id.btn_next, R.id.btn_screen})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_voice:
                isMute = !isMute;
                updateVolume();
                break;
            case R.id.btn_exit:
                //退出播放界面
                finish();
                break;
            case R.id.btn_pre://播放上一个
                playPre();
                break;
            case R.id.btn_play:
                togglePlay();
                break;
            case R.id.btn_next:
                playNext();//播放下一个
                break;
            case R.id.btn_screen:
               /* videoView.toggleFullScreen();
                btnScreen.setBackgroundResource(videoView.isFullScreen()?
                R.drawable.selector_btn_defaultscreen:
                R.drawable.selector_btn_fullscreen);*/
                break;
        }
    }

    /**
     * 播放下一个
     */
    private void playNext() {
        if(currentVideo<(videoList.size()-1)){
            currentVideo++;
            playVideo();
        }
    }

    /**
     * 播放上一个视频
     */
    private void playPre() {
        if(currentVideo>0){
            currentVideo--;
            playVideo();
        }
    }

    /**
     * 切换播放的方法
     */
    private void togglePlay() {
        if(videoView.isPlaying()){
            //暂停播放
            videoView.pause();
            btnPlay.setBackgroundResource(R.drawable.selector_btn_play);
        }else {
            //开始播放
            updateVideoTimeAndProgress();//调用更新视频进度和播放时间的方法
            videoView.start();
            btnPlay.setBackgroundResource(R.drawable.selector_btn_pause);
        }
    }


    /**
     * 电量变化的接收者
     */
    class BatterChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //1.获取电量等级,level表示当前电量等级，0-100
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            //2.根据电量的等级去显示不同的电量图片
            showBatteryByLevel(level);
        }
    }

    /**
     * 根据电量的等级显示不同的图片
     *
     * @param level
     */
    private void showBatteryByLevel(int level) {
        if (level == 0) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_0);
        } else if (level > 0 && level < 10) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_10);
        } else if (level >= 10 && level < 20) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_20);
        } else if (level >= 20 && level < 40) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_40);
        } else if (level >= 40 && level < 60) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_60);
        } else if (level >= 60 && level < 80) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_80);
        } else if (level >= 80) {
            ivBattery.setBackgroundResource(R.mipmap.ic_battery_100);
        }
    }

    /**
     * 点击物理按键都会执行该方法
     *
     * @param keyCode 所按下物理键的唯一标识
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //如果按下的是音量向上或者向下的键
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            //获取音量，更新SeekBar
            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            sbVoice.setProgress(currentVolume);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batterChangeReceiver);
        handler.removeCallbacksAndMessages(null);//移除消息
    }
}
