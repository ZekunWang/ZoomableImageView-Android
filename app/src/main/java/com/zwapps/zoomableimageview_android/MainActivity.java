package com.zwapps.zoomableimageview_android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.zwapps.zoomableimageview.ZoomableImageView;

import static com.zwapps.zoomableimageview_android.R.id.imageView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    ZoomableImageView zoomableImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        zoomableImageView = (ZoomableImageView) findViewById(imageView);

        zoomableImageView.setMaxScale(2);
        zoomableImageView.setMinScale(0.6f);
        zoomableImageView.setTranslationStickyFactor(0.2f);
        zoomableImageView.setShouldBounceFromMinScale(true);
        zoomableImageView.setShouldBounceToMaxScale(true);
        zoomableImageView.setShouldBounceFromTranslation(true);

        zoomableImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");
            }
        });

        zoomableImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "onLongClick");
                return true;
            }
        });

        zoomableImageView.setImageResource(R.drawable.demo_image);
    }
}
