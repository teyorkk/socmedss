package com.example.socmedss.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socmedss.PostDetailsActivity
import com.example.socmedss.R
import com.example.socmedss.adapter.PostsAdapter
import com.example.socmedss.databinding.ActivityUserProfileBinding
import com.example.socmedss.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * User Profile Activity
 * 
 * Displays another user's profile information and their posts.
 */
class UserProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserProfileBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postsAdapter: PostsAdapter
    
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get userId from intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        
        if (userId.isEmpty()) {
            Toast.makeText(this, "Invalid user profile", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        
        setupToolbar()
        setupRecyclerView()
        loadUserProfile()
        loadUserPosts()
    }

    /**
     * Sets up the toolbar with back navigation
     */
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Sets up RecyclerView for user's posts
     */
    private fun setupRecyclerView() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        
        postsAdapter = PostsAdapter(
            currentUserId = currentUserId,
            onPostClick = { post -> 
                navigateToPostDetails(post)
            },
            onUsernameClick = null, // Don't allow username clicks in user profile
            onLikeClick = { post, isLiked -> toggleLike(post, isLiked) }, // Handle like/unlike
            onEditClick = { post -> /* No edit for other users */ },
            onDeleteClick = { post -> /* No delete for other users */ }
        )
        
        binding.recyclerViewUserPosts.apply {
            layoutManager = LinearLayoutManager(this@UserProfileActivity)
            adapter = postsAdapter
        }
    }

    /**
     * Loads user profile information
     */
    private fun loadUserProfile() {
        showLoading(true)
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                
                if (document.exists()) {
                    val username = document.getString("username") ?: "Unknown User"
                    val email = document.getString("email") ?: ""
                    val profileImage = document.getString("profileImage")
                    
                    binding.toolbar.title = username
                    binding.tvUsername.text = username
                    binding.tvEmail.text = email
                    
                    // Load profile picture
                    if (!profileImage.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(profileImage)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .circleCrop()
                            .into(binding.ivProfileImage)
                    } else {
                        binding.ivProfileImage.setImageResource(R.drawable.ic_launcher_foreground)
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    /**
     * Loads posts created by this user
     */
    private fun loadUserPosts() {
        firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (!::binding.isInitialized) return@addSnapshotListener
                
                if (error != null) {
                    Toast.makeText(
                        this,
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
     * Shows or hides the loading indicator
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    /**
     * Navigates to Post Details screen
     * 
     * @param post The Post object to display in details
     */
    private fun navigateToPostDetails(post: Post) {
        val intent = Intent(this, PostDetailsActivity::class.java).apply {
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
     * Toggles like status for a post
     * 
     * @param post The post to like/unlike
     * @param shouldLike true to like, false to unlike
     */
    private fun toggleLike(post: Post, shouldLike: Boolean) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
}

