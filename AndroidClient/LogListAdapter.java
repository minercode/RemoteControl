package com.perples.mobileapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.perples.recosample.R;

import java.util.ArrayList;

/**
 * Created by Wangduk on 2015-12-11.
 */
public class LogListAdapter extends BaseAdapter{
    private ArrayList<LogInfo> mList;
    private Context context;
    private LayoutInflater inflater;

    public LogListAdapter(Context context) {
        this.context = context;
        mList = new ArrayList<LogInfo>();
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount(){return mList.size();}
    @Override
    public LogInfo getItem(int position){
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        if(convertView == null){
            convertView = inflater.inflate(R.layout.list_log_item, parent, false);
        }
        final TextView logType = (TextView)convertView.findViewById(R.id.logType);
        final TextView logDate = (TextView)convertView.findViewById(R.id.logDate);

        final LogInfo logInfo = mList.get(pos);
        if(logInfo.getLogType() == 1){
            logType.setText("잠금   ");
        }
        else if(logInfo.getLogType() == 0){
            logType.setText("해제   ");
        }
        logDate.setText(logInfo.getDate());
        return convertView;
    }

    public void add(LogInfo logInfo){mList.add(logInfo);}
    public void removeAll(ArrayList<LogInfo> logInfos){
        mList.removeAll(logInfos);
    }
    public void updateAll(ArrayList<LogInfo> logInfos){
        synchronized (logInfos) {
            mList = new ArrayList<LogInfo>(logInfos);
        }
    }
}
