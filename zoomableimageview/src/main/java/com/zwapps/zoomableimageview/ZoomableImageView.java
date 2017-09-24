package com.zwapps.zoomableimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/**
 * Created by zekunwang on 9/23/17.
 */

public class ZoomableImageView extends AppCompatImageView {
    private static final String TAG = ZoomableImageView.class.getSimpleName();

    private Drawable drawable;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private Matrix matrix ;
    private float[] values;
    private boolean isScaling = false;
    private float initScale = Float.MAX_VALUE;

    private float maxScale = 3;
    private float minScale = 0.4f;
    private float translationStickyFactor = 0.25f;
    private boolean shouldBounceToMaxScale = true;
    private boolean shouldBounceFromMinScale  = true;
    private boolean shouldBounceFromTranslation = true;

    public ZoomableImageView(Context context) {
        super(context);
        initialize(null);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(attrs);
    }

    private void initialize(@Nullable AttributeSet attrs) {
        setScaleType(ScaleType.MATRIX);

        values = new float[9];

        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());

        if (attrs != null) {
            TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.ZoomableImageView);

            maxScale = attributes.getFloat(R.styleable.ZoomableImageView_maxScale, 3);
            minScale = attributes.getFloat(R.styleable.ZoomableImageView_minScale, 0.4f);
            translationStickyFactor = attributes.getFloat(R.styleable.ZoomableImageView_translationStickyFactor, 0.25f);
            shouldBounceToMaxScale = attributes.getBoolean(R.styleable.ZoomableImageView_shouldBounceToMaxScale, true);
            shouldBounceFromMinScale = attributes.getBoolean(R.styleable.ZoomableImageView_shouldBounceFromMinScale, true);
            shouldBounceFromTranslation = attributes.getBoolean(R.styleable.ZoomableImageView_shouldBounceFromTranslation, true);
            Log.e(TAG, "maxScale: " + maxScale);
            Log.e(TAG, "minScale: " + minScale);
            Log.e(TAG, "translationStickyFactor: " + translationStickyFactor);
            Log.e(TAG, "shouldBounceToMaxScale: " + shouldBounceToMaxScale);
            Log.e(TAG, "shouldBounceFromMinScale: " + shouldBounceFromMinScale);
            Log.e(TAG, "shouldBounceFromTranslation: " + shouldBounceFromTranslation);
            attributes.recycle();
        }
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        // setImageResource, setImageBitmap all redirect to setImageDrawable
        this.drawable = drawable;
        initializeMatrix();
    }

    private void initializeMatrix() {
        if (drawable == null) {
            return;
        }

        // Make sure getWidth() and getHeight() is not 0
        post(new Runnable() {
            @Override
            public void run() {
                // Get initScale for scale fit
                float scaleX = getWidth() / (float) drawable.getIntrinsicWidth();
                float scaleY = getHeight() / (float) drawable.getIntrinsicHeight();
                initScale = Math.min(scaleX, scaleY);

                // Init matrix and values
                matrix = new Matrix();
                matrix.postScale(initScale, initScale);
                matrix.getValues(values);

                // Center scaled image
                values[Matrix.MTRANS_X] = (getWidth() - drawable.getIntrinsicWidth() * initScale) / 2;
                values[Matrix.MTRANS_Y] = (getHeight() - drawable.getIntrinsicHeight() * initScale) / 2;

                matrix.setValues(values);
                setImageMatrix(matrix);
            }
        });
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        matrix.getValues(values);

        if (isScaling) {
            updateMinMaxScale();
            updateScaleTrans();

        } else {
            if (!shouldBounceFromTranslation) {
                transToBounds();
            }

            // TODO: reach scaleToFit translation down scales down and animate out image

        }

        matrix.setValues(values);
        super.setImageMatrix(matrix);
    }

    private void updateMinMaxScale() {
        float scaleFactor = values[Matrix.MSCALE_X];

        float chosenMinScale = shouldBounceFromMinScale ? minScale * initScale : initScale;
        scaleFactor = Math.max(scaleFactor, chosenMinScale);

        if (!shouldBounceToMaxScale) {
            scaleFactor = Math.min(scaleFactor, maxScale * initScale);
        }

        values[Matrix.MSCALE_X] = values[Matrix.MSCALE_Y] = scaleFactor;
    }

    private void updateScaleTrans() {
        updateScaleTransForOneDirection(Matrix.MTRANS_X, drawable.getIntrinsicWidth() * values[Matrix.MSCALE_X], getWidth());
        updateScaleTransForOneDirection(Matrix.MTRANS_Y, drawable.getIntrinsicHeight() * values[Matrix.MSCALE_Y], getHeight());
    }

    private void updateScaleTransForOneDirection(int matrixIndex, float imageSize, float screenSize) {
        float diff = screenSize - imageSize;
        if (imageSize > screenSize) {
            // Stick axis to min bound
            values[matrixIndex] = Math.min(values[matrixIndex], 0);
            // Stick axis to max bound
            values[matrixIndex] = Math.max(values[matrixIndex], diff);

        } else {
            // Stick axis to center
            values[matrixIndex] = diff / 2;
        }
    }

    private float updateGestureDistance(float distance, int matrixIndex, float imageSize, float screenSize) {
        float diff = screenSize - imageSize;
        if (imageSize > screenSize) {
            // Stick axis to min & max bounds
            if (values[matrixIndex] > 0 || values[matrixIndex] < diff) {
                distance *= shouldBounceFromTranslation ? translationStickyFactor : 0;
            }

        } else {
            // Stay axis to bounds
            distance = 0;
        }

        return distance;
    }

    private void scaleToInit(float fromScale, float focusX, float focusY) {
        float diffScale = initScale / fromScale;
        matrix.postScale(diffScale, diffScale, focusX, focusY);
        setImageMatrix(matrix);

    }

    private void scaleToMax(float fromScale, float focusX, float focusY) {
        float diffScale = initScale * maxScale / fromScale;
        matrix.postScale(diffScale, diffScale, focusX, focusY);
        setImageMatrix(matrix);
    }

    private void transToBounds() {
        if (isScaling) {
            return;
        }

        float fromTransX = values[Matrix.MTRANS_X];
        float fromTransY = values[Matrix.MTRANS_Y];
        float diffWidth = getWidth() - drawable.getIntrinsicWidth() * values[Matrix.MSCALE_X];
        float diffHeight = getHeight() - drawable.getIntrinsicHeight() * values[Matrix.MSCALE_Y];

        // Pick bounds to trans toward
        float toTransX = diffWidth < 0 && 0 < fromTransX
                ? 0
                : (diffWidth < 0 && fromTransX < diffWidth ? diffWidth : fromTransX);
        float toTransY = diffHeight < 0 && 0 < fromTransY
                ? 0
                : (diffHeight < 0 && fromTransY < diffHeight ? diffHeight : fromTransY);

        if (fromTransX != toTransX || fromTransY != toTransY) {
            values[Matrix.MTRANS_X] = toTransX;
            values[Matrix.MTRANS_Y] = toTransY;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (drawable == null) {
            return super.onTouchEvent(event);
        }

        scaleGestureDetector.onTouchEvent(event);

        if (!scaleGestureDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP && !isScaling) {
            transToBounds();
            matrix.setValues(values);
            setImageMatrix(matrix);
        }

        return true;
    }

    public boolean isScaling() {
        return isScaling;
    }

    public float getInitScale() {
        return initScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public boolean isShouldBounceToMaxScale() {
        return shouldBounceToMaxScale;
    }

    public void setShouldBounceToMaxScale(boolean shouldBounceToMaxScale) {
        this.shouldBounceToMaxScale = shouldBounceToMaxScale;
    }

    public boolean isShouldBounceFromMinScale() {
        return shouldBounceFromMinScale;
    }

    public void setShouldBounceFromMinScale(boolean shouldBounceFromMinScale) {
        this.shouldBounceFromMinScale = shouldBounceFromMinScale;
    }

    public float getTranslationStickyFactor() {
        return translationStickyFactor;
    }

    public void setTranslationStickyFactor(float translationStickyFactor) {
        this.translationStickyFactor = translationStickyFactor;
    }

    private final class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            isScaling = true;
            // getScaleFactor() is delta value
            matrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                    detector.getFocusX(), detector.getFocusY());
            setImageMatrix(matrix);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);

            matrix.getValues(values);

            float scaleFactor = values[Matrix.MSCALE_X];

            if (scaleFactor < initScale) {
                scaleToInit(scaleFactor, detector.getFocusX(), detector.getFocusY());

            } else if (scaleFactor > initScale * maxScale) {
                scaleToMax(scaleFactor, detector.getFocusX(), detector.getFocusY());
            }
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            isScaling = false;

            // Apply translationStickyFactor if exceeds bounds
            distanceX = updateGestureDistance(distanceX, Matrix.MTRANS_X, drawable.getIntrinsicWidth() * values[Matrix.MSCALE_X], getWidth());
            distanceY = updateGestureDistance(distanceY, Matrix.MTRANS_Y, drawable.getIntrinsicHeight() * values[Matrix.MSCALE_Y], getHeight());

            // distanceX and distanceY are delta values
            matrix.postTranslate(-distanceX, -distanceY);
            setImageMatrix(matrix);
            return true;
        }
    }
}
