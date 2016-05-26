package com.itheima84.moblieplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.activity.MusicPlayerActivity;
import com.itheima84.moblieplayer.bean.MusicItem;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by lxj on 2016/5/15.
 */
public class MusicPlayService extends Service {

    public static String ACTION_START_PLAY = "ACTION_START_PLAY";
    public static String ACTION_COMPLETION_PLAY = "ACTION_COMPLETION_PLAY";

    private static final String TAG = "MusicPlayService";
    private MusicPlayerProxy musicPlayerProxy;
    private int currentMusic;
    private ArrayList<MusicItem> musicList;
    private MediaPlayer mediaPlayer;

    public static final int ACTION_PRE = 1;//播放上一个的行为
    public static final int ACTION_NEXT = 2;//播放下一个的行为
    public static final int ACTION_NOTIFICATION = 3;//点击整个通知的行为

    //播放模式三种：顺序播放，随机播放，单曲循环
    public static final int MODE_ORDER = 1;//顺序播放
    public static final int MODE_RANDOM = 2;//随机播放
    public static final int MODE_SINGLE = 3;//单曲循环
    private int currentMode = MODE_ORDER;//默认是顺序播放
    private SharedPreferences sp;
    NotificationManager manager;
    @Override
    public void onCreate() {
        super.onCreate();
        musicPlayerProxy = new MusicPlayerProxy();
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //先从sp中获取播放模式
        sp = getSharedPreferences("mobileplayer.cfg", Context.MODE_PRIVATE);
        currentMode = sp.getInt("currentMode",currentMode);
    }

    /**
     * 只有以startService来开启服务才会执行
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null){

            int action = intent.getIntExtra("action", 0);
            if(action>0){
                //说明当前的开启service是从通知中开启的
                if(action==ACTION_PRE){
                    //播放上一个
                    musicPlayerProxy.playPre();
                }else if(action==ACTION_NEXT){
                    //播放下一个
                    musicPlayerProxy.playNext();
                }else if(action==ACTION_NOTIFICATION){
                    //如果是从整个通知点击进入的，则不用重新播放，
                    //但是Activiy是空白的，需要更新数据
                    notifyStartPlay();
                }
            }else {
                //没有则说明，是从播放界面开启的Service
                currentMusic = intent.getIntExtra("currentMusic", 0);
                musicList = (ArrayList<MusicItem>) intent.getSerializableExtra("musicList");
                //开始播放音乐
                musicPlayerProxy.playMusic();
            }
        }

        return START_STICKY;//如果服务被杀死，会自动重启
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return musicPlayerProxy;
    }


    public class MusicPlayerProxy extends Binder{
        /**
         * 播放音乐
         */
        public void playMusic() {
            if(mediaPlayer!=null){
                //先释放之前的资源
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            if(musicList==null)return;

            MusicItem musicItem = musicList.get(currentMusic);
            mediaPlayer =  new MediaPlayer();
            try {
                mediaPlayer.setDataSource(musicItem.path);
                mediaPlayer.setOnPreparedListener(mOnPreparedListener);
                mediaPlayer.setOnCompletionListener(mOnCompletionListener);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * 切换播放的方法
         */
        public void togglePlay() {
            if(mediaPlayer==null)return;
            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                //暂停的时候取消通知
                manager.cancel(1);
            }else {
                start();
            }
        }

        /**
         * 开始播放
         */
        public void start(){
            notifyStartPlay();
            mediaPlayer.start();
            sendNotification();//当开始播放的时候重新发送广播
        }

        /**
         * 判断是否正在播放
         * @return
         */
        public boolean isPlaying() {
            return mediaPlayer!=null && mediaPlayer.isPlaying();
        }

        /**
         * 获取当前的播放时间
         * @return
         */
        public int getCurrentPosition() {
            return mediaPlayer.getCurrentPosition();
        }

        /**
         * 获取总时间
         * @return
         */
        public long getDuration() {
            return mediaPlayer.getDuration();
        }

        /**
         * 改变进度
         * @param progress
         */
        public void seekTo(int progress) {
            mediaPlayer.seekTo(progress);
        }

        /**
         * 播放上一个
         */
        public void playPre() {
            if(currentMusic>0){
                currentMusic--;
                playMusic();
            }
        }
        /**
         * 播放下一个
         */
        public void playNext() {
            if(currentMusic<(musicList.size()-1)){
                currentMusic++;
                playMusic();
            }
        }

        /**
         * 当前播放的是否是第一首
         * @return
         */
        public boolean isPlayingFirst() {
            return currentMusic==0;
        }
        /**
         * 当前播放的是否是最后一首
         * @return
         */
        public boolean isPlayingLast() {
            return currentMusic==(musicList.size()-1);
        }

        /**
         * 切换播放模式
         */
        public void switchPlayMode() {
            switch (currentMode){
                case MODE_ORDER:
                    currentMode = MODE_RANDOM;
                    break;
                case MODE_RANDOM:
                    currentMode = MODE_SINGLE;
                    break;
                case MODE_SINGLE:
                    currentMode = MODE_ORDER;
                    break;
            }

            //保存播放模式到sp中
            sp.edit().putInt("currentMode",currentMode).commit();
        }

        public int getCurrentMode() {
            return currentMode;
        }
    }
    MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start();
            //开始播放，通知Activity进行UI更新
            notifyStartPlay();
            //开始播放，发送通知
            sendNotification();
        }
    };

    private void sendNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        MusicItem musicItem = musicList.get(currentMusic);
        builder.setSmallIcon(R.mipmap.icon_notification)//设置状态栏出现的小图标
                .setTicker("正在播放："+musicItem.title)//设置状态栏出现的文字
                .setContentTitle(musicItem.title)
                .setContentText(musicItem.artist)
