package com.example.sunchan.goodsaying;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class AppearanceActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    private DisplayStyle draft;
    private File draftImage;
    private Bitmap previewImage;
    private File pendingPhoto;
    private PhotoBackgroundView editorPhoto;
    private AlertDialog photoDialog;
    private View appearanceRoot;
    private int measuredDetailWidth;
    private int measuredDetailHeight;
    private float measuredDetailAspect;
    private boolean loading;
    private boolean destroyed;
    private SeekBar sizeControl;
    private Spinner fontControl;
    private Spinner colorControl;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.appearance_layout);
        ScreenInsets.apply(this);
        ViewGroup content = findViewById(android.R.id.content);
        appearanceRoot = content.getChildAt(0);
        setSupportActionBar((Toolbar) findViewById(R.id.sub_toolbar));
        getSupportActionBar().setTitle(R.string.action_appearance);
        draft = DisplayStyle.load(this);
        if (state != null) {
            draft.font = state.getInt("font");
            draft.size = state.getInt("size");
            draft.color = state.getInt("color");
            draft.imageFile = state.getString("image");
            draft.photoX = state.getFloat("photo_x", draft.photoX);
            draft.photoY = state.getFloat("photo_y", draft.photoY);
            draft.photoZoom = state.getFloat("photo_zoom", draft.photoZoom);
            draft.photoOverlay = state.getInt("photo_overlay", draft.photoOverlay);
            String cacheName = state.getString("draft");
            if (cacheName != null) {
                draftImage = new File(getCacheDir(), cacheName);
            }
        }
        previewImage = draftImage == null ? draft.readSavedImage(this)
                : BitmapFactory.decodeFile(draftImage.getPath());

        fontControl = spinner(R.id.appearance_font, R.array.saying_fonts, draft.font);
        colorControl = spinner(R.id.appearance_color, R.array.saying_colors, draft.color);
        sizeControl = findViewById(R.id.appearance_size);
        sizeControl.setMax(DisplayStyle.MAX_SIZE - DisplayStyle.MIN_SIZE);
        sizeControl.setProgress(draft.size - DisplayStyle.MIN_SIZE);
        sizeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                draft.size = value + DisplayStyle.MIN_SIZE;
                showPreview();
            }
        });
        findViewById(R.id.pick_background).setOnClickListener(view -> {
            Intent picker = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                    .addCategory(Intent.CATEGORY_OPENABLE);
            try {
                startActivityForResult(picker, PICK_IMAGE);
            } catch (android.content.ActivityNotFoundException error) {
                showImageError();
            }
        });
        findViewById(R.id.remove_background).setOnClickListener(view -> {
            clearDraftImage();
            draft.imageFile = null;
            previewImage = null;
            showPreview();
        });
        findViewById(R.id.save_appearance).setOnClickListener(view -> saveAppearance());
        findViewById(R.id.cancel_appearance).setOnClickListener(view -> finish());
        findViewById(R.id.adjust_background).setOnClickListener(view ->
                editPhoto(previewImage, null, draft.photoX, draft.photoY,
                        draft.photoZoom, draft.photoOverlay));
        appearanceRoot.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                   oldLeft, oldTop, oldRight, oldBottom) ->
                sizeAppearancePreview());
        showPreview();
        if (state != null && state.containsKey("edit_zoom")) {
            appearanceRoot.post(() -> restoreEditor(state));
        }
    }

    private Spinner spinner(int id, int labels, int selected) {
        Spinner spinner = findViewById(id);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, labels,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(selected);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) {}
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent.getId() == R.id.appearance_font) {
                    draft.font = position;
                } else {
                    draft.color = position;
                }
                showPreview();
            }
        });
        return spinner;
    }

    private void showPreview() {
        ((TextView) findViewById(R.id.appearance_size_label)).setText(
                getString(R.string.appearance_size_value, draft.size));
        draft.apply(findViewById(R.id.appearance_preview), findViewById(R.id.preview_image),
                (TextView) findViewById(R.id.preview_text), previewImage);
        findViewById(R.id.adjust_background).setVisibility(previewImage == null ? View.GONE : View.VISIBLE);
        sizeAppearancePreview();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float detailAspect() {
        int width = appearanceRoot.getWidth() - appearanceRoot.getPaddingLeft()
                - appearanceRoot.getPaddingRight();
        int height = appearanceRoot.getHeight() - appearanceRoot.getPaddingTop()
                - appearanceRoot.getPaddingBottom();
        if (width <= 0 || height <= 0) {
            return 0f;
        }
        if (width == measuredDetailWidth && height == measuredDetailHeight) {
            return measuredDetailAspect;
        }
        // Measure the real detail layout with the current window, insets and font scale.
        View detail = getLayoutInflater().inflate(R.layout.noti_layout, null);
        detail.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        View background = detail.findViewById(R.id.noti_layout);
        measuredDetailWidth = width;
        measuredDetailHeight = height;
        measuredDetailAspect = (float) background.getMeasuredWidth()
                / Math.max(1, background.getMeasuredHeight());
        return measuredDetailAspect;
    }

    private void sizeAppearancePreview() {
        FrameLayout preview = findViewById(R.id.appearance_preview);
        View parent = (View) preview.getParent();
        int availableWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
        float aspect = previewImage == null || availableWidth <= 0 ? 0f : detailAspect();
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) preview.getLayoutParams();
        int width = aspect == 0f ? ViewGroup.LayoutParams.MATCH_PARENT
                : Math.max(1, Math.min(availableWidth, Math.round(dp(180) * aspect)));
        int height = aspect == 0f ? dp(180) : Math.max(1, Math.round(width / aspect));
        if (params.width != width || params.height != height) {
            params.width = width;
            params.height = height;
            params.gravity = Gravity.CENTER_HORIZONTAL;
            preview.setLayoutParams(params);
        }
    }

    private void editPhoto(Bitmap bitmap, File candidate, float x, float y, float zoom, int opacity) {
        if (bitmap == null) {
            if (candidate != null) candidate.delete();
            showImageError();
            return;
        }
        pendingPhoto = candidate;
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(dp(20), 0, dp(20), dp(8));
        TextView instructions = new TextView(this);
        instructions.setText(R.string.photo_crop_hint);
        controls.addView(instructions);

        FrameLayout crop = new FrameLayout(this);
        crop.setBackgroundColor(DisplayStyle.COLORS[draft.color]);
        LinearLayout.LayoutParams cropSize = new LinearLayout.LayoutParams(1, 1);
        cropSize.gravity = Gravity.CENTER_HORIZONTAL;
        cropSize.topMargin = dp(8);
        controls.addView(crop, cropSize);
        editorPhoto = new PhotoBackgroundView(this);
        editorPhoto.setEditing();
        editorPhoto.setPhoto(bitmap, x, y, zoom, opacity);
        crop.addView(editorPhoto, new FrameLayout.LayoutParams(-1, -1));
        TextView sample = new TextView(this);
        sample.setText(R.string.default_1);
        sample.setGravity(Gravity.CENTER);
        sample.setPadding(dp(8), 0, dp(8), 0);
        draft.applyText(sample, true);
        crop.addView(sample, new FrameLayout.LayoutParams(-1, -1));

        TextView overlayLabel = new TextView(this);
        controls.addView(overlayLabel);
        SeekBar overlay = new SeekBar(this);
        overlay.setMax(100);
        overlay.setProgress(opacity);
        controls.addView(overlay);
        updateOverlayLabel(overlayLabel, overlay, opacity);
        overlay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                editorPhoto.setOverlay(value);
                updateOverlayLabel(overlayLabel, bar, value);
            }
        });

        android.widget.ScrollView dialogScroll = new android.widget.ScrollView(this);
        dialogScroll.addView(controls);
        controls.addOnLayoutChangeListener((view, left, top, right, bottom,
                                             oldLeft, oldTop, oldRight, oldBottom) -> {
            int availableWidth = right - left - controls.getPaddingLeft() - controls.getPaddingRight();
            float aspect = detailAspect();
            if (aspect <= 0f || availableWidth <= 0) return;
            int maxHeight = Math.max(dp(64), (int) (appearanceRoot.getHeight() * 0.48f));
            int width = Math.max(1, Math.min(availableWidth, (int) (maxHeight * aspect)));
            int height = Math.max(1, Math.round(width / aspect));
            if (cropSize.width != width || cropSize.height != height) {
                cropSize.width = width;
                cropSize.height = height;
                crop.setLayoutParams(cropSize);
            }
        });
        photoDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.adjust_background)
                .setView(dialogScroll)
                .setNegativeButton(R.string.txt_cancel, null)
                .setPositiveButton(R.string.apply_photo, (ignored, which) -> {
                    if (pendingPhoto != null) {
                        clearDraftImage();
                        draftImage = pendingPhoto;
                        pendingPhoto = null;
                    }
                    previewImage = bitmap;
                    draft.photoX = editorPhoto.photoCenterX();
                    draft.photoY = editorPhoto.photoCenterY();
                    draft.photoZoom = editorPhoto.photoZoom();
                    draft.photoOverlay = editorPhoto.photoOverlay();
                    showPreview();
                })
                .create();
        photoDialog.setOnDismissListener(ignored -> {
            if (!isChangingConfigurations() && pendingPhoto != null) {
                pendingPhoto.delete();
                pendingPhoto = null;
            }
            editorPhoto = null;
            photoDialog = null;
        });
        photoDialog.show();
    }

    private void updateOverlayLabel(TextView label, SeekBar bar, int opacity) {
        String description = getString(R.string.photo_overlay_value, opacity);
        label.setText(description);
        bar.setContentDescription(description);
    }

    private void restoreEditor(Bundle state) {
        String candidateName = state.getString("edit_file");
        File candidate = candidateName == null ? null : new File(getCacheDir(), candidateName);
        Bitmap bitmap = candidate == null ? previewImage : BitmapFactory.decodeFile(candidate.getPath());
        editPhoto(bitmap, candidate, state.getFloat("edit_x"), state.getFloat("edit_y"),
                state.getFloat("edit_zoom"), state.getInt("edit_overlay"));
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_IMAGE && result == RESULT_OK && data != null && data.getData() != null) {
            loadImage(data.getData());
        }
    }

    private void loadImage(Uri uri) {
        loading = true;
        setImageControlsEnabled(false);
        new Thread(() -> {
            File file = new File(getCacheDir(), "preview-" + UUID.randomUUID() + ".jpg");
            Bitmap bitmap = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(input, null, options);
                }
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1600) {
                    options.inSampleSize *= 2;
                }
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    bitmap = BitmapFactory.decodeStream(input, null, options);
                }
                if (bitmap == null) {
                    throw new IOException("Unreadable image");
                }
                try (FileOutputStream output = new FileOutputStream(file)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) {
                        throw new IOException("Could not store image");
                    }
                }
            } catch (IOException | RuntimeException error) {
                file.delete();
                bitmap = null;
            }
            Bitmap result = bitmap;
            runOnUiThread(() -> {
                if (destroyed || isFinishing()) {
                    file.delete();
                    return;
                }
                loading = false;
                setImageControlsEnabled(true);
                if (result == null) {
                    showImageError();
                    return;
                }
                editPhoto(result, file, 0.5f, 0.5f, 1f, DisplayStyle.DEFAULT_PHOTO_OVERLAY);
            });
        }, "saying-background").start();
    }

    private void setImageControlsEnabled(boolean enabled) {
        findViewById(R.id.pick_background).setEnabled(enabled);
        findViewById(R.id.remove_background).setEnabled(enabled);
        findViewById(R.id.adjust_background).setEnabled(enabled);
        findViewById(R.id.save_appearance).setEnabled(enabled);
        colorControl.setEnabled(enabled);
    }

    private void saveAppearance() {
        if (loading) {
            return;
        }
        String previous = DisplayStyle.load(this).imageFile;
        String draftFileName = draft.imageFile;
        File saved = null;
        if (draftImage != null) {
            saved = new File(getFilesDir(), "saying-" + UUID.randomUUID() + ".jpg");
            if (!draftImage.renameTo(saved)) {
                showImageError();
                return;
            }
            draft.imageFile = saved.getName();
        }
        if (!draft.save(this)) {
            draft.imageFile = draftFileName;
            if (saved != null) {
                saved.renameTo(draftImage);
            }
            showImageError();
            return;
        }
        if (previous != null && !previous.equals(draft.imageFile)) {
            new File(getFilesDir(), previous).delete();
        }
        draftImage = null;
        finish();
    }

    private void showImageError() {
        Toast.makeText(this, R.string.appearance_image_error, Toast.LENGTH_LONG).show();
    }

    private void clearDraftImage() {
        if (draftImage != null) {
            draftImage.delete();
            draftImage = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("font", draft.font);
        state.putInt("size", draft.size);
        state.putInt("color", draft.color);
        state.putString("image", draft.imageFile);
        state.putString("draft", draftImage == null ? null : draftImage.getName());
        state.putFloat("photo_x", draft.photoX);
        state.putFloat("photo_y", draft.photoY);
        state.putFloat("photo_zoom", draft.photoZoom);
        state.putInt("photo_overlay", draft.photoOverlay);
        if (editorPhoto != null) {
            state.putString("edit_file", pendingPhoto == null ? null : pendingPhoto.getName());
            state.putFloat("edit_x", editorPhoto.photoCenterX());
            state.putFloat("edit_y", editorPhoto.photoCenterY());
            state.putFloat("edit_zoom", editorPhoto.photoZoom());
            state.putInt("edit_overlay", editorPhoto.photoOverlay());
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (photoDialog != null) {
            photoDialog.dismiss();
        }
        if (isFinishing()) {
            clearDraftImage();
            if (pendingPhoto != null) pendingPhoto.delete();
        }
        super.onDestroy();
    }
}
