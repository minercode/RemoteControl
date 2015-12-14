/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2014-2015 Perples, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.perples.mobileapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.perples.recosample.R;

import java.util.StringTokenizer;

public class MainActivity extends Activity {
	//This is a default proximity uuid of the RECO
	public static final String RECO_UUID = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";
	public static final boolean SCAN_RECO_ONLY = true;
	public static final boolean ENABLE_BACKGROUND_RANGING_TIMEOUT = true;
	public static final boolean DISCONTINUOUS_SCAN = false;
	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
    //View Objects
    private EditText idText;
    private EditText passText;
    private Button loginBtn;

    //beacon Info
    private String major = null;
    private String minor = null;

    //network
    private NetworkApp networkApp;

    private void InitView(){
        idText = (EditText)findViewById(R.id.idText);
        passText = (EditText)findViewById(R.id.passText);
        loginBtn = (Button)findViewById(R.id.loginBtn);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//If a user device turns off bluetooth, request to turn it on.
		//사용자가 블루투스를 켜도록 요청합니다.
		mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		
		if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
		}
        InitView();
        networkApp = (NetworkApp)getApplication();
        if(networkApp.isNetworkConnected()){
            final Intent intent = new Intent(this, LoginedActivity.class);
            startActivity(intent);
            this.finish();
        }
        final DBManager dbManager = new DBManager(this);
        String ip= dbManager.getIP();
        if(ip.equals("0")){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final EditText input = new EditText(this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final String inputIp = input.getText().toString();
                    networkApp.setServerIp(inputIp);
                    dbManager.updateIP(inputIp);
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            alert.show();
        }
        else{
            networkApp.setServerIp(ip);
        }
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			//If the request to turn on bluetooth is denied, the app will be finished.
			//사용자가 블루투스 요청을 허용하지 않았을 경우, 어플리케이션은 종료됩니다.
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onResume() {
		Log.i("MainActivity", "onResume()");
		super.onResume();

	}
	
	@Override
	protected void onDestroy() {
		Log.i("MainActivity", "onDestroy");
		super.onDestroy();
	}


    public void onLoginBtnClicked(View view) {
        LoginNetworkThreadC loginNetworkThreadC = new LoginNetworkThreadC();
        loginNetworkThreadC.start();
        try {
            loginNetworkThreadC.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (loginNetworkThreadC.authFlag) {
            idText.setText("로그인 ok");
            final Intent intent = new Intent(this, LoginedActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "아이디랑 비밀번호가 맞지 않습니다. 다시 설정하여 주십시오", Toast.LENGTH_LONG).show();
            networkApp.setTerminate();
            networkApp.setConnetion();
            idText.setText("");
            passText.setText("");
        }
        this.finish();
    }

    public void onIpSetBtnClicked(View view) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String inputIp = input.getText().toString();
                networkApp.setServerIp(inputIp);
                DBManager dbManager = new DBManager(getApplicationContext());
                dbManager.updateIP(inputIp);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alert.show();
    }

    /**
     *  created by minercode
     */
    class LoginNetworkThreadC extends Thread{
        private boolean authFlag;
        public void run() {
            networkApp.setConnetion();
            while(true){
                if(networkApp.isNetworkConnected()) break;
            }
            networkApp.sendMsg("login#" + idText.getText().toString() + "#" + passText.getText().toString());

            String msg = null;
            synchronized (this) {
                msg = networkApp.recvMsg();
            }
            StringTokenizer stringTokenizer = new StringTokenizer(msg);
            String auth = stringTokenizer.nextToken("#");

            if(auth.equals("auth")) {
                authFlag = true;
                major = stringTokenizer.nextToken("#");
                minor = stringTokenizer.nextToken("#");
                networkApp.setMajor(Integer.parseInt(major));
                networkApp.setMinor(Integer.parseInt(minor));
            }
            else if(auth.equals("no")){
                authFlag = false;
            }
            else{
                Log.e("여기", "로그인 통신 에러 발생");
            }
        }
    }
}
