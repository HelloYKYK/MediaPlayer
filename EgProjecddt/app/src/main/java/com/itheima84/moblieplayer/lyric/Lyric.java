package com.itheima84.moblieplayer.lyric;

/**
 * Created by lxj on 2016/5/16.
 * 封装的一行歌词。有个属性，就是起始点和内容
 */
public class Lyric implements Comparable<Lyric>{
    public Lyric(String content, long startPoint) {
        this.content = content;
        this.startPoint = startPoint;
    }

    public Lyric(){}

    private String content;
    private long startPoint;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(long startPoint) {
        this.startPoint = startPoint;
    }

    @Override
    public int compareTo(Lyric another) {
        return (int) (this.startPoint-another.getStartPoint());
    }
}
