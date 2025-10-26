package com.example.socmedss.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.socmedss.PostDetailsActivity
import com.example.socmedss.adapter.PostsAdapter
import com.example.socmedss.databinding.FragmentHomeFeedBinding
import com.example.socmedss.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Home Feed Fragment
 * 
 * Displays a feed of all posts from all users in real-time.
 * Uses RecyclerView with PostsAdapter to display posts.
 * Listens to Firestore for real-time updates.
 */
class HomeFeedFragment : Fragment() {

    private var _binding: FragmentHomeFeedBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postsAdapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupRecyclerView()
        loadPosts()
    }

    /**
     * Sets up RecyclerView with adapter and layout manager
     */
    private fun setupRecyclerView() {
        val currentUserId = auth.currentUser?.uid ?: ""
        
        postsAdapter = PostsAdapter(
            currentUserId = currentUserId,
            onPostClick = { post -> navigateToPostDetails(post) },
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeletePostConfirmation(post) }
        )
        
        binding.recyclerViewPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }

    /**
     * Loads posts from Firestore with real-time updates
     */
    private fun loadPosts() {
        showLoading(true)
        
        // Query posts ordered by timestamp (newest first)
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                // Check if view is still available
                if (_binding == null) return@addSnapshotListener
                
                showLoading(false)
                
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error loading posts: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    // Convert Firestore documents to Post objects
                    val posts = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.copy(postId = doc.id)
                    }
                    
                    // Update adapter with new posts
                    postsAdapter.submitList(posts)
                    
                    // Show/hide empty state
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerViewPosts.visibility = View.VISIBLE
                } else {
                    // Show empty state if no posts
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewPosts.visibility = View.GONE
                }
            }
    }

    /**
     * Shows or hides the loading indicator
     * 
     * @param show true to show loading, false to hide
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
        val editText = EditText(requireContext()).apply {
            setText(post.text)
            hint = "Edit your post"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Edit Post")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty()) {
                    updatePost(post.postId, newText)
                } else {
                    Toast.makeText(requireContext(), "Post text cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



