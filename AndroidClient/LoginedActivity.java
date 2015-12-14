package com.perples.mobileapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.perples.recosample.R;

public class LoginedActivity extends Activity {

    //Beacon Info

    //Button Text State
    private String startServiceStr = "서비스 실행하기";
    private String endServiceStr = "서비스 중지하기";
    //View Group
    private Button serviceToggleBtn;
    private TextView currentText;
    private CurrentTextReceiver currentTextReceiver;
    private IntentFilter intentFilter;
    private NetworkApp networkApp;
    private EditText rangeText;
    private SeekBar controlRange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        networkApp = (NetworkApp)getApplication();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logined);

        this.initView();

        if(this.isServiceStarted(getApplicationContext())) serviceToggleBtn.setText(endServiceStr);
        else serviceToggleBtn.setText(startServiceStr);
        currentText = (TextView)findViewById(R.id.currentText);
        currentTextReceiver = new CurrentTextReceiver();

        intentFilter = new IntentFilter();
        intentFilter.addAction("hookStart");
        intentFilter.addAction("hookTerminate");
        registerReceiver(currentTextReceiver, intentFilter);
        if(networkApp.isHooked()){
            currentText.setText("잠금 상태");
        }
        else{
            currentText.setText("해제 상태");
        }
        rangeText = (EditText)findViewById(R.id.rangeText);
        controlRange = (SeekBar)findViewById(R.id.controlRange);
        int initialRange = 17;
        controlRange.setProgress(17);
        controlRange.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double range = (double)progress * 3 / 100 +1;
                range = (range *100) / 100;

                rangeText.setText(String.format("%.2f", range) + "m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String range = rangeText.getText().toString();
                range = range.split("m")[0];
                networkApp.setControlRange(Double.parseDouble(range));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(this.isServiceStarted(getApplicationContext())) serviceToggleBtn.setText(endServiceStr);
        else serviceToggleBtn.setText(startServiceStr);
        if(networkApp.isHooked()){
            currentText.setText("잠금 상태");
        }
        else{
            currentText.setText("해제 상태");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_logined, menu);
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

    public void initView(){
        this.serviceToggleBtn = (Button)findViewById(R.id.serviceToggleBtn);
    }

    public boolean isServiceStarted(Context context){
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo runningService : am.getRunningServices(Integer.MAX_VALUE)) {
            if(RECOBackgroundRangingService.class.getName().equals(runningService.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
    public void onServiceToggleBtnClicked(View view) {
        Intent intent = new Intent(this, RECOBackgroundRangingService.class);
        if(isServiceStarted(getApplicationContext())){
            stopService(intent);
            serviceToggleBtn.setText(startServiceStr);
        }
        else{
            startService(intent);
            serviceToggleBtn.setText(endServiceStr);
        }
    }

    public void onLogBtnClicked(View view) {
        final Intent intent = new Intent(this, LogInfoActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(currentTextReceiver);
    }

    public void onProcessBtnClicked(View view) {
        final Intent intent = new Intent(this, ProcessActivity.class);
        startActivity(intent);
    }
    class CurrentTextReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getAction();
            if(command.equals("hookStart")){
                currentText.setText("잠금 상태");
            }
            else if(command.equals("hookTerminate")){
                currentText.setText("해제 상태");
            }
        }
    }
}
