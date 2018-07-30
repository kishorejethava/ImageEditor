package com.android.photoeditor

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.droidninja.imageeditengine.ImageEditor
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val rxPermissions = RxPermissions(this)
    select_image_btn.setOnClickListener {

      rxPermissions
          .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE)
          .subscribe({ granted ->
            if (granted) { // Always true pre-M
              // I can control the camera now
                ImageEditor.Builder(this, "file:///android_asset/sachin.jpg")
                  .setStickerAssets("stickers")
                  .open()

            } else {
              // Oups permission denied
              Toast.makeText(this, "Not given permission", Toast.LENGTH_SHORT).show()
            }
          })

    }
  }
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      ImageEditor.RC_IMAGE_EDITOR ->
        if (resultCode == Activity.RESULT_OK && data != null) {
          val imagePath: String = data.getStringExtra(ImageEditor.EXTRA_EDITED_PATH)
          edited_image.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        }
    }
  }
}
