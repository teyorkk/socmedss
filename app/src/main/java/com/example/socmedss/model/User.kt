package com.example.socmedss.model

/**
 * Data class representing a user in the social media app.
 *
 * @property userId The unique ID of the user (Firebase UID).
 * @property username The display name of the user.
 * @property email The email address of the user.
 * @property profileImage Optional URL for the user's profile image.
 */
data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = "",
    val profileImage: String? = null
)



