package com.itheima84.moblieplayer.bean;

import android.database.Cursor;
import android.provider.MediaStore;

import com.itheima84.moblieplayer.util.Utils;

import java.io.Serializable;

/**
 * Created by lxj on 2016/5/15.
 */
public class MusicItem implements Serializable{
    public String title;
    public String path;
    public long duration;
    public long size;
    public String artist;//艺术家

    public static MusicItem fromCursor(Cursor cursor){
        MusicItem musicItem =  new MusicItem();
        String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
        musicItem.title = Utils.formatName(title);
        musicItem.path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        musicItem.duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        musicItem.size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));
        musicItem.artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        return musicItem;
    }
}
