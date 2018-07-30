package com.droidninja.imageeditengine;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.droidninja.imageeditengine.utils.FragmentUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static com.droidninja.imageeditengine.ImageEditor.EXTRA_IMAGE_PATH;

public class ImageEditActivity extends BaseImageEditActivity
    implements PhotoEditorFragment.OnFragmentInteractionListener,
    CropFragment.OnFragmentInteractionListener {
  private Rect cropRect;

  //private View touchView;

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_image_edit);

    String imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
    if (imagePath != null) {
      PhotoEditorFragment photoEditorFragment = PhotoEditorFragment.newInstance(imagePath);
      FragmentUtil.addFragment(this, R.id.fragment_container, photoEditorFragment);
    }
  }

  @Override public void onCropClicked(Bitmap bitmap) {
    FragmentUtil.replaceFragment(this, R.id.fragment_container,
        CropFragment.newInstance(bitmap, cropRect));
  }

  @Override public void onDoneClicked(String imagePath) {

    Intent intent = new Intent();
    intent.putExtra(ImageEditor.EXTRA_EDITED_PATH, imagePath);
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  @Override public void onImageCropped(Bitmap bitmap, Rect cropRect) {
    this.cropRect = cropRect;

    String path = "";
    try {
      OutputStream output;
      File folder = new File(Environment.getExternalStorageDirectory() + "/whatsappCamera");
      if (!folder.exists()) {
        folder.mkdirs();
      }
      File file = new File(folder.getAbsolutePath(),  "cropped.jpg");
      try {
        output = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
        output.flush();
        output.close();
        path = file.getAbsolutePath();
      } catch (Exception e) {
        e.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    PhotoEditorFragment photoEditorFragment = PhotoEditorFragment.newInstance(path);
    FragmentUtil.replaceFragment(this, R.id.fragment_container,
      photoEditorFragment);

      photoEditorFragment.reset();
      FragmentUtil.removeFragment(this,
          (BaseFragment) FragmentUtil.getFragmentByTag(this, CropFragment.class.getSimpleName()));
  }

  @Override public void onCancelCrop() {
    FragmentUtil.removeFragment(this,
        (BaseFragment) FragmentUtil.getFragmentByTag(this, CropFragment.class.getSimpleName()));
  }

  @Override public void onBackPressed() {
    super.onBackPressed();
  }
}
