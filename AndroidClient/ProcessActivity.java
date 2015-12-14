package com.perples.mobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.perples.recosample.R;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class ProcessActivity extends Activity {
    private ListView processListView;
    private ProcessListThread processListThread;
    private ProcessListAdapter processListAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);
        processListThread = new ProcessListThread();
        processListThread.start();
        try {
            processListThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        processListView = (ListView)findViewById(R.id.processList);
        processListAdapter = new ProcessListAdapter(this);
        processListView.setAdapter(processListAdapter);
        processListAdapter.removeAll(processListThread.processList);
        processListAdapter.updateAll(processListThread.processList);
        processListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_process, menu);
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
    class ProcessListThread extends Thread{
        ArrayList<String> processList = new ArrayList<String>();
        NetworkApp networkApp;
        public void run(){
            networkApp = (NetworkApp)getApplication();
            networkApp.sendMsg("processList#");

            while(true){
                String processName =  networkApp.recvMsg();

                if(processName.equals("end#")) break;
                else {
                    StringTokenizer stringTokenizer = new StringTokenizer(processName);
                    String process = stringTokenizer.nextToken("#");
                    processList.add(process);
                }
            }
        }

    }
}
