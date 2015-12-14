package com.perples.mobileapp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by Wangduk on 2015-12-10.
 */
public class DBManager {
    private static final String dbName = "mobileDB";
    private static final String logTable = "logTable";
    private static final String ipTable = "ipTable";
    private static final int dbVersion = 1;

    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private Context context;
    public DBManager(Context context){
        this.context = context;
        this.dbHelper = new DBHelper(context, dbName, null, dbVersion);
        db = dbHelper.getWritableDatabase();
    }
    private  class DBHelper extends SQLiteOpenHelper{
        public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
            super(context, name, null, version);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {

            String createLogTable = "create table "+ ipTable +" (id integer primary key autoincrement, "+ "ip text)";
            db.execSQL(createLogTable);
            //logType = 1 잠금 logType = 0 해제
            String createIpTable = "create table "+logTable+" (id integer primary key autoincrement, logType integer, Date Text)";
            db.execSQL(createIpTable);
            String ipInsert = "insert into "+ipTable+" values(NULL, '"+"0"+"');";

            db.execSQL(ipInsert);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
    public void updateIP(String ip){
        String sql = "update " + ipTable+ " set ip = '"+ip+"' where id = 1;";
        db.execSQL(sql);
    }

    public void insertLog(int logType, String date){
        String sql = "insert into "+ logTable + " values(NULL, "+logType+", '"+date+"');";
        db.execSQL(sql);
    }
    public void deleteLog(){
        String sql1 = "delete from "+ logTable + " where logType = "+ 0 + ";";
        String sql2 = "delete from "+ logTable + " where logType = "+ 1 + ";";
        db.execSQL(sql1);
        db.execSQL(sql2);
    }
    public String getIP(){
        String sql = "select ip from "+ ipTable+ " where id = "+ 1+";";
        Cursor result = db.rawQuery(sql, null);
        result.moveToFirst();
        String ip = result.getString(0);
        result.close();
        return ip;
    }
    public ArrayList<LogInfo> getLogData(){
        String sql = "select logType, Date from " + logTable + ";";

        ArrayList<LogInfo>  logInfos = new ArrayList<LogInfo>();
        Cursor result = db.rawQuery(sql, null);
        result.moveToFirst();
        while(!result.isAfterLast()){
            LogInfo logInfo = new LogInfo(result.getInt(0), result.getString(1));
            logInfos.add(logInfo);
            result.moveToNext();
        }
        result.close();
        return logInfos;
    }
}
