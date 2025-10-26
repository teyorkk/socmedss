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
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socmedss.PostDetailsActivity
import com.example.socmedss.R
import com.example.socmedss.adapter.PostsAdapter
import com.example.socmedss.auth.LoginActivity
import com.example.socmedss.databinding.FragmentProfileBinding
import com.example.socmedss.model.Post
import com.example.socmedss.util.HybridImageUploader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.*



/**
 * Profile Fragment
 * 
 * Displays current user's profile information and their posts.
 * Provides logout functionality.
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postsAdapter: PostsAdapter
    
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
     * Activity result launcher for picking profile picture from gallery
     */
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadProfilePicture(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupRecyclerView()
        loadUserProfile()
        loadUserPosts()
        setupClickListeners()
    }

    /**
     * Sets up RecyclerView for user's posts
     */
    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        postsAdapter = PostsAdapter(
            currentUserId = currentUserId,
            onPostClick = { post -> navigateToPostDetails(post) },
            onUsernameClick = null, // No username clicks needed in profile view (own posts)
            onLikeClick = { post, isLiked -> toggleLike(post, isLiked) }, // Handle like/unlike
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeletePostConfirmation(post) }
        )
        
        binding.recyclerViewUserPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }

    /**
     * Loads current user's profile information
     */
    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        showLoading(true)
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Check if view is still available
                if (_binding == null) return@addOnSuccessListener
                
                showLoading(false)
                
                val username = document.getString("username") ?: "Anonymous"
                val email = document.getString("email") ?: ""
                val profileImage = document.getString("profileImage")
                
                binding.tvUsername.text = username
                binding.tvEmail.text = email
                
                // Load profile picture
                if (!profileImage.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(profileImage)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for faster loading
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .circleCrop()
                        .into(binding.ivProfileImage)
                }
            }
            .addOnFailureListener { e ->
                // Check if view is still available
                if (_binding == null) return@addOnFailureListener
                
                showLoading(false)
                Toast.makeText(
                    requireContext(),
                    "Failed to load profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Loads posts created by current user
     */
    private fun loadUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                // Check if view is still available
                if (_binding == null) return@addSnapshotListener
                
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading posts: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(postId = doc.id)
                    }.sortedByDescending { it.timestamp }
                    
                    postsAdapter.submitList(posts)
                    binding.recyclerViewUserPosts.visibility = View.VISIBLE
                    binding.tvNoPosts.visibility = View.GONE
                } else {
                    binding.recyclerViewUserPosts.visibility = View.GONE
                    binding.tvNoPosts.visibility = View.VISIBLE
                }
            }
    }

    /**
     * Sets up click listeners for buttons
     */
    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
        
        binding.fabChangeProfilePic.setOnClickListener {
            checkPermissionsAndOpenPicker()
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
     * Opens the device's image picker for profile picture
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        pickImageLauncher.launch(intent)
    }
    
    /**
     * Uploads profile picture to Firebase Storage (with ImgBB backup) and updates Firestore
     */
    private fun uploadProfilePicture(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        
        showLoading(true)
        
        // Upload to Firebase Storage using coroutine
        lifecycleScope.launch {
            try {
                // Upload image using hybrid uploader (Firebase Storage primary, ImgBB backup)
                val imageUrl = HybridImageUploader.uploadImage(requireContext(), uri, userId, true)
                
                if (imageUrl != null) {
                    // Update Firestore user document
                    firestore.collection("users")
                        .document(userId)
                        .update("profileImage", imageUrl)
                        .addOnSuccessListener {
                            if (_binding == null) return@addOnSuccessListener
                            
                            showLoading(false)
                            Toast.makeText(
                                requireContext(),
                                "Profile picture updated!",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Update profile picture in all posts and comments
                            updateProfilePictureInPostsAndComments(userId, imageUrl)
                            
                            // Reload profile
                            loadUserProfile()
                        }
                        .addOnFailureListener { e ->
                            if (_binding == null) return@addOnFailureListener
                            
                            showLoading(false)
                            Toast.makeText(
                                requireContext(),
                                "Failed to update profile: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
                // Log the full error for debugging
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error uploading image: ${e.localizedMessage ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Shows logout confirmation dialog
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Performs logout and navigates to login screen
     */
    private fun performLogout() {
        auth.signOut()
        
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    /**
     * Shows or hides the modern loading overlay
     * 
     * @param show true to show loading, false to hide
     */
    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Navigates to Post Details screen
     * 
     * @param post The Post object to display in details
     */
    private fun navigateToPostDetails(post: Post) {
        val intent = Intent(requireContext(), PostDetailsActivity::class.java).apply {
            putExtra("POST_ID", post.postId)
            putExtra("USER_ID", post.userId)
            putExtra("USERNAME", post.username)
            putExtra("TEXT", post.text)
            putExtra("IMAGE_URL", post.imageUrl)
            putExtra("TIMESTAMP", post.timestamp?.time ?: 0L)
        }
        startActivity(intent)
    }

    /**
     * Shows dialog to edit post
     */
    private fun showEditPostDialog(post: Post) {
        // Navigate to dedicated edit post activity
        val intent = Intent(requireContext(), EditPostActivity::class.java).apply {
            putExtra("POST_ID", post.postId)
        }
        startActivity(intent)
    }
    
    /**
     * Updates post in Firestore
     */
    private fun updatePost(postId: String, newText: String) {
        firestore.collection("posts")
            .document(postId)
            .update("text", newText)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Post updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to update post: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    /**
     * Shows confirmation dialog before deleting post
     */
    private fun showDeletePostConfirmation(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                deletePost(post.postId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Deletes post from Firestore along with all its comments
     */
    private fun deletePost(postId: String) {
        // First, delete all comments associated with this post
        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .get()
            .addOnSuccessListener { commentsSnapshot ->
                // Use batch to delete all comments at once
                val batch = firestore.batch()
                for (comment in commentsSnapshot.documents) {
                    batch.delete(comment.reference)
                }
                
                // Commit the batch delete for comments
                batch.commit()
                    .addOnSuccessListener {
                        // After deleting comments, delete the post itself
                        firestore.collection("posts")
                            .document(postId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Post and comments deleted!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to delete post: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to delete comments: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                // If getting comments fails, still try to delete the post
                firestore.collection("posts")
                    .document(postId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Post deleted!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { deleteError ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to delete post: ${deleteError.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }
    
    /**
     * Updates profile picture URL in all user's posts and comments
     * This ensures that when a user changes their profile picture,
     * it updates across all their existing posts and comments
     * 
     * @param userId The user's ID
     * @param newProfileImageUrl The new profile image URL
     */
    private fun updateProfilePictureInPostsAndComments(userId: String, newProfileImageUrl: String) {
        // Update all posts by this user
        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { postsSnapshot ->
                if (!postsSnapshot.isEmpty) {
                    val batch = firestore.batch()
                    for (document in postsSnapshot.documents) {
                        batch.update(document.reference, "profileImageUrl", newProfileImageUrl)
                    }
                    batch.commit()
                }
                
                // Update all comments by this user (in all posts)
                firestore.collection("posts")
                    .get()
                    .addOnSuccessListener { allPostsSnapshot ->
                        for (postDoc in allPostsSnapshot.documents) {
                            firestore.collection("posts")
                                .document(postDoc.id)
                                .collection("comments")
                                .whereEqualTo("userId", userId)
                                .get()
                                .addOnSuccessListener { commentsSnapshot ->
                                    if (!commentsSnapshot.isEmpty) {
                                        val batch = firestore.batch()
                                        for (commentDoc in commentsSnapshot.documents) {
                                            batch.update(commentDoc.reference, "profileImageUrl", newProfileImageUrl)
                                        }
                                        batch.commit()
                                    }
                                }
                        }
                    }
            }
    }

    /**
     * Toggles like status for a post
     * 
     * @param post The post to like/unlike
     * @param shouldLike true to like, false to unlike
     */
    private fun toggleLike(post: Post, shouldLike: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val postRef = firestore.collection("posts").document(post.postId)
        
        val updatedLikedBy = if (shouldLike) {
            (post.likedBy + currentUserId).distinct()
        } else {
            post.likedBy.filter { it != currentUserId }
        }
        
        postRef.update("likedBy", updatedLikedBy)
            .addOnCompleteListener {
                // Re-enable button regardless of success/failure
                // The snapshot listener will update the UI anyway
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

