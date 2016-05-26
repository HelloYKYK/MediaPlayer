package com.itheima84.moblieplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.itheima84.moblieplayer.R;
import com.itheima84.moblieplayer.bean.VideoItem;
import com.itheima84.moblieplayer.util.Utils;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by lxj on 2016/5/12.
 */
public class VideoListAdapter extends CursorAdapter {

    public VideoListAdapter(Context context, Cursor c) {
        super(context, c);
    }

    /**
     * 返回adapter的布局文件
     *
     * @param context
     * @param cursor
     * @param parent
     * @return
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = View.inflate(context, R.layout.adapter_list, null);
        return view;
    }

    /**
     * 将Cursor数据绑定到View
     *
     * @param view
     * @param context
     * @param cursor
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = ViewHolder.getHolder(view);

        //绑定数据
        //将Cursor转为java bean
        VideoItem videoItem = VideoItem.fromCursor(cursor);
        holder.tvTitle.setText(videoItem.title);
        holder.tvSize.setText(Formatter.formatFileSize(context,videoItem.size));
        holder.tvDuration.setText(Utils.formatDuration(videoItem.duration));
    }

    static class ViewHolder {
        @Bind(R.id.tv_title)
        TextView tvTitle;
        @Bind(R.id.tv_duration)
        TextView tvDuration;
        @Bind(R.id.tv_size)
        TextView tvSize;

        ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }

        public static ViewHolder getHolder(View view){
            ViewHolder holder = (ViewHolder) view.getTag();
            if(holder==null){
                holder = new ViewHolder(view);
                view.setTag(holder);
            }
            return holder;
        }
    }
}
