package com.perples.mobileapp;

/**
 * Created by Wangduk on 2015-12-10.
 */
public class LogInfo {
    private int logType;
    private String Date;

    public LogInfo(int logType, String date) {
        this.logType = logType;
        Date = date;
    }

    public int getLogType() {
        return logType;
    }

    public void setLogType(int logType) {
        this.logType = logType;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }
}
