package com.perples.mobileapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.perples.recosample.R;

import java.util.ArrayList;

/**
 * Created by Wangduk on 2015-12-11.
 */
public class ProcessListAdapter extends BaseAdapter {
    private ArrayList<String> mList;
    private Context context;
    private LayoutInflater inflater;

    public ProcessListAdapter(Context context) {
        this.context = context;
        mList = new ArrayList<String>();
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount(){return mList.size();}
    @Override
    public String getItem(int position){
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
            convertView = inflater.inflate(R.layout.list_process_item, parent, false);
        }
        final TextView processName = (TextView)convertView.findViewById(R.id.processName);
        final Button endBtn = (Button)convertView.findViewById(R.id.endBtn);

        processName.setText(mList.get(pos));
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProcessEndThread processEndThread = new ProcessEndThread();
                String msg = "processEnd#" + processName.getText().toString() + "#";
                processEndThread.setMsg(msg);
                processEndThread.run();
                Intent intent = new Intent(context, LoginedActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
        return convertView;
    }
    public void removeAll(ArrayList<String> processList){
        mList.removeAll(processList);
    }
    public void updateAll(ArrayList<String> processList){
        synchronized (processList) {
            mList = new ArrayList<String>(processList);
        }
    }
    class ProcessEndThread extends Thread{
        NetworkApp networkApp;
        String msg;
        public void setMsg(String msg){
            this.msg = msg;
        }
        public void run(){
            networkApp = (NetworkApp)context.getApplicationContext();
            networkApp.sendMsg(msg);
        }
    }
}
