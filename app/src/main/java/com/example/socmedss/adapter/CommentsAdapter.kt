package com.example.socmedss.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.socmedss.R
import com.example.socmedss.databinding.ItemCommentBinding
import com.example.socmedss.model.Comment
import java.text.SimpleDateFormat
import java.util.*

/**
 * RecyclerView Adapter for displaying comments on a post.
 * 
 * Uses ListAdapter with DiffUtil for efficient list updates.
 * Handles profile images with Glide and formats timestamps.
 * 
 * @param currentUserId The current logged-in user's ID
 * @param onEditClick Lambda function invoked when edit is clicked
 * @param onDeleteClick Lambda function invoked when delete is clicked
 */
class CommentsAdapter(
    private val currentUserId: String,
    private val onEditClick: (Comment) -> Unit,
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    /**
     * ViewHolder for a single comment item.
     * Uses ViewBinding for type-safe view access.
     */
    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds comment data to the views.
         * 
         * @param comment The Comment object to display
         */
        fun bind(comment: Comment) {
            binding.apply {
                // Set username
                tvUsername.text = comment.username
                
                // Set comment text
                tvCommentText.text = comment.text
                
                // Format and display timestamp
                tvTimestamp.text = formatTimestamp(comment.timestamp)
                
                // Load and display profile picture
                if (!comment.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(itemView.context)
                        .load(comment.profileImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache for faster loading
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .circleCrop()
                        .into(ivProfilePicture)
                } else {
                    ivProfilePicture.setImageResource(R.drawable.ic_launcher_foreground)
                }
                
                // Show/hide more options button based on ownership
                if (comment.userId == currentUserId) {
                    btnMoreOptions.visibility = View.VISIBLE
                    btnMoreOptions.setOnClickListener {
                        showPopupMenu(it, comment)
                    }
                } else {
                    btnMoreOptions.visibility = View.GONE
                }
            }
        }

        /**
         * Shows popup menu with edit and delete options
         */
        private fun showPopupMenu(view: View, comment: Comment) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.comment_options_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit -> {
                        onEditClick(comment)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteClick(comment)
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
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                diff < 604800000 -> "${diff / 86400000}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(timestamp)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * DiffUtil callback for calculating the difference between two lists.
     * Improves performance by only updating changed items.
     */
    private class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}

