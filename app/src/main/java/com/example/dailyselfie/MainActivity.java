package com.example.dailyselfie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import com.example.dailyselfie.databinding.ActivityMainBinding;
import com.example.dailyselfie.service.AlarmReceiver;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GalleryAdapter.ItemClickListener {

    private ActivityMainBinding binding;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ShapeableImageView largeImage;
    private List<Bitmap> list;
    private File[] images;
    private SharedPreferences prefs;
    static final int SECOND = 1000;        // no. of ms in a second
    static final int MINUTE = SECOND * 60; // no. of ms in a minute
    static final int HOUR = MINUTE * 60;   // no. of ms in an hour
    static final int DAY = HOUR * 24;      // no. of ms in a day
    static final int WEEK = DAY * 7;       // no. of ms in a week

    private void setTimeForNotification(boolean isToast) {
        AlarmReceiver.cancel(getApplicationContext());
        int hour = prefs.getInt("hour",9);
        int minute = prefs.getInt("minute",0);
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.set(Calendar.HOUR_OF_DAY,hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND,0);
        long nextTime;
        if (calendar.getTimeInMillis() - Calendar.getInstance(Locale.getDefault()).getTimeInMillis() < 0) {
            //Toast.makeText(this, "Đã qua " + hour + " giờ",Toast.LENGTH_SHORT).show();
            nextTime = (calendar.getTimeInMillis() - Calendar.getInstance(Locale.getDefault()).getTimeInMillis()) + (1_000L * 3600 * 24);
        }

        else {
            //Toast.makeText(this, "Chưa tới " + hour + " giờ",Toast.LENGTH_SHORT).show();
            nextTime = (calendar.getTimeInMillis() - Calendar.getInstance(Locale.getDefault()).getTimeInMillis());
        }

        AlarmReceiver.remindAfterTime(getApplicationContext(),nextTime);
//        Toast.makeText(this, "Còn "+((double)nextTime / 3600D / 1000D) + " giờ nữa sẽ đến thời gian tự sướng.",Toast.LENGTH_SHORT).show();
//        Toast.makeText(this, "Còn "+nextTime + " miliseconds nữa sẽ đến thời gian tự sướng.",Toast.LENGTH_SHORT).show();
        int hourR   = (int)((nextTime % DAY) / HOUR);
        int minuteR = (int)((nextTime % HOUR) / MINUTE);
        int secondR = (int)((nextTime % MINUTE) / SECOND);
        if (isToast)
        Toast.makeText(this, "Còn "+ (hourR > 0 ? hourR + " giờ " : "") + (minuteR > 0 ? minuteR + " phút " : "") + (secondR > 0 ? secondR + " giây " : "") + " nữa sẽ đến thời gian selfie.",Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(!prefs.getBoolean("firstTime", false)) {
            // run your one time code
            setTimeForNotification(false);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("firstTime", true);
            editor.apply();
        }

        initViews();
        initListeners();
        initRecycler();

        setContentView(binding.getRoot());
    }

    private void initViews() {
        toolbar = binding.toolbar;
        recyclerView = binding.recycler;
        largeImage = binding.largeImage;
    }

    private void initListeners() {
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.camera) {
                Intent i = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(i);
            }
            if (item.getItemId() == R.id.clock) {
                MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                        .setTimeFormat(TimeFormat.CLOCK_24H)
                        .setHour(prefs.getInt("hour",9))
                        .setTitleText("Chọn thời gian thông báo mỗi ngày")
                        .build();
                materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("hour", materialTimePicker.getHour());
                        editor.putInt("minute", materialTimePicker.getMinute());
                        editor.apply();
                        setTimeForNotification(true);
                    }
                });
                materialTimePicker.show(getSupportFragmentManager(),"GICUNGDC");
            }
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (largeImage.getVisibility() == View.VISIBLE)
            hideLargeImage();
        else
            super.onBackPressed();
    }

    private void showLargeImage(Bitmap bitmap) {
        toolbar.setNavigationIcon(getDrawable(R.drawable.ic_arrow_sm_left));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideLargeImage();
            }
        });
        largeImage.setImageBitmap(bitmap);
        Transition transition = new CircularRevealTransition();
        transition.setDuration(600);
        transition.addTarget(largeImage);

        TransitionManager.beginDelayedTransition(binding.getRoot(), transition);
        largeImage.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void  hideLargeImage() {
        Transition transition = new Fade();
        transition.setDuration(600);
        transition.addTarget(recyclerView);

        TransitionManager.beginDelayedTransition(binding.getRoot(), transition);
        largeImage.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        toolbar.setNavigationIcon(null);
    }

    private void initRecycler() {
        File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toURI());
        if (folder.exists()) {
            images = folder.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return (s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png"));
                }
            });
            list = new ArrayList<>();
            for (File image : images) {
                list.add(BitmapFactory.decodeFile(image.getPath()));
            }
            recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
            GalleryAdapter galleryAdapter = new GalleryAdapter(this,list);
            galleryAdapter.setClickListener(this);
            recyclerView.setAdapter(galleryAdapter);
        }
        else
            Toast.makeText(this,"Không thể load thư mục ảnh selfie",Toast.LENGTH_LONG).show();
    }


    @Override
    public void onItemClick(View view, int position) {
        if (list != null)
        showLargeImage(list.get(position));
    }

    @Override
    public void onItemLongClick(View view, int position) {
        if (images != null) {
            new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
                    .setTitle("Xóa ảnh ?")
                    .setMessage("Bạn có chắc muốn xóa ảnh này ?")
                    .setPositiveButton("Xóa", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (images[position].delete()) {
                                initRecycler();
                                Toast.makeText(MainActivity.this, "Xóa thành công", Toast.LENGTH_SHORT).show();

                            }

                            else
                                Toast.makeText(MainActivity.this, "Đã có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNeutralButton("Hủy", (dialogInterface, i) -> dialogInterface.dismiss()).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initRecycler();
        hideLargeImage();
    }
}