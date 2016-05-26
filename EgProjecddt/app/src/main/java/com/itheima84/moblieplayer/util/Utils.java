package com.itheima84.moblieplayer.util;

import android.database.Cursor;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lxj on 2016/5/12.
 */
public class Utils {
    private static final String TAG = "Utils";
    public static void printCursor(Cursor c){
        if(c==null)return;

        //打印有多少条记录
        Log.e(TAG, "共"+c.getCount()+"条记录" );
        //打印每条记录中的所有列的数据
        while (c.moveToNext()){
            Log.e(TAG, "----------------------" );
            //获取当前条记录的列数
            int columnCount = c.getColumnCount();
            for (int i=0;i<columnCount;i++){
                String columnName = c.getColumnName(i);//获取列名
                String columnValue = c.getString(i);//获取列值
                Log.e(TAG, columnName+ " => " +columnValue);
            }
        }
    }

    /**
     * 将long类型的时间转为01:22:33格式的
     * @param duration
     * @return
     */
    public static String formatDuration(long duration){
        int HOUR = 60*60*1000;
        int MINUTE = 60*1000;
        int SECOND = 1000;

        //1.先计算有多少小时
        int hour = (int) (duration / HOUR);//计算有多少小时
        long remain = duration%HOUR;//算完小时剩余的时间
        //2.再计算有多少分钟
        int minute = (int) (remain / MINUTE);//计算有多少分钟
        remain = remain%MINUTE;//得到算完分钟剩余的时间
        //3.最后计算有多少秒
        int second = (int) (remain / SECOND);

        //4.组装成01:22:33格式，如果不足一小时则是01:22
        if(hour==0){
            //说明不足一个小时，01:22
            return String.format("%02d:%02d",minute,second);
        }else {
            //说明超过一个小时,01:22:33
            return String.format("%02d:%02d:%02d",hour,minute,second);
        }
    }

    public static String formatSystemTime(){
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return  format.format(new Date());
    }

    /**
     * 去电音乐名称后的.mp3
     * @param musicName
     * @return
     */
    public static  String formatName(String musicName){
        int mp3Index = musicName.lastIndexOf(".");
        return musicName.substring(0,mp3Index);
    }
}
