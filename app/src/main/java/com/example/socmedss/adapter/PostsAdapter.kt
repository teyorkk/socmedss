package com.example.socmedss.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socmedss.R
import com.example.socmedss.databinding.ItemPostBinding
import com.example.socmedss.model.Post
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView Adapter for displaying posts in a list.
 * 
 * Uses ListAdapter with DiffUtil for efficient list updates.
 * Handles post images with Glide and formats timestamps.
 * 
 * @param currentUserId The current logged-in user's ID
 * @param onPostClick Lambda function invoked when a post is clicked
 * @param onEditClick Lambda function invoked when edit is clicked
 * @param onDeleteClick Lambda function invoked when delete is clicked
 */
class PostsAdapter(
    private val currentUserId: String,
    private val onPostClick: (Post) -> Unit,
    private val onUsernameClick: ((String) -> Unit)? = null, // New callback for username clicks
    private val onLikeClick: ((Post, Boolean) -> Unit)? = null, // Callback for like/unlike
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    /**
     * ViewHolder for a single post item.
     * Uses ViewBinding for type-safe view access.
     */
    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds post data to the views.
         * 
         * @param post The Post object to display
         */
        fun bind(post: Post) {
            binding.apply {
                // Set username
                tvUsername.text = post.username
                
                // Make username clickable if callback is provided and it's not current user's post
                if (onUsernameClick != null && post.userId != currentUserId) {
                    tvUsername.setTextColor(ContextCompat.getColor(itemView.context, R.color.colorPrimary))
                    tvUsername.isClickable = true
                    tvUsername.setOnClickListener {
                        onUsernameClick(post.userId)
                    }
                } else {
                    tvUsername.isClickable = false
                    tvUsername.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.primary_text_light))
                }
                
                // Set post text
                tvPostText.text = post.text
                
                // Format and display timestamp
                tvTimestamp.text = formatTimestamp(post.timestamp)
                
                // Load and display profile picture
                if (!post.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(post.profileImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for faster loading
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .circleCrop()
                        .into(ivProfilePicture)
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_launcher_foreground)
                }
                
                // Load and display post image if available
                if (!post.imageUrl.isNullOrEmpty()) {
                    ivPostImage.visibility = View.VISIBLE
                    Glide.with(itemView.context)
                        .load(post.imageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for faster loading
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(ivPostImage)
                } else {
                    ivPostImage.visibility = View.GONE
                }
                
                // Show/hide more options button based on ownership
                if (post.userId == currentUserId) {
                    btnMoreOptions.visibility = View.VISIBLE
                    btnMoreOptions.setOnClickListener {
                        showPopupMenu(it, post)
                    }
                } else {
                    btnMoreOptions.visibility = View.GONE
                }
                
                // Update like button state and count
                val isLiked = post.likedBy.contains(currentUserId)
                val likeCount = post.likedBy.size
                
                // Set like button icon and text
                btnLike.setIconResource(
                    if (isLiked) R.drawable.ic_heart_filled
                    else R.drawable.ic_heart_empty
                )
                
                // Set like count as button text
                btnLike.text = "$likeCount"
                
                // Customize button appearance based on like state
                if (isLiked) {
                    // Red when liked
                    btnLike.setIconTintResource(R.color.red)
                    btnLike.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                } else {
                    // Gray when not liked
                    btnLike.setIconTintResource(android.R.color.darker_gray)
                    btnLike.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                }
                
                // Set like button click listener
                if (onLikeClick != null) {
                    btnLike.setOnClickListener {
                        btnLike.isEnabled = false
                        onLikeClick(post, !isLiked)
                    }
                } else {
                    btnLike.visibility = View.GONE
                }
                
                // Set click listener for the entire post card
                root.setOnClickListener {
                    onPostClick(post)
                }
            }
        }

        /**
         * Shows popup menu with edit and delete options
         */
        private fun showPopupMenu(view: View, post: Post) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.post_options_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick(post)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(post)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        /**
         * Formats a timestamp for display.
         * Shows relative time (e.g., "5 minutes ago") or formatted date.
         * 
         * @param timestamp The Date object to format
         * @return Formatted time string
         */
        private fun formatTimestamp(timestamp: Date?): String {
            if (timestamp == null) return "Just now"
            
            val now = System.currentTimeMillis()
            val diff = now - timestamp.time
            
            return when {
                diff < 60000 -> "Just now" // Less than 1 minute
                diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
                diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
                diff < 604800000 -> "${diff / 86400000}d ago" // Less than 1 week
                else -> {
                    // Show formatted date for older posts
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(timestamp)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * DiffUtil callback for calculating the difference between two lists.
     * Improves performance by only updating changed items.
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}



