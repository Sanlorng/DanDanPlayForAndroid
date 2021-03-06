package com.xyoye.dandanplay.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import com.xyoye.dandanplay.R;
import com.xyoye.dandanplay.ui.activities.smb.SmbDeviceActivity;
import com.xyoye.dandanplay.utils.smb.SmbServer;

import java.io.IOException;

/**
 * Created by xyoye on 2018/11/22.
 */

public class SmbService extends Service {
    private int NOTIFICATION_ID = 1002;

    private NotificationManager notificationManager;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //创建NotificationChannel
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel("com.xyoye.dandanplay.smbservice.playchannel", "SMB服务", NotificationManager.IMPORTANCE_LOW);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
            channel.enableLights(false);
            channel.setSound(null, null);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SmbServer.getInstance().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Notification buildNotification(){
        Intent intent = new Intent(this, SmbDeviceActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, intent,0);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("弹弹play")
                .setContentText("已开启SMB服务")
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setDefaults(NotificationCompat.FLAG_ONLY_ALERT_ONCE)
                .setVibrate(new long[]{0})
                .setSound(null);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("com.xyoye.dandanplay.smbservice.playchannel");
        }
        Notification notify = builder.build();
        notify.flags = Notification.FLAG_FOREGROUND_SERVICE ;
        return notify;
    }

    @Override
    public void onDestroy() {
        if (notificationManager != null){
            notificationManager.cancel(NOTIFICATION_ID);
        }
        SmbServer.getInstance().stop();
        super.onDestroy();
    }
}