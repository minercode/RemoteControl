package com.perples.mobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.perples.recosample.R;

import java.util.ArrayList;

public class LogInfoActivity extends Activity {
    private ListView mLogListView;

    private LogListAdapter logListAdapter;
    private ArrayList<LogInfo> logInfos;
    private Button resetBtn;
    private Button deleteBtn;
    private DBManager dbManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_info);
        resetBtn = (Button)findViewById(R.id.resetBtn);
        deleteBtn = (Button)findViewById(R.id.deleteBtn);
        dbManager = new DBManager(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLogListView = (ListView)findViewById(R.id.logList);
        logListAdapter = new LogListAdapter(this);
        mLogListView.setAdapter(logListAdapter);
        this.resetList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_log_info, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private void resetList(){
        logInfos = dbManager.getLogData();
        logListAdapter.removeAll(logInfos);
        logListAdapter.updateAll(logInfos);
        logListAdapter.notifyDataSetChanged();
    }
    public void onResetBtnClicked(View view) {
        this.resetList();
    }

    public void onDeleteBtnClicked(View view) {
        logInfos = dbManager.getLogData();
        logListAdapter.removeAll(logInfos);
        dbManager.deleteLog();
        logInfos = dbManager.getLogData();
        logListAdapter.updateAll(logInfos);
        logListAdapter.notifyDataSetChanged();
    }
}
