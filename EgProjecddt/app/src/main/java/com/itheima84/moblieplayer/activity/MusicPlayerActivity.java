package com.itheima84.moblieplayer.activity;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.base.BaseActivity;
import com.itheima84.moblieplayer.bean.MusicItem;
import com.itheima84.moblieplayer.lyric.LyricLoader;
import com.itheima84.moblieplayer.lyric.LyricView;
import com.itheima84.moblieplayer.service.MusicPlayService;
import com.itheima84.moblieplayer.util.Utils;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by lxj on 2016/5/15.
 */
public class MusicPlayerActivity extends BaseActivity {
    private static final String TAG = "MusicPlayerActivity";
    @Bind(R.id.btn_bakc)
    ImageView btnBakc;
    @Bind(R.id.tv_music_title)
    TextView tvMusicTitle;
    @Bind(R.id.iv_anim)
    ImageView ivAnim;
    @Bind(R.id.tv_artist)
    TextView tvArtist;
    @Bind(R.id.tv_play_time)
    TextView tvPlayTime;
    @Bind(R.id.sb_music)
    SeekBar sbMusic;
    @Bind(R.id.btn_playmode)
    ImageView btnPlaymode;
    @Bind(R.id.btn_music_pre)
    ImageView btnMusicPre;
    @Bind(R.id.btn_music_play)
    ImageView btnMusicPlay;
    @Bind(R.id.btn_music_next)
    ImageView btnMusicNext;
    @Bind(R.id.lyricView)
    LyricView lyricView;
    private MusicPlayService.MusicPlayerProxy musicPlayProxy;
    private MusicReceiver musicReceiver;

