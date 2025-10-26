package com.example.socmedss.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.GlobalScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socmedss.R
import com.example.socmedss.databinding.ActivityEditPostBinding
import com.example.socmedss.util.HybridImageUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

/**
 * Edit Post Activity
 * 
 * Allows users to edit their post content and/or image.
 */
class EditPostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPostBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private var postId: String = ""
    private var currentImageUrl: String? = null
    private var selectedImageUri: Uri? = null
    private var imageChanged: Boolean = false

    /**
     * Permission launcher for requesting storage permissions
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(
                this,
                "Permission denied. Cannot access images.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Activity result launcher for picking images from gallery
     */
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                imageChanged = true
                showImagePreview(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        postId = intent.getStringExtra("POST_ID") ?: ""
        
        if (postId.isEmpty()) {
            Toast.makeText(this, "Invalid post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        loadPost()
        setupClickListeners()
    }

    /**
     * Sets up the toolbar with back navigation
     */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.toolbar.title = "Edit Post"
    }

    /**
     * Sets up click listeners for buttons
     */
    private fun setupClickListeners() {
        // Change image button
        binding.btnChangeImage.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
        
        // Remove image button
        binding.fabRemoveImage.setOnClickListener {
            removeImage()
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            savePost()
        }
    }

    /**
     * Loads post data from Firestore
     */
    private fun loadPost() {
        showLoading(true)
        
        firestore.collection("posts")
            .document(postId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                
                if (document.exists()) {
                    val text = document.getString("text") ?: ""
                    currentImageUrl = document.getString("imageUrl")
                    
                    binding.etPostText.setText(text)
                    
                    // Load current image if exists
                    if (!currentImageUrl.isNullOrEmpty()) {
                        binding.cardImagePreview.visibility = View.VISIBLE
                        Glide.with(this)
                            .load(currentImageUrl)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_launcher_background)
                            .error(R.drawable.ic_launcher_background)
                            .into(binding.ivImagePreview)
                    } else {
                        binding.cardImagePreview.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to load post: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
    }

    /**
     * Checks permissions before opening image picker
     */
    private fun checkPermissionsAndOpenPicker() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openImagePicker()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else -> {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openImagePicker()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    /**
     * Opens the device's image picker
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }

    /**
     * Shows preview of selected image
     */
    private fun showImagePreview(uri: Uri) {
        binding.ivImagePreview.setImageURI(uri)
        binding.cardImagePreview.visibility = View.VISIBLE
        binding.fabRemoveImage.visibility = View.VISIBLE
    }

    /**
     * Removes the selected image
     */
    private fun removeImage() {
        selectedImageUri = null
        imageChanged = true
        binding.cardImagePreview.visibility = View.GONE
        binding.ivImagePreview.setImageURI(null)
    }

    /**
     * Saves the edited post
     */
    private fun savePost() {
        val newText = binding.etPostText.text.toString().trim()
        
        // Validate input
        if (newText.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        
        if (imageChanged && selectedImageUri != null) {
            // Upload new image
            uploadNewImageAndSave(newText)
        } else if (imageChanged && selectedImageUri == null) {
            // Image was removed
            savePostWithNewData(newText, null)
        } else {
            // Only text changed
            savePostWithNewData(newText, currentImageUrl)
        }
    }

    /**
     * Uploads new image and saves post
     */
    private fun uploadNewImageAndSave(newText: String) {
        val imageUri = selectedImageUri ?: return
        val userId = auth.currentUser?.uid ?: return
        
        GlobalScope.launch {
            try {
                val imageUrl = HybridImageUploader.uploadImage(this@EditPostActivity, imageUri, userId)
                
                if (imageUrl != null) {
                    savePostWithNewData(newText, imageUrl)
                } else {
                    showLoading(false)
                    Toast.makeText(
                        this@EditPostActivity,
                        "Failed to upload image. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@EditPostActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Saves post data to Firestore
     */
    private fun savePostWithNewData(newText: String, imageUrl: String?) {
        firestore.collection("posts")
            .document(postId)
            .update(
                "text", newText,
                "imageUrl", imageUrl
            )
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Post updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Failed to update post: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Shows or hides the modern loading overlay
     */
    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !show
        binding.btnChangeImage.isEnabled = !show
    }
}

