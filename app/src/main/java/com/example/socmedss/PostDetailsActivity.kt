package com.example.socmedss

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socmedss.adapter.CommentsAdapter
import com.example.socmedss.databinding.ActivityPostDetailsBinding
import com.example.socmedss.model.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

/**
 * Post Details Activity
 * 
 * Displays a larger view of a selected post with full details.
 * Shows username, timestamp, text content, and image (if available).
 */
class PostDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostDetailsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var commentsAdapter: CommentsAdapter
    
    private var postId: String = ""
    private var currentUserId: String = ""
    private var currentUsername: String = "Anonymous"
    private var currentProfileImageUrl: String? = null
    private var isPostLiked: Boolean = false
    private var currentLikedBy: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        postId = intent.getStringExtra("POST_ID") ?: ""

        currentUserId = auth.currentUser?.uid ?: ""
        
        setupToolbar()
        setupCommentsRecyclerView()
        displayPostDetails()
        loadPostData()
        loadUserInfo()
        loadComments()
        setupClickListeners()
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
     * Loads post data including like information
     */
    private fun loadPostData() {
        if (postId.isEmpty()) return
        
        firestore.collection("posts")
            .document(postId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val likedBy = document.get("likedBy") as? List<*> ?: emptyList<String>()
                    currentLikedBy = likedBy.mapNotNull { it as? String }
                    isPostLiked = currentLikedBy.contains(currentUserId)
                    updateLikeButton()
                }
            }
    }

    /**
     * Displays post details from intent extras
     */
    private fun displayPostDetails() {
        val username = intent.getStringExtra("USERNAME") ?: "Unknown User"
        val text = intent.getStringExtra("TEXT") ?: ""
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val timestamp = intent.getLongExtra("TIMESTAMP", 0L)

        // Set username
        binding.tvUsername.text = username
        
        // Set post text
        binding.tvPostText.text = text
        
        // Format and set timestamp
        binding.tvTimestamp.text = formatTimestamp(timestamp)
        
        // Load and display image if available
        if (!imageUrl.isNullOrEmpty()) {
            binding.ivPostImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for faster loading
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.ivPostImage)
        } else {
            binding.ivPostImage.visibility = View.GONE
        }
    }

    /**
     * Formats a timestamp for display
     * 
     * @param timestamp The timestamp in milliseconds
     * @return Formatted time string
     */
    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown time"
        
        val date = Date(timestamp)
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000} minutes ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            diff < 604800000 -> "${diff / 86400000} days ago"
            else -> {
                val sdf = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
                sdf.format(date)
            }
        }
    }
    
    /**
     * Sets up the comments RecyclerView
     */
    private fun setupCommentsRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        commentsAdapter = CommentsAdapter(
            currentUserId = currentUserId,
            onEditClick = { comment -> showEditCommentDialog(comment) },
            onDeleteClick = { comment -> showDeleteCommentConfirmation(comment) }
        )
        binding.recyclerViewComments.apply {
            layoutManager = LinearLayoutManager(this@PostDetailsActivity)
            adapter = commentsAdapter
        }
    }
    
    /**
     * Loads current user info for commenting
     */
    private fun loadUserInfo() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                currentUsername = document.getString("username") ?: "Anonymous"
                currentProfileImageUrl = document.getString("profileImage")
            }
    }
    
    /**
     * Loads comments for this post
     */
    private fun loadComments() {
        if (postId.isEmpty()) return
        
        // Note: Comments are stored as subcollection under posts
        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error loading comments: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Comment::class.java)?.copy(commentId = doc.id)
                    }.sortedBy { it.timestamp }
                    
                    commentsAdapter.submitList(comments)
                    binding.recyclerViewComments.visibility = View.VISIBLE
                    binding.tvNoComments.visibility = View.GONE
                } else {
                    binding.recyclerViewComments.visibility = View.GONE
                    binding.tvNoComments.visibility = View.VISIBLE
                }
            }
    }
    
    /**
     * Sets up click listeners
     */
    private fun setupClickListeners() {
        binding.btnSendComment.setOnClickListener {
            addComment()
        }
        
        binding.btnLike.setOnClickListener {
            toggleLike()
        }
    }
    
    /**
     * Adds a new comment to the post
     */
    private fun addComment() {
        val commentText = binding.etComment.text.toString().trim()
        
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = auth.currentUser?.uid ?: return
        
        val comment = Comment(
            postId = postId,
            userId = userId,
            username = currentUsername,
            profileImageUrl = currentProfileImageUrl,
            text = commentText
        )
        
        // Store comment as subcollection of post
        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener {
                binding.etComment.text?.clear()
                Toast.makeText(this, "Comment added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to add comment: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    /**
     * Shows dialog to edit comment
     */
    private fun showEditCommentDialog(comment: Comment) {
        val editText = EditText(this).apply {
            setText(comment.text)
            hint = "Edit your comment"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Edit Comment")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updateComment(comment.commentId, newText)
                } else {
                    Toast.makeText(this, "Comment text cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Updates comment in Firestore
     */
    private fun updateComment(commentId: String, newText: String) {
        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .update("text", newText)
            .addOnSuccessListener {
                Toast.makeText(this, "Comment updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to update comment: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    /**
     * Shows confirmation dialog before deleting comment
     */
    private fun showDeleteCommentConfirmation(comment: Comment) {
        AlertDialog.Builder(this)
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                deleteComment(comment.commentId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Deletes comment from Firestore
     */
    private fun deleteComment(commentId: String) {
        firestore.collection("posts")
            .document(postId)
            .collection("comments")
            .document(commentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Comment deleted!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to delete comment: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
    
    /**
     * Updates the like button appearance
     */
    private fun updateLikeButton() {
        val likeCount = currentLikedBy.size
        
        // Set like button icon and text
        binding.btnLike.setIconResource(
            if (isPostLiked) R.drawable.ic_heart_filled
            else R.drawable.ic_heart_empty
        )
        
        // Set like count as button text
        binding.btnLike.text = "$likeCount"
        
        // Customize button appearance based on like state
        if (isPostLiked) {
            // Red when liked
            binding.btnLike.setIconTintResource(R.color.red)
            binding.btnLike.setTextColor(getColor(R.color.red))
        } else {
            // Gray when not liked
            binding.btnLike.setIconTintResource(android.R.color.darker_gray)
            binding.btnLike.setTextColor(getColor(android.R.color.darker_gray))
        }
    }
    
    /**
     * Toggles like status for the post
     */
    private fun toggleLike() {
        val postRef = firestore.collection("posts").document(postId)
        
        val updatedLikedBy = if (isPostLiked) {
            currentLikedBy.filter { it != currentUserId }
        } else {
            (currentLikedBy + currentUserId).distinct()
        }
        
        postRef.update("likedBy", updatedLikedBy)
            .addOnSuccessListener {
                isPostLiked = !isPostLiked
                currentLikedBy = updatedLikedBy
                updateLikeButton()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to update like: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}


