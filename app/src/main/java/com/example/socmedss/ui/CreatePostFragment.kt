package com.example.socmedss.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.socmedss.R
import com.example.socmedss.databinding.FragmentCreatePostBinding
import com.example.socmedss.model.Post
import com.example.socmedss.util.ImgBBUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*

/**
 * Create Post Fragment
 * 
 * Allows users to create new posts with text and optional images.
 * Uploads images to ImgBB and saves post data to Firestore.
 */
class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private var selectedImageUri: Uri? = null
    private var currentUsername: String = "Anonymous"
    private var currentProfileImageUrl: String? = null

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
                requireContext(),
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
                showImagePreview(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        loadCurrentUsername()
        setupClickListeners()
    }

    /**
     * Loads the current user's username from Firestore
     */
    private fun loadCurrentUsername() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                currentUsername = document.getString("username") ?: "Anonymous"
                currentProfileImageUrl = document.getString("profileImage")
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to load user info: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Sets up click listeners for buttons
     */
    private fun setupClickListeners() {
        // Add image button
        binding.btnAddImage.setOnClickListener {
            checkPermissionsAndOpenPicker()
        }
        
        // Remove image button
        binding.fabRemoveImage.setOnClickListener {
            removeImage()
        }
        
        // Post button
        binding.btnPost.setOnClickListener {
            createPost()
        }
    }

    /**
     * Checks permissions before opening image picker
     */
    private fun checkPermissionsAndOpenPicker() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ uses READ_MEDIA_IMAGES
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    openImagePicker()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else -> {
                // Android 12 and below use READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
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
     * 
     * @param uri URI of the selected image
     */
    private fun showImagePreview(uri: Uri) {
        binding.ivImagePreview.setImageURI(uri)
        binding.cardImagePreview.visibility = View.VISIBLE
    }

    /**
     * Removes the selected image
     */
    private fun removeImage() {
        selectedImageUri = null
        binding.cardImagePreview.visibility = View.GONE
        binding.ivImagePreview.setImageURI(null)
    }

    /**
     * Creates and publishes a new post
     */
    private fun createPost() {
        val postText = binding.etPostText.text.toString().trim()
        
        // Validate input
        if (postText.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Please enter some text",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        showLoading(true)
        
        if (selectedImageUri != null) {
            // Upload image first, then create post
            uploadImageAndCreatePost(postText)
        } else {
            // Create post without image
            createPostInFirestore(postText, null)
        }
    }

    /**
     * Uploads image to ImgBB and creates post
     * 
     * @param postText The text content of the post
     */
    private fun uploadImageAndCreatePost(postText: String) {
        val imageUri = selectedImageUri ?: return
        
        // Upload to ImgBB using coroutine
        lifecycleScope.launch {
            try {
                // Upload image to ImgBB
                val imageUrl = ImgBBUploader.uploadImage(requireContext(), imageUri)
                
                if (imageUrl != null) {
                    createPostInFirestore(postText, imageUrl)
                } else {
                    if (_binding == null) return@launch
                    
                    showLoading(false)
                    Toast.makeText(
                        requireContext(),
                        "Failed to upload image. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                if (_binding == null) return@launch
                
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Creates post document in Firestore
     * 
     * @param postText The text content of the post
     * @param imageUrl Optional URL of uploaded image
     */
    private fun createPostInFirestore(postText: String, imageUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        
        // Debug logging
        println("CreatePost - ImageURL: $imageUrl")
        
        // Create Post object
        val post = Post(
            userId = userId,
            username = currentUsername,
            profileImageUrl = currentProfileImageUrl,
            text = postText,
            imageUrl = imageUrl
        )
        
        // Add post to Firestore
        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener {
                showLoading(false)
                
                if (_binding == null) return@addOnSuccessListener
                
                val message = if (imageUrl != null) {
                    "Post with image created!"
                } else {
                    "Post created successfully!"
                }
                
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
                clearForm()
                
                // Navigate to home feed to see the new post
                findNavController().navigate(R.id.navigation_home)
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Failed to create post: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Clears the post creation form
     */
    private fun clearForm() {
        binding.etPostText.text?.clear()
        removeImage()
    }

    /**
     * Shows or hides the loading indicator
     * 
     * @param show true to show loading, false to hide
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPost.isEnabled = !show
        binding.btnAddImage.isEnabled = !show
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



