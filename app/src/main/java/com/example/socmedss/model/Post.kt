package com.example.socmedss.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a single post in the social media feed.
 *
 * @property postId The unique ID of the post.
 * @property userId The ID of the user who created the post.
 * @property username The username of the user who created the post.
 * @property profileImageUrl The profile image URL of the user who created the post.
 * @property text The text content of the post.
 * @property imageUrl An optional URL for an image attached to the post.
 * @property timestamp The time when the post was created. Annotated with @ServerTimestamp
 *                   to be automatically populated by Firestore.
 */
data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val text: String = "",
    val imageUrl: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)
