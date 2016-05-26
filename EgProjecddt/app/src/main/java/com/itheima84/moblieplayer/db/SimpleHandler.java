package com.itheima84.moblieplayer.db;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.Cursor;
import android.widget.CursorAdapter;

import com.itheima84.moblieplayer.util.Utils;

/**
 * Created by lxj on 2016/5/12.
 */
public class SimpleHandler extends AsyncQueryHandler {
    public SimpleHandler(ContentResolver cr) {
        super(cr);
    }


    @Override
    protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
        super.onQueryComplete(token, cookie, cursor);

        if(cookie!=null && cookie instanceof CursorAdapter){
            CursorAdapter cursorAdapter = (CursorAdapter) cookie;
            //更新数据
            cursorAdapter.changeCursor(cursor);//相当于notifyDatasetChange
        }

//        Utils.printCursor(cursor);
    }
}
