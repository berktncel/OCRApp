package com.berkt.ocrapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.berkt.ocrapp.databinding.ActivityMainBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var imageData: Uri

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        registerLauncher()

        binding.btSelectImage.setOnClickListener {

            // if not allowed
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                ) {
                    // rationale
                    Snackbar.make(
                        view, "Permission needed for gallery",
                        Snackbar.LENGTH_INDEFINITE
                    ).setAction("Give Permission", View.OnClickListener {
                        // request permission
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()

                } else {
                    // request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }

        }

        binding.btOcr.setOnClickListener {

            imageToText(imageData)

            // Ocr text for scroll property
            binding.tvOcr.movementMethod = ScrollingMovementMethod()
        }

    }

    private fun registerLauncher() {

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    imageData = intentFromResult.data!!
                    binding.ivSelectedImage.setImageURI(imageData)
                    // Ocr button is Enabled
                    binding.btOcr.isEnabled = true
                }
            }

        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                // permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                // permission denied
                Toast.makeText(this@MainActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun imageToText(imageUri: Uri) {
        val image = InputImage.fromFilePath(applicationContext, imageUri)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Task completed successfully
                if (visionText.text != "") {
                    binding.tvOcr.text = visionText.text
                } else {
                    binding.tvOcr.text = "No text was found in the image you selected."
                }

            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                binding.tvOcr.text = "Error: $e"
            }
    }

}