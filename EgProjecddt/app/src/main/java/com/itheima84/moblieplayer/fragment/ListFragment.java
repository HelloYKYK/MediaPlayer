package com.itheima84.moblieplayer.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.activity.MusicPlayerActivity;
import com.itheima84.moblieplayer.activity.VideoPlayerActivity;
import com.itheima84.moblieplayer.adapter.MusicListAdapter;
import com.itheima84.moblieplayer.adapter.VideoListAdapter;
import com.itheima84.moblieplayer.base.BaseFragment;
import com.itheima84.moblieplayer.bean.MusicItem;
import com.itheima84.moblieplayer.bean.VideoItem;
import com.itheima84.moblieplayer.db.SimpleHandler;
import com.itheima84.moblieplayer.util.Constant;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by lxj on 2016/5/12.
 */
public class ListFragment extends BaseFragment {

    @Bind(R.id.listview)
    ListView listview;
    private CursorAdapter listAdapter;
    private int type;

    /**
     * 创建Fragment，运行传参数
     *
     * @param args
     * @return
     */
    public static ListFragment newInstance(Bundle args) {
        ListFragment listFragment = new ListFragment();
        listFragment.setArguments(args);
        return listFragment;
    }


    @Override
    public int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    public void setListener() {
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //注意：获取的cursor对象是已经move到当前position的Cursor
                Cursor cursor = (Cursor) listAdapter.getItem(position);
                if(type==Constant.VIDEO){
                    //将视频数据传递给视频播放界面
                    Intent intent = new Intent(getActivity(),VideoPlayerActivity.class);
                    intent.putExtra("currentVideo",position);
                    intent.putExtra("videoList",cursor2VideoList(cursor));
                    startActivity(intent);
                }else if(type==Constant.MUSIC){
                    Intent intent = new Intent(getActivity(),MusicPlayerActivity.class);
                    intent.putExtra("currentMusic",position);
                    intent.putExtra("musicList",cursor2MusicList(cursor));
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * 将Cursor中的数据全部放入集合中
     * @param cursor
     * @return
     */
    private ArrayList<VideoItem> cursor2VideoList(Cursor cursor) {
        ArrayList<VideoItem> list = new ArrayList<>();
        //由于Cursor可能移动过，那么会造成获取的数据缺失，所以应该先让Cursor重置
        cursor.moveToPosition(-1);//先让Cursor移动到第0个之前的位置
        while (cursor.moveToNext()){
            list.add(VideoItem.fromCursor(cursor));
        }
        return  list;
    }
    private ArrayList<MusicItem> cursor2MusicList(Cursor cursor) {
        ArrayList<MusicItem> list = new ArrayList<>();
        //由于Cursor可能移动过，那么会造成获取的数据缺失，所以应该先让Cursor重置
        cursor.moveToPosition(-1);//先让Cursor移动到第0个之前的位置
        while (cursor.moveToNext()){
            list.add(MusicItem.fromCursor(cursor));
        }
        return  list;
    }

    @Override
    public void initData() {
        //1.给listview设置adapter
        SimpleHandler simpleHandler = new SimpleHandler(getActivity().getContentResolver());
        Bundle bundle = getArguments();
        type = bundle.getInt("type");

        if (type == Constant.VIDEO) {
            loadVideoList(simpleHandler);

        } else if (type == Constant.MUSIC) {
            loadMusicList(simpleHandler);
        }

        listview.setAdapter(listAdapter);
    }

    private void loadMusicList(SimpleHandler simpleHandler) {
        listAdapter = new MusicListAdapter(getActivity(),null);
        //获取音乐
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;//sd卡上音乐的uri路径
        String[] projection = {Audio.Media._ID , Audio.Media.DISPLAY_NAME, Audio.Media.DURATION, Audio.Media.SIZE
                , Audio.Media.DATA, Audio.Media.ARTIST};//定义所查询的列
        //使用AsyncQueryHandler进行异步查询
        simpleHandler.startQuery(0, listAdapter, uri, projection, null, null, null);
    }

    private void loadVideoList(SimpleHandler simpleHandler) {
        listAdapter = new VideoListAdapter(getActivity(),null);
        //获取视频
        //通过系统对外暴露的内容提供者获取多媒体数据
        //由于原生系统只认识MP3,3gp，所以该方法是无法获取到avi等其他格式的视频的
        Uri uri = Video.Media.EXTERNAL_CONTENT_URI;//sd卡上视频的uri路径
        String[] projection = {Video.Media._ID , Video.Media.TITLE, Video.Media.DURATION, Video.Media.SIZE
                , Video.Media.DATA};//定义所查询的列

        //在主线程查询，会造成UI阻塞
//            Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

        //使用AsyncQueryHandler进行异步查询
        simpleHandler.startQuery(0, listAdapter, uri, projection, null, null, null);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
