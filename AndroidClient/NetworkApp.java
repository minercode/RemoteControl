package com.perples.mobileapp;

import android.app.Application;

import com.perples.recosample.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by Wangduk on 2015-12-08.
 */
public class NetworkApp extends Application {
    private boolean networkConnected = false;
    private GlobalNetworkThread globalNetworkThread = new GlobalNetworkThread();
    private String serverIp;
    private int major;
    private int minor;

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    private boolean hooked = false;

    public double getControlRange() {
        return controlRange;
    }

    public void setControlRange(double controlRange) {
        this.controlRange = controlRange;
    }

    private double controlRange=1.5;
    public boolean isNetworkConnected(){
        return  globalNetworkThread.networkConnected;
    }
    public void setTerminate(){
        globalNetworkThread.terminateConnection();
    }
    public void setConnetion(){
        globalNetworkThread.start();
        networkConnected =true;
    }
    public void setServerIp(String serverIp){
        this.serverIp = serverIp;
    }
    public String getServerIp(){
        return this.serverIp;
    }

    public boolean isHooked() {
        return hooked;
    }

    public void setHooked(boolean hooked) {
        this.hooked = hooked;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
        if(networkConnected){
            globalNetworkThread.terminateConnection();
        }
    }
    public void sendMsg(String msg) {
        synchronized (this) {
            globalNetworkThread.sendMsg(msg);
        }
    }
    public String recvMsg(){
        String msg = null;
        synchronized (this) {

            msg=  globalNetworkThread.recvMsg();
        }

        return msg;
    }
    class GlobalNetworkThread extends Thread{
        private boolean networkConnected = false;
        private Socket socket = null;
        private BufferedReader bufferedReader = null;
        private PrintWriter printWriter = null;
        public void run(){
            synchronized (this) {
                try {
                    socket = new Socket(serverIp, Integer.parseInt(getResources().getString(R.string.serverPort)));
                    socket.setTcpNoDelay(true);
                    bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                    networkConnected = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            while(true){
                synchronized (this){
                    if(!networkConnected){
                        break;
                    }
                }
            }
        }
        public void sendMsg(String msg){
            printWriter.println(msg);
        }

        public String recvMsg(){
            String msg = new String();
            try {
                msg = bufferedReader.readLine();
                msg.replace("\r\n", "");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return msg;
        }
        public void terminateConnection(){
            networkConnected = false;
            try {
                printWriter.println("exit#");
                bufferedReader.close();
                printWriter.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
