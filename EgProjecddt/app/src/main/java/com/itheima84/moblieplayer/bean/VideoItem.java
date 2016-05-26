package com.itheima84.moblieplayer.bean;

import android.database.Cursor;
import android.provider.MediaStore.Video;

import java.io.Serializable;

/**
 * Created by lxj on 2016/5/12.
 */
public class VideoItem implements Serializable{
    public String title;
    public String path;
    public long duration;
    public long size;

    public static VideoItem fromCursor(Cursor cursor){
        VideoItem videoItem = new VideoItem();
        videoItem.title = cursor.getString(cursor.getColumnIndex(Video.Media.TITLE));
        videoItem.path = cursor.getString(cursor.getColumnIndex(Video.Media.DATA));
        videoItem.duration = cursor.getLong(cursor.getColumnIndex(Video.Media.DURATION));
        videoItem.size = cursor.getLong(cursor.getColumnIndex(Video.Media.SIZE));
        return videoItem;
    }
}
