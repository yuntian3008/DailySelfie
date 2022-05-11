package com.example.dailyselfie.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import com.example.dailyselfie.MainActivity;
import com.example.dailyselfie.R;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalField;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class AlarmReceiver extends BroadcastReceiver {

    public static long getEndOfDayMs() {
        final Calendar date = Calendar.getInstance(Locale.getDefault());

        date.set(Calendar.HOUR_OF_DAY,
                date.getActualMaximum(Calendar.HOUR_OF_DAY));
        date.set(Calendar.MINUTE, date.getActualMaximum(Calendar.MINUTE));
        date.set(Calendar.SECOND, date.getActualMaximum(Calendar.SECOND));
        date.set(Calendar.MILLISECOND,
                date.getActualMaximum(Calendar.MILLISECOND));
        return date.getTimeInMillis();
    }

    public static void remindAfterHour(Context context,double hour) {
//        WorkManager.getInstance(this).cancelAllWork();
//        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(RemindWorker.class,hour, TimeUnit.HOURS)
//                .setInitialDelay(hour, TimeUnit.HOURS)
//                .build();
//        WorkManager.getInstance(this).enqueue(periodicWorkRequest);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2206, i, PendingIntent.FLAG_IMMUTABLE);


        final long ALARM_DELAY_IN = Math.round(hour * 3600 * 1000);
        long alarmTimeAtUTC = System.currentTimeMillis() + ALARM_DELAY_IN;
        // Toast.makeText(context,ALARM_DELAY_IN + "",Toast.LENGTH_SHORT).show();
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeAtUTC, pendingIntent);
    }

    public static void remindAfterTime(Context context,long time) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2206, i, PendingIntent.FLAG_IMMUTABLE);


        final long ALARM_DELAY_IN = time;
        long alarmTimeAtUTC = System.currentTimeMillis() + ALARM_DELAY_IN;
        // Toast.makeText(context,ALARM_DELAY_IN + "",Toast.LENGTH_SHORT).show();
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeAtUTC, pendingIntent);
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent i = new Intent(context, AlarmReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 2206, i, PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intentThis) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int id = new Random().nextInt();

        PendingIntent pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String channelId = "123";
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Thông báo")
                .setContentText("Đã đến lúc selfie rồi bạn ơi..")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

        NotificationChannel channel = new NotificationChannel(channelId,
                "Thông báo nhắc nhở",
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        //NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManager.notify(id, notificationBuilder.build());

        remindAfterHour(context, 24D );
    }
}
