Download
--------
Use Gradle:

```gradle
dependencies {
    // ZoomableImageView
    compile 'com.github.ZekunWang:ZoomableImageView-Android:0.1.0'
}
```
How do I use?
-------------------
Configure zoomable image view in layout xml file:
```xml
    <com.zwapps.zoomableimageview.ZoomableImageView
        android:id="@+id/imageView"
        android:layout_width="350dp"
        android:layout_height="450dp"

        app:maxScale="2"
        app:minScale="0.6"
        app:translationStickyFactor="0.2"
        app:shouldBounceFromMinScale="true"
        app:shouldBounceToMaxScale="true"
        app:shouldBounceFromTranslation="true"
        android:src="@drawable/demo_image"
        android:background="#ff0000"/>
```

Or configure zoomable image view in code:
```java
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
```
## License

    Copyright [2016] [Zekun Wang]

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.