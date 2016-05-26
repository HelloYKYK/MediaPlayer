package com.itheima84.moblieplayer.lyric;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by lxj on 2016/5/16.
 * 歌词模块的加载类，负责加载歌词文件和解析歌词文件
 */
public class LyricLoader {
    private static String DIR = Environment.getExternalStorageDirectory()+"/test/audio";
    private static final String TAG = "LyricLoader";
    public static ArrayList<Lyric> parseLyric(File file){
        if(file==null || !file.exists())return null;

        ArrayList<Lyric> list = new ArrayList<>();
        try {
            //1.读取文件
            InputStreamReader isr = new InputStreamReader(new FileInputStream(file),"gbk");
            BufferedReader reader = new BufferedReader(isr);
            String line;
            while((line=reader.readLine())!=null){
                //2.将每行歌词解析为一个Lyric对象
                //以]分割,得到[01:48.07	[00:41.00	你哭着对我说 童话里都是骗人的
                String[] arr = line.split("\\]");
                for (int i=0;i<arr.length-1;i++){
                    Lyric lyric = new Lyric();
                    lyric.setContent(arr[arr.length-1]);//设置歌词内容
                    lyric.setStartPoint(formatStartPoint(arr[i]));
                    //将lyric放入list中
                    list.add(lyric);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //3.对歌词排序，保证时间点大的歌词在后面
        Collections.sort(list);

        return list;
    }

    /**
     * 格式化歌曲的时间
     * @param s
     * @return
     */
    private static long formatStartPoint(String s) {
        //[01:48.07
        s = s.substring(1);//得到01:48.07
        //1.以：分割，得到01   48.07
        String[] arr = s.split("\\:");
        //2.再以.分割，得到48   07
        String[] arr2 = arr[1].split("\\.");

        //转换分钟，秒
        long minute = Integer.parseInt(arr[0]);
        long second = Integer.parseInt(arr2[0]);
        long mills = Integer.parseInt(arr2[1]);//代表10毫秒
        return minute*60*1000 + second*1000 + mills*10;
    }

    /**
     * 加载歌词文件，在真实开发应该是通过一个接口去下载歌词文件，
     * 但是我们只能模拟了，根据歌曲名称加载文件
     * @return
     */
    public static File loadLyricFile(String musicName){
        File file = new File(DIR,musicName+".lrc");
        if(!file.exists()){
            file = new File(DIR,musicName+".txt");
        }
        return file;
    }
}
