package com.alinaj.dev.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.alinaj.dev.activities.OpenVPNClient;
import com.alinaj.dev.R;

import java.util.concurrent.TimeUnit;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2024/05/12
 */

public class TimerService extends Service {

    private static final String NOTIFICATION = "technore timer";
    private static final long DEFAULT_TIME = TimeUnit.HOURS.toMillis(3);
    //private static final long DEFAULT_TIME = TimeUnit.SECONDS.toMillis(30);

    private boolean pause;
    private boolean noticeSent = false;

    private Notification.Builder notification;
    private NotificationManager notificationManager;

    private SharedPreferences sp;
    private CountDownTimer timer;
    private TimerListener timerListener;

    @Override
    public IBinder onBind(Intent p1) {
        return new ServiceBinder();
    }

    public class ServiceBinder extends Binder {
        public TimerService getService() {
            return TimerService.this;
        }
    }

    public interface TimerListener {
        void onTimeChanged(String time);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        noticeSent = false;
        startNotification();
        start();
        return Service.START_STICKY;
    }

    public void setTimerListener(TimerListener timerListener) {
        this.timerListener = timerListener;
        if (timerListener != null) {
            timerListener.onTimeChanged(getStringTime(sp.getLong("timeLeft", DEFAULT_TIME)));
        }
    }

    public void add(long millis) {
        pause = timer != null;
        stop();
        setTime(sp.getLong("timeLeft", DEFAULT_TIME) + millis);
        if (pause) {
            start();
            pause = false;
        }
        if (timerListener != null) {
            timerListener.onTimeChanged(getStringTime(sp.getLong("timeLeft", DEFAULT_TIME)));
        }
    }

    public boolean isTimeAvailable() {
        return !getStringTime(sp.getLong("timeLeft", DEFAULT_TIME)).equals(getStringTime(0));
    }

    private synchronized void setTime(long millis) {
        sp.edit().putLong("timeLeft", millis).apply();
    }

    private void start() {
        timer = new CountDownTimer(sp.getLong("timeLeft", DEFAULT_TIME), 1000) {
            @Override
            public void onTick(long millis) {
                setTime(millis);
                if (timerListener != null) {
                    timerListener.onTimeChanged(getStringTime(millis));
                }
                if (notification != null) {
                    notification.setContentText(getStringTime(millis));
                    notificationManager.notify(NOTIFICATION.hashCode(), notification.build());
                }
            }

            @Override
            public void onFinish() {

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(TimerService.this.getApplicationContext(), "VPN stopped!", Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                android.os.Process.killProcess(android.os.Process.myPid());
                            }
                        }, 2000);
                    }
                });

                stop();
                stopNotification();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void startNotification() {
        notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_clock)
                .setContentTitle("Time Left")
                .setContentText(getStringTime(sp.getLong("timeLeft", DEFAULT_TIME)))
                .setCategory(Notification.CATEGORY_SERVICE);
        Intent notificationIntent = new Intent(this, OpenVPNClient.class);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent resultIntent = PendingIntent.getActivity(this, 0, notificationIntent, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT : PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setContentIntent(resultIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION, getClass().getName(), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);
            notification.setChannelId(NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startForeground(NOTIFICATION.hashCode(), notification.build());
        } else {
            startForeground(NOTIFICATION.hashCode(), notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        }
    }

    public void stopNotification() {
        notificationManager.cancelAll();
        notification = null;
        stopForeground(true);
    }

    private String getStringTime(long millis) {
        long seconds = (millis / 1000);
        long sec = seconds % 60;
        long min = (seconds / 60) % 60;
        long hrs = (seconds / (60 * 60)) % 24;
        return String.format("%02dh:%02dm:%02ds", hrs, min, sec);
    }

    @Override
    public void onDestroy() {
        stop();
        stopNotification();
        stopSelf();
        super.onDestroy();
    }
}
