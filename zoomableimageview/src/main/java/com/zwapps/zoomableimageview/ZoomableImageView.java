package com.zwapps.zoomableimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by zekunwang on 9/23/17.
 */

public class ZoomableImageView extends AppCompatImageView {
    private static final String TAG = ZoomableImageView.class.getSimpleName();

    private static int ACTION_MOVE_THRESHOLD = 20;

    private enum Status {
        NON_CLICKABLE, CLICKABLE, DRAGGING, SCALING
    }

    private Drawable drawable;

    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private Matrix matrix ;
    private float[] values;
    private float initScale = Float.MAX_VALUE;
    private Status status = Status.NON_CLICKABLE;

    private float moveX, moveY;
    private View.OnClickListener clickListener;
    private View.OnLongClickListener longClickListener;

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

        if (status == Status.SCALING) {
            updateMinMaxScale();
            updateScaleTrans();

        } else {
            if (!shouldBounceFromTranslation) {
                transToBounds();
            }

            // TODO: reaching scaleToFit translation down scales down and animate out image

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
        if (status == Status.SCALING) {
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

        boolean shouldMove;

        int action = event.getActionMasked();

        // Manage click listeners
        if (action == MotionEvent.ACTION_DOWN) {
            if (clickListener != null || longClickListener != null) {
                updateClickListeners(true);
            }

            moveX = event.getAxisValue(MotionEvent.AXIS_X);
            moveY = event.getAxisValue(MotionEvent.AXIS_Y);

        } else if (status == Status.CLICKABLE && action == MotionEvent.ACTION_POINTER_DOWN) {
            // Do not trigger click event if is clickable but move far enough or about to scale
            updateClickListeners(false);

        } else if (status == Status.CLICKABLE && action == MotionEvent.ACTION_MOVE) {
            float diffX = Math.abs(event.getAxisValue(MotionEvent.AXIS_X) - moveX);
            float diffY = Math.abs(event.getAxisValue(MotionEvent.AXIS_Y) - moveY);

            shouldMove = diffX >= ACTION_MOVE_THRESHOLD || diffY >= ACTION_MOVE_THRESHOLD;

            // Do not trigger click event if is clickable but move far enough or about to scale
            if (shouldMove) {
                updateClickListeners(false);
            }
        }

        scaleGestureDetector.onTouchEvent(event);

        if (!scaleGestureDetector.isInProgress()) {
            gestureDetector.onTouchEvent(event);
        }

        if (action == MotionEvent.ACTION_UP && status == Status.DRAGGING) {
            transToBounds();
            matrix.setValues(values);
            setImageMatrix(matrix);
        }

        return super.onTouchEvent(event);
    }

    private void updateClickListeners(boolean enabled) {
        if (enabled) {
            super.setOnClickListener(clickListener);
            super.setOnLongClickListener(longClickListener);
            status = Status.CLICKABLE;
        } else {
            super.setOnClickListener(null);
            super.setOnLongClickListener(null);
            status = Status.NON_CLICKABLE;
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        clickListener = l;
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener l) {
        longClickListener = l;
    }

    public Status getStatus() {
        return status;
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

    public boolean isShouldBounceFromTranslation() {
        return shouldBounceFromTranslation;
    }

    public void setShouldBounceFromTranslation(boolean shouldBounceFromTranslation) {
        this.shouldBounceFromTranslation = shouldBounceFromTranslation;
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
            status = Status.SCALING;
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
            status = Status.DRAGGING;

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
