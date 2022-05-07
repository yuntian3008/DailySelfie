package com.example.dailyselfie;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import com.example.dailyselfie.databinding.ActivityMainBinding;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements GalleryAdapter.ItemClickListener {

    private ActivityMainBinding binding;
    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ShapeableImageView largeImage;
    private List<Bitmap> list;
    private File[] images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

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