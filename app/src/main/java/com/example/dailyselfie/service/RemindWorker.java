package com.example.dailyselfie.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.WorkerParameters;

import com.example.dailyselfie.MainActivity;
import com.example.dailyselfie.R;

import java.util.Random;

public class RemindWorker extends androidx.work.Worker {

    public RemindWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        sendNotification();
        return Result.success();
    }

    private void sendNotification() {

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int id = new Random().nextInt();

        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(),id,intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String channelId = "123";
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(),channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Thông báo")
                .setContentText("Đã đến lúc selfie rồi bạn ơi..")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);

        NotificationChannel channel = new NotificationChannel(channelId,
                "Thông báo nhắc nhở",
                NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        //NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManager.notify(id, notificationBuilder.build());
    }
}
