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

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;

import com.perples.recosample.R;
import com.perples.recosdk.RECOBeacon;
import com.perples.recosdk.RECOBeaconManager;
import com.perples.recosdk.RECOBeaconRegion;
import com.perples.recosdk.RECOBeaconRegionState;
import com.perples.recosdk.RECOErrorCode;
import com.perples.recosdk.RECOMonitoringListener;
import com.perples.recosdk.RECORangingListener;
import com.perples.recosdk.RECOServiceConnectListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

/**
 * RECOBackgroundRangingService is to monitor regions and range regions when the device is inside in the BACKGROUND.
 * 
 * RECOBackgroundMonitoringService는 백그라운드에서 monitoring을 수행하며, 특정 region 내부로 진입한 경우 백그라운드 상태에서 ranging을 수행합니다.
 */
public class RECOBackgroundRangingService extends Service implements RECOMonitoringListener, RECORangingListener, RECOServiceConnectListener {
	
	/**
	 * We recommend 1 second for scanning, 10 seconds interval between scanning, and 60 seconds for region expiration time. 
	 * 1초 스캔, 10초 간격으로 스캔, 60초의 region expiration time은 당사 권장사항입니다.
	 */
	private long mScanDuration = 1*1000L;
	private long mSleepDuration = 10*500L;
	private long mRegionExpirationTime = 60*5000L;
	private int mNotificationID = 9999;

    private CheckRegionThread checkRegionThread = new CheckRegionThread();
	private NetworkThread networkThread = new NetworkThread();
    private RECOBeaconManager mRecoManager;
	private ArrayList<RECOBeaconRegion> mRegions;
    private double limitDistance = 1.5;
    private boolean hookToogle= false;
    private int lockCount = 0;
    private DBManager dbManager;
    private int major;
    private int minor;
    private HookNotification hookNotification;