//                .setContentIntent(getPendingIntent())//设置通知被点击的时候要做的事情
                .setContent(getRemoteView())
                .setWhen(System.currentTimeMillis())//发的时候立即显示
                .setOngoing(true);//通知不能被手动移除
        manager.notify(1,builder.build());
    }

    /**
     * 获取自定义VIew的通知
     * @return
     */
    public RemoteViews getRemoteView() {
        MusicItem musicItem = musicList.get(currentMusic);
        RemoteViews remoteView = new RemoteViews(getPackageName(),R.layout.notification_music);
        //动态的设置通知中的标题和文本内容
        remoteView.setTextViewText(R.id.notification_title,musicItem.title);
        remoteView.setTextViewText(R.id.notification_artist,musicItem.artist);
        //给通知中的按钮设置点击事件
        remoteView.setOnClickPendingIntent(R.id.notification_pre,getPrePendingIntent());
        remoteView.setOnClickPendingIntent(R.id.notification_next,getNextPendingIntent());
        remoteView.setOnClickPendingIntent(R.id.notification,getNotificationPendingIntent());
        return remoteView;
    }

    /**
     * 获取点击通知中上一个按钮要做的意图
     * @return
     */
    public PendingIntent getPrePendingIntent() {
        Intent intent = new Intent(this,MusicPlayService.class);
        intent.putExtra("action",ACTION_PRE);
        PendingIntent pendingIntent = PendingIntent.getService(this,1,intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }
    /**
     * 获取点击通知中下一个按钮要做的意图
     * @return
     */
    public PendingIntent getNextPendingIntent() {
        Intent intent = new Intent(this,MusicPlayService.class);
        intent.putExtra("action",ACTION_NEXT);
        PendingIntent pendingIntent = PendingIntent.getService(this,2,intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }
    /**
     * 获取点击整个通知要做的意图
     * @return
     */
    public PendingIntent getNotificationPendingIntent() {
        Intent intent = new Intent(this, MusicPlayerActivity.class);
        intent.putExtra("action",ACTION_NOTIFICATION);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,3,intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }


    MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            //播放完成发送广播
            notifyCompletePlay();

            //播放完成了，要根据播放模式播放下一首
            autoPlayByMode();
        }
    };

    /**
     * 根据播放模式自动播放下一首
     */
    private void autoPlayByMode() {
        switch (currentMode){
            case MODE_ORDER://顺序
                //应该是播放下一首,并且是如果最后一首播放，应该从头继续播放
                if(musicPlayerProxy.isPlayingLast()){
                    currentMusic = 0;//将索引置为0，从头播放
                    musicPlayerProxy.playMusic();
                }else {
                    musicPlayerProxy.playNext();
                }
                break;
            case MODE_RANDOM://随机
                //应该是随机选择一首播放
                currentMusic = new Random().nextInt(musicList.size());
                musicPlayerProxy.playMusic();
                break;
            case MODE_SINGLE://单曲
                //应该再来一遍
                musicPlayerProxy.start();
                break;
        }
    }

    /**
     * 发送播放完成的广播
     */
    private void notifyCompletePlay() {
        Intent intent = new Intent(ACTION_COMPLETION_PLAY);
        intent.putExtra("musicItem",musicList.get(currentMusic));
        sendBroadcast(intent);
    }

    /**
     * 通知开始播放音乐了
     */
    private void notifyStartPlay() {
        Intent intent = new Intent(ACTION_START_PLAY);
        intent.putExtra("musicItem",musicList.get(currentMusic));
        sendBroadcast(intent);
    }
}