    private final int MSG_UPDATE_PROGRESS = 1;//更新播放进度
    private final int MSG_ROLL_LYRIC = 2;//更新歌词的滚动
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_UPDATE_PROGRESS:
                    updatePlayProgress();
                    break;
                case MSG_ROLL_LYRIC:
                    loopRollLyric();
                    break;
            }
        }
    };

    @Override
    public int getLayoutId() {
        return R.layout.activity_music_player;
    }

    @Override
    public void setListener() {
        sbMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    //改变音乐播放的进度
                    musicPlayProxy.seekTo(progress);
                    tvPlayTime.setText( Utils.formatDuration(musicPlayProxy.getCurrentPosition())
                            +"/"+Utils.formatDuration(musicPlayProxy.getDuration()));
                    //拖动的时候也去手动滚动歌词
                    lyricView.startRoll(progress,musicPlayProxy.getDuration());
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void initData() {
        playAnim();
        registerMusicReceiver();

        Intent intent = new Intent(this, MusicPlayService.class);
        int action = getIntent().getIntExtra("action", 0);
        if(action==MusicPlayService.ACTION_NOTIFICATION){
            //从通知中点击进入的,我们将action的标识再次传入Service中
            intent.putExtra("action",action);
        }else {
            //从列表中点击进入的
            int currentMusic = getIntent().getIntExtra("currentMusic", 0);
            ArrayList<MusicItem> musicList = (ArrayList<MusicItem>) getIntent().getSerializableExtra("musicList");
            //将currentMusic和musicList数据传递给Service
            intent.putExtra("currentMusic", currentMusic);
            intent.putExtra("musicList", musicList);
        }
        MusicServiceConn musicServiceConn = new MusicServiceConn();
        bindService(intent, musicServiceConn, Service.BIND_AUTO_CREATE);
        //目前的问题是通过bindService方法开启服务，无法给Service传递数据
        //解决方法是调用startServic再次开启服务;
        startService(intent);//由于Service只会create一次，所以并没有什么问题

    }

    /**
     * 播放帧动画
     */
    private void playAnim() {
        AnimationDrawable drawable = (AnimationDrawable) ivAnim.getBackground();
        drawable.start();
    }

    /**
     * 注册广播接受者
     */
    private void registerMusicReceiver() {
        musicReceiver = new MusicReceiver();
        IntentFilter filter = new IntentFilter();
        //添加过滤行为
        filter.addAction(MusicPlayService.ACTION_START_PLAY);
        filter.addAction(MusicPlayService.ACTION_COMPLETION_PLAY);

        registerReceiver(musicReceiver,filter);
    }

    @OnClick({R.id.btn_bakc, R.id.btn_playmode, R.id.btn_music_pre, R.id.btn_music_play, R.id.btn_music_next})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_bakc:
                finish();
                break;
            case R.id.btn_playmode://切换播放模式
                musicPlayProxy.switchPlayMode();
                updatePlayModeBg();
                break;
            case R.id.btn_music_pre:
                //如果当前是第0个了，那么就提示一下
                if(musicPlayProxy.isPlayingFirst()){
                    Toast.makeText(MusicPlayerActivity.this, "当前是第1首了", Toast.LENGTH_SHORT).show();
                    return;
                }
                musicPlayProxy.playPre();
                break;
            case R.id.btn_music_play:
                musicPlayProxy.togglePlay();
                btnMusicPlay.setBackgroundResource(musicPlayProxy.isPlaying()?
                        R.drawable.selector_btn_audio_pause:
                        R.drawable.selector_btn_audio_play);
                if(!musicPlayProxy.isPlaying()){
                    //应该移除更新播放进度的msg
                    handler.removeMessages(MSG_UPDATE_PROGRESS);
                    //如果暂停则也移除更新歌词的消息
                    handler.removeMessages(MSG_ROLL_LYRIC);
                }
                break;
            case R.id.btn_music_next:
                //如果当前是第0个了，那么就提示一下
                if(musicPlayProxy.isPlayingLast()){
                    Toast.makeText(MusicPlayerActivity.this, "当前是最后1首了", Toast.LENGTH_SHORT).show();
                    return;
                }
                musicPlayProxy.playNext();
                break;
        }
    }

    /**
     * 更新播放模式的背景图片
     */
    private void updatePlayModeBg() {
        switch (musicPlayProxy.getCurrentMode()){
            case MusicPlayService.MODE_ORDER:
                btnPlaymode.setBackgroundResource(R.drawable.selector_btn_playmode_order);
                break;
            case MusicPlayService.MODE_RANDOM:
                btnPlaymode.setBackgroundResource(R.drawable.selector_btn_playmode_random);
                break;
            case MusicPlayService.MODE_SINGLE:
                btnPlaymode.setBackgroundResource(R.drawable.selector_btn_playmode_single);
                break;
        }
    }


    class MusicServiceConn implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicPlayProxy = (MusicPlayService.MusicPlayerProxy) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    class MusicReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(MusicPlayService.ACTION_START_PLAY.equals(action)){
                //说明是开始播放音乐了，那就要更新UI了
                MusicItem musicItem = (MusicItem) intent.getSerializableExtra("musicItem");

                updateUI(musicItem);//初始化UI
                updatePlayProgress();//开始更新播放进度

                //设置歌词列表
                File file = LyricLoader.loadLyricFile(musicItem.title);
                lyricView.setLyricList(LyricLoader.parseLyric(file));
                //在此处开始滚动歌词
                loopRollLyric();

            }else if(MusicPlayService.ACTION_COMPLETION_PLAY.equals(action)){
                //说明是播放音乐完成，那就要更新UI了
                MusicItem musicItem = (MusicItem) intent.getSerializableExtra("musicItem");
                btnMusicPlay.setBackgroundResource(R.drawable.selector_btn_audio_play);
                handler.removeMessages(MSG_UPDATE_PROGRESS);

                //移除歌曲滚动的msg
                handler.removeMessages(MSG_ROLL_LYRIC);

                tvPlayTime.setText(Utils.formatDuration(musicItem.duration)+"/"+
                        Utils.formatDuration(musicItem.duration));
            }
        }
    }

    /**
     * 根据歌曲播放的位置，循环的滚动歌词
     */
    private void loopRollLyric() {
        lyricView.startRoll(musicPlayProxy.getCurrentPosition(),musicPlayProxy.getDuration());
        handler.sendEmptyMessageDelayed(MSG_ROLL_LYRIC,10);
    }

    /**
     * 更新播放进度
     */
    private void updatePlayProgress() {
        sbMusic.setProgress(musicPlayProxy.getCurrentPosition());
        tvPlayTime.setText( Utils.formatDuration(musicPlayProxy.getCurrentPosition())
                +"/"+Utils.formatDuration(musicPlayProxy.getDuration()));
        handler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS,100);
    }


    /**
     * 更新标题和艺术家
     */
    private void updateUI(MusicItem musicItem) {
        tvMusicTitle.setText(musicItem.title);
        tvArtist.setText(musicItem.artist);
        //初始化时间
        tvPlayTime.setText("00:00/"+ Utils.formatDuration(musicItem.duration));
        //初始进度条
        sbMusic.setMax((int) musicItem.duration);
        //给播放按钮设置暂停图片
        btnMusicPlay.setBackgroundResource(R.drawable.selector_btn_audio_pause);
        //更新播放模式的图片
        updatePlayModeBg();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
