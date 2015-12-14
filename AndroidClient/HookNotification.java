package com.perples.mobileapp;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.perples.recosample.R;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Wangduk on 2015-12-13.
 */
public class HookNotification {
    private NotificationManager notificationManager = null;
    private Context context;
    private Notification notification;
    private PendingIntent pendingIntent;
    public HookNotification(Context context) {
        this.notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
        Intent intent = new Intent(context, LoginedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    }

    public void showHookMessage(){
        notification = new Notification(R.drawable.main_icon,"원격 잠금", System.currentTimeMillis());
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String strNow = sdfNow.format(date);
        notification.setLatestEventInfo(context,"원격 잠금 실행" ,"현재 시간 "+ strNow + " 에 컴퓨터가 원격 잠금 되었습니다.", pendingIntent);
        notification.flags = notification.flags | notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(1234, notification);
    }
    public void showUnHookMessage(){
        notification = new Notification(R.drawable.main_icon,"잠금 해제", System.currentTimeMillis());
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String strNow = sdfNow.format(date);
        notification.setLatestEventInfo(context,"원격 잠금 해제" ,"현재 시간 "+ strNow + " 에 컴퓨터가 잠금 해제 되었습니다.", pendingIntent);
        notification.flags = notification.flags | notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(1234, notification);
    }
}