	@Override
	public void onCreate() {
		super.onCreate();
        dbManager = new DBManager(this);
        hookNotification = new HookNotification(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		/**
		 * Create an instance of RECOBeaconManager (to set scanning target and ranging timeout in the background.)
		 * If you want to scan only RECO, and do not set ranging timeout in the backgournd, create an instance: 
		 * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false);
		 * WARNING: False enableRangingTimeout will affect the battery consumption.
		 * 
		 * RECOBeaconManager 인스턴스틀 생성합니다. (스캔 대상 및 백그라운드 ranging timeout 설정)
		 * RECO만을 스캔하고, 백그라운드 ranging timeout을 설정하고 싶지 않으시다면, 다음과 같이 생성하시기 바랍니다.
		 * 		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), true, false); 
		 * 주의: enableRangingTimeout을 false로 설정 시, 배터리 소모량이 증가합니다.
		 */
		mRecoManager = RECOBeaconManager.getInstance(getApplicationContext(), MainActivity.SCAN_RECO_ONLY, MainActivity.ENABLE_BACKGROUND_RANGING_TIMEOUT);
		this.bindRECOService();
		return START_STICKY;
	}

    @Override
	public void onDestroy() {
        this.checkRegionThread.interrupt();
        networkThread.sendEndConnection();
        this.networkThread.interrupt();
		this.tearDown();
		super.onDestroy();
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		super.onTaskRemoved(rootIntent);
	}

	private void bindRECOService() {

		mRegions = new ArrayList<RECOBeaconRegion>();
		this.generateBeaconRegion();
		
		mRecoManager.setMonitoringListener(this);
		mRecoManager.setRangingListener(this);
		mRecoManager.bind(this);
	}
	
	private void generateBeaconRegion() {

		RECOBeaconRegion recoRegion;
		
		recoRegion = new RECOBeaconRegion(MainActivity.RECO_UUID, "RECO Sample Region");
		recoRegion.setRegionExpirationTimeMillis(this.mRegionExpirationTime);
		mRegions.add(recoRegion);
	}
	
	private void startMonitoring() {

		mRecoManager.setScanPeriod(this.mScanDuration);
		mRecoManager.setSleepPeriod(this.mSleepDuration);
		
		for(RECOBeaconRegion region : mRegions) {
			try {
				mRecoManager.startMonitoringForRegion(region);
			} catch (RemoteException e) {

				e.printStackTrace();
			} catch (NullPointerException e) {

				e.printStackTrace();
			}
		}
        checkRegionThread.start();
        networkThread.start();
	}
	
	private void stopMonitoring() {

		for(RECOBeaconRegion region : mRegions) {
			try {
				mRecoManager.stopMonitoringForRegion(region);
			} catch (RemoteException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
        networkThread.sendEndConnection();
        networkThread.interrupt();
        checkRegionThread.interrupt();
	}
	
	private void startRangingWithRegion(RECOBeaconRegion region) {

		/**
		 * There is a known android bug that some android devices scan BLE devices only once. (link: http://code.google.com/p/android/issues/detail?id=65863)
		 * To resolve the bug in our SDK, you can use setDiscontinuousScan() method of the RECOBeaconManager. 
		 * This method is to set whether the device scans BLE devices continuously or discontinuously. 
		 * The default is set as FALSE. Please set TRUE only for specific devices.
		 * 
		 * mRecoManager.setDiscontinuousScan(true);
		 */
		
		try {
			mRecoManager.startRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	private void stopRangingWithRegion(RECOBeaconRegion region) {

		try {
			mRecoManager.stopRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

    private void tearDown() {
		this.stopMonitoring();
		
		try {
			mRecoManager.unbind();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
        networkThread.sendEndConnection();
        networkThread.interrupt();
        checkRegionThread.interrupt();
	}
	
	@Override
	public void onServiceConnect() {
		this.startMonitoring();
		//Write the code when RECOBeaconManager is bound to RECOBeaconService
	}

	@Override
	public void didDetermineStateForRegion(RECOBeaconRegionState state, RECOBeaconRegion region) {
		//Write the code when the state of the monitored region is changed
	}

	@Override
	public void didEnterRegion(RECOBeaconRegion region, Collection<RECOBeacon> beacons) {
		/**
		 * For the first run, this callback method will not be called. 
		 * Please check the state of the region using didDetermineStateForRegion() callback method.
		 * 
		 * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다. 
		 * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
		 */
		
		//Get the region and found beacon list in the entered region
		this.popupNotification("Inside of " + region.getUniqueIdentifier());
		//Write the code when the device is enter the region
		
		this.startRangingWithRegion(region); //start ranging to get beacons inside of the region
		//from now, stop ranging after 10 seconds if the device is not exited
	}

	@Override
	public void didExitRegion(RECOBeaconRegion region) {
		/**
		 * For the first run, this callback method will not be called. 
		 * Please check the state of the region using didDetermineStateForRegion() callback method.
		 * 
		 * 최초 실행시, 이 콜백 메소드는 호출되지 않습니다. 
		 * didDetermineStateForRegion() 콜백 메소드를 통해 region 상태를 확인할 수 있습니다.
		 */
		
		this.popupNotification("Outside of " + region.getUniqueIdentifier());
		//Write the code when the device is exit the region
		
		this.stopRangingWithRegion(region); //stop ranging because the device is outside of the region from now
	}

	@Override
	public void didStartMonitoringForRegion(RECOBeaconRegion region) {
		//Write the code when starting monitoring the region is started successfully
	}

	@Override
	public void didRangeBeaconsInRegion(Collection<RECOBeacon> beacons, RECOBeaconRegion region) {
		//Write the code when the beacons inside of the region is received
        ArrayList<RECOBeacon> beaconArrayList = new ArrayList<>(beacons);

        if(beaconArrayList.size() == 0) {
            if(lockCount <1) {
                lockCount++;
            }
            else if(!hookToogle && lockCount >=1) {
                hookToogle = true;
                networkThread.sendStart();
                sendBroadcast(new Intent("hookStart"));
                hookNotification.showHookMessage();
                synchronized (this){
                    long time = System.currentTimeMillis();
                    SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                    String str = dayTime.format(new Date(time));
                    dbManager.insertLog(1, str);
                }
            }

        }
        for(int i = 0; i < beaconArrayList.size();i++){
            //locking
            if(beaconArrayList.get(i).getAccuracy() > limitDistance){
                if(lockCount < 1){
                    lockCount++;
                }
                else if(!hookToogle && lockCount >=1) {
                    hookToogle = true;
                    networkThread.sendStart();
                    sendBroadcast(new Intent("hookStart"));
                    hookNotification.showHookMessage();
                    synchronized (this){
                        long time = System.currentTimeMillis();
                        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                        String str = dayTime.format(new Date(time));
                        dbManager.insertLog(1, str);
                    }
                }
            }
            //unlocking
            else{
                if(lockCount >-2){
                    lockCount--;
                }
                else if(hookToogle && lockCount <= -1){
                    hookToogle = false;
                    networkThread.sendTerminate();
                    sendBroadcast(new Intent("hookTerminate"));
                    hookNotification.showUnHookMessage();
                    synchronized (this){
                        long time = System.currentTimeMillis();
                        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
                        String str = dayTime.format(new Date(time));
                        dbManager.insertLog(0,str);
                    }
                }
            }
        }
    }
	
	private void popupNotification(String msg) {
		String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date());
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.main_icon)
																				.setContentTitle(msg + " " + currentTime)
																				.setContentText(msg);

		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		builder.setStyle(inboxStyle);
		nm.notify(mNotificationID, builder.build());
		mNotificationID = (mNotificationID - 1) % 1000 + 9000;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		//This method is not used
		return null;
	}
	
	@Override
	public void onServiceFail(RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed.
		//See the RECOErrorCode in the documents.
		return;
	}
	
	@Override
	public void monitoringDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed to monitor the region.
		//See the RECOErrorCode in the documents.
		return;
	}
	
	@Override
	public void rangingBeaconsDidFailForRegion(RECOBeaconRegion region, RECOErrorCode errorCode) {
		//Write the code when the RECOBeaconService is failed to range beacons in the region.
		//See the RECOErrorCode in the documents.
		return;
	}

    class CheckRegionThread extends Thread{
        String mProximityUuid = "24DDF411-8CF1-440C-87CD-E368DAF9C93E";
        public void run(){
            while(true){
                try {
                    sleep(2000);
                    RECOBeaconRegion region = new RECOBeaconRegion(mProximityUuid,major, minor,"rk");

                    mRecoManager.startRangingBeaconsInRegion(region);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    class NetworkThread extends Thread{
        //boolean terminateSend = false;
        //boolean startSend = false;
        boolean endLoop = false;
        NetworkApp networkApp;
        public void run(){
            networkApp = (NetworkApp)getApplication();
            limitDistance = networkApp.getControlRange();
            major = networkApp.getMajor();
            minor = networkApp.getMinor();
            if(!networkApp.isNetworkConnected()) return;
            while(true){
                synchronized (this){
                    if(endLoop) break;
                }
            }
        }
        public void sendStart(){
            networkApp.sendMsg("start#");
            synchronized (this) {
                networkApp.setHooked(true);
            }
        }
        public void sendTerminate(){
            networkApp.sendMsg("terminate#");
            synchronized (this) {
                networkApp.setHooked(false);
            }
        }
        public void sendEndConnection(){
            synchronized (this){
                if(hookToogle) {
                    sendTerminate();
                    lockCount = 0;
                }
                endLoop = true;
            }
        }
    }
}
