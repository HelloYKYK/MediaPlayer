package com.itheima84.moblieplayer.lyric;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by lxj on 2016/5/16.
 */
public class LyricView extends View {
    private Paint paint;
    private int COLOR_DEFAULT = Color.WHITE;//普通歌词的颜色
    private int COLOR_LIGHT = Color.GREEN;//高亮歌词的颜色
    private int SIZE_DEFAULT = 14;//普通歌词的字体大小
    private int SIZE_LIGHT = 17;//高亮歌词的字体大小

    private ArrayList<Lyric> lyricList;//存放歌词的集合
    private int lightLyricIndex = 0;//高亮行歌词的索引
    private int lineHeight = 19;//设定每行歌词的高度，一般比字体大一点就行
    public LyricView(Context context) {
        super(context);
        init();
    }

    public LyricView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LyricView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(COLOR_DEFAULT);
        paint.setTextSize(SIZE_DEFAULT);
        //设置文本绘制的起点是底边的中心
        paint.setTextAlign(Paint.Align.CENTER);

        //虚拟歌词
//        lyricList = new ArrayList<>();
//        for (int i=0;i<50;i++){
//            lyricList.add(new Lyric("我是歌词,啦啦啦 - "+i,i*2000));
//        }
    }

    /**
     * 设置歌词的列表
     * @param lyricList
     */
    public void setLyricList(ArrayList<Lyric> lyricList){
        this.lyricList = lyricList;
    }

    private float width,height;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = getMeasuredWidth();
        height = getMeasuredHeight();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(lyricList==null){
            //绘制简单的文本
            String text = "正在加载歌词";
            float y = 30;
            drawHorizontalCenterText(canvas, text, y,true);
        }else {
            drawLyricList(canvas);
        }
    }

    /**
     * 绘制多行的歌词
     * @param canvas
     */
    private void drawLyricList(Canvas canvas) {
        Lyric lightLyric = lyricList.get(lightLyricIndex);
        smoothRoll(canvas, lightLyric);

        //1.先绘制高亮行歌词
        //绘制高亮歌词的y坐标
        float lightY = height/2 + getTextHeight(lightLyric.getContent())/2;
        drawHorizontalCenterText(canvas,lightLyric.getContent(),lightY,true);

        //2.绘制高亮行之前的所有歌词
        for (int i=0;i<lightLyricIndex;i++){
            Lyric lyric = lyricList.get(i);
            float y = lightY - (lightLyricIndex-i)*lineHeight;
            drawHorizontalCenterText(canvas,lyric.getContent(),y,false);
        }

        //3.绘制高亮行之后的所有歌词
        for (int i=lightLyricIndex+1;i<lyricList.size();i++){
            Lyric lyric = lyricList.get(i);
            float y = lightY + (i-lightLyricIndex)*lineHeight;
            drawHorizontalCenterText(canvas,lyric.getContent(),y,false);
        }
    }

    /**
     * 平滑滚动歌词
     * @param canvas
     * @param lightLyric
     */
    private void smoothRoll(Canvas canvas, Lyric lightLyric) {
        //缓慢滚动高亮歌词的逻辑：
        //拿高亮歌词作为参考：高亮歌词需要在自己的歌唱时间内往上滚动一个lineHeight的距离
        //a.计算歌唱时间，下一行歌词的startPoint减去当前的startPoint
        long lyricDuration;
        if(lightLyricIndex==(lyricList.size()-1)){
            //如果是最后一行，那么歌唱时间应该是歌曲的总时间减去startPoint
            lyricDuration = musicDuration - lightLyric.getStartPoint();
        }else {
            //获取高亮行下一行的歌词
            Lyric nextLyric = lyricList.get(lightLyricIndex+1);
            lyricDuration = nextLyric.getStartPoint() - lightLyric.getStartPoint();
        }
        //b.计算当前歌曲已经唱了多少时间
        long offset = musicPosition - lightLyric.getStartPoint();
        //c.计算已经唱的时间占总歌唱时间的百分比
        float percent = offset*1f/lyricDuration;
        //d.根据歌唱的百分比计算当前应该移动的距离(总距离是lineHeight)
        float dy = percent*lineHeight;
        //e.让画布移动
        canvas.translate(0,-1*dy);//往上移动
    }

    private long musicPosition;//歌曲播放的位置
    private long musicDuration;//当前歌曲的总时间
    /**
     * 滚动歌词的方法,该方法调用的频率是非常高的，因为歌曲的播放位置是实时变化的
     */
    public void startRoll(long musicPosition,long musicDuration){
        this.musicPosition = musicPosition;
        this.musicDuration = musicDuration;

        if(lyricList==null)return;
        //1.在歌曲播放过程中基三当前高亮歌词的索引
        caculateLightLyricIndex();
        //2.当lightLyricIndex更新之后，刷新重绘
        invalidate();
    }

    /**
     * 计算歌词集合中高亮行歌词的索引到底是哪个
     * 逻辑是这样的：如果歌曲播放的位置position大于当前的startPoint，并且小于
     * 我下一个歌词的startPoint，那么当前的i就是高亮；
     * 如果当前是最后一个歌词，那么直直接判断当前歌曲的position如果大于最后一个的startPoint，
     *
     */
    private void caculateLightLyricIndex() {
        for (int i=0;i<lyricList.size();i++){
            Lyric lyric = lyricList.get(i);
            if(i<(lyricList.size()-1)){
                Lyric nextLyric = lyricList.get(i + 1);//下一个歌词
                if(musicPosition>=lyric.getStartPoint() && musicPosition<nextLyric.getStartPoint()){
                    //说明当前的i就是高亮歌词了
                    lightLyricIndex = i;
                }
            }else {
                //说明lyric就是最后一个歌词了
                if(musicPosition>=lyric.getStartPoint()){
                    lightLyricIndex = i;
                }
            }
        }
    }


    /**
     * 绘制水平居中的文字
     *
     * @param canvas
     * @param text
     * @param y
     */
    private void drawHorizontalCenterText(Canvas canvas, String text, float y,boolean isLight) {
        float x = width / 2;
        //判断是否是高亮
        paint.setColor(isLight?COLOR_LIGHT:COLOR_DEFAULT);
        paint.setTextSize(isLight?SIZE_LIGHT:SIZE_DEFAULT);
        canvas.drawText(text, x, y, paint);
    }

    /**
     * 获取文字的高度
     * @param text
     * @return
     */
    private int getTextHeight(String text){
        Rect bounds = new Rect();
        paint.getTextBounds(text,0,text.length(),bounds);
        return  bounds.height();
    }
}
