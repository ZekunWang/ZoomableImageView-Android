package com.zwapps.zoomableimageview_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zwapps.zoomableimageview.ZoomableImageView;

public class MainActivity extends AppCompatActivity {

    ZoomableImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ZoomableImageView) findViewById(R.id.imageView);
    }
}
