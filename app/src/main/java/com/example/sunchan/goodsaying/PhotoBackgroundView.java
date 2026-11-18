package com.example.sunchan.goodsaying;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/** Draws the same photo framing in the editor, Appearance preview, and saying detail. */
public final class PhotoBackgroundView extends View {
    private static final float MIN_FIT_FRACTION = 0.5f;
    private static final float MAX_COVER_ZOOM = 5f;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private Bitmap photo;
    private float centerX = 0.5f;
    private float centerY = 0.5f;
    private float zoom = 1f;
    private int overlay = DisplayStyle.DEFAULT_PHOTO_OVERLAY;
    private boolean editing;
    private float lastX;
    private float lastY;
    private final ScaleGestureDetector scaleGesture;

    public PhotoBackgroundView(Context context) {
        this(context, null);
    }

    public PhotoBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleGesture = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float oldScale = coverScale() * zoom;
                        float x = (detector.getFocusX() - getWidth() / 2f) / photo.getWidth();
                        float y = (detector.getFocusY() - getHeight() / 2f) / photo.getHeight();
                        zoom = Math.max(minimumZoom(),
                                Math.min(MAX_COVER_ZOOM, zoom * detector.getScaleFactor()));
                        float newScale = coverScale() * zoom;
                        centerX += x / oldScale - x / newScale;
                        centerY += y / oldScale - y / newScale;
                        constrainPosition();
                        invalidate();
                        return true;
                    }
                });
        if (android.os.Build.VERSION.SDK_INT >= 19) {
            scaleGesture.setQuickScaleEnabled(false);
        }
    }

    void setEditing() {
        editing = true;
    }

    void setPhoto(Bitmap bitmap, float x, float y, float scale, int opacity) {
        photo = bitmap;
        centerX = x;
        centerY = y;
        zoom = scale;
        setOverlay(opacity);
    }

    void setOverlay(int opacity) {
        overlay = opacity;
        invalidate();
    }

    float photoCenterX() { return centerX; }
    float photoCenterY() { return centerY; }
    float photoZoom() { return zoom; }
    int photoOverlay() { return overlay; }

    private float coverScale() {
        return Math.max((float) getWidth() / photo.getWidth(),
                (float) getHeight() / photo.getHeight());
    }

    private float minimumZoom() {
        float fitScale = Math.min((float) getWidth() / photo.getWidth(),
                (float) getHeight() / photo.getHeight());
        // Keep saved zoom values relative to the cover scale for existing crops.
        return MIN_FIT_FRACTION * fitScale / coverScale();
    }

    private void constrainPosition() {
        if (photo == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float scale = coverScale() * zoom;
        centerX = boundedCenter(centerX, getWidth(), photo.getWidth() * scale);
        centerY = boundedCenter(centerY, getHeight(), photo.getHeight() * scale);
    }

    private float boundedCenter(float center, int viewportSize, float photoSize) {
        // Keep small photos inside the frame and let larger photos pan to either edge.
        float travel = Math.abs(0.5f - viewportSize / (2f * photoSize));
        return Math.max(0.5f - travel, Math.min(0.5f + travel, center));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (photo == null) {
            return;
        }
        float scale = coverScale() * zoom;
        float width = photo.getWidth() * scale;
        float height = photo.getHeight() * scale;
        float left = getWidth() / 2f - width * boundedCenter(centerX, getWidth(), width);
        float top = getHeight() / 2f - height * boundedCenter(centerY, getHeight(), height);
        destination.set(left, top, left + width, top + height);
        canvas.drawBitmap(photo, null, destination, paint);
        int saveCount = canvas.save();
        canvas.clipRect(destination);
        canvas.drawColor(Color.argb(Math.round(overlay * 255f / 100f), 255, 255, 255));
        canvas.restoreToCount(saveCount);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editing || photo == null) {
            return super.onTouchEvent(event);
        }
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            constrainPosition();
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        boolean wasScaling = scaleGesture.isInProgress();
        scaleGesture.onTouchEvent(event);
        int index = action == MotionEvent.ACTION_POINTER_UP && event.getActionIndex() == 0 ? 1 : 0;
        float x = event.getX(index);
        float y = event.getY(index);
        if (action == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1
                && !wasScaling && !scaleGesture.isInProgress()) {
            float scale = coverScale() * zoom;
            centerX -= (x - lastX) / (photo.getWidth() * scale);
            centerY -= (y - lastY) / (photo.getHeight() * scale);
            constrainPosition();
            invalidate();
        }
        lastX = x;
        lastY = y;
        return true;
    }
}
