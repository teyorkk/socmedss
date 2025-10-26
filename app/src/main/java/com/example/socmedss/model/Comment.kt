package com.example.socmedss.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a comment on a post.
 *
 * @property commentId The unique ID of the comment.
 * @property postId The ID of the post this comment belongs to.
 * @property userId The ID of the user who created the comment.
 * @property username The username of the user who created the comment.
 * @property profileImageUrl The profile image URL of the user who created the comment.
 * @property text The text content of the comment.
 * @property timestamp The time when the comment was created.
 */
data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val text: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)

