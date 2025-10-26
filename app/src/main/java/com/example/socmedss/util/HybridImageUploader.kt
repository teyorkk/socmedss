package com.example.socmedss.util

import android.content.Context
import android.net.Uri

/**
 * Hybrid Image Uploader
 * 
 * Uploads images using Firebase Storage as primary method,
 * and falls back to ImgBB if Firebase Storage fails.
 * This ensures reliability and reduces dependency on external services.
 */
object HybridImageUploader {
    
    /**
     * Upload image with automatic fallback
     * 
     * @param context Application context
     * @param imageUri URI of the image to upload
     * @param userId Current user ID for organizing uploads
     * @param isProfilePicture Whether this is a profile picture (default: false)
     * @return Image URL from Firebase Storage or ImgBB, or null if both fail
     */
    suspend fun uploadImage(
        context: Context, 
        imageUri: Uri, 
        userId: String,
        isProfilePicture: Boolean = false
    ): String? {
        // Try Firebase Storage first (preferred method)
        try {
            val folderPath = if (isProfilePicture) "profile_images" else "post_images"
            val firebaseUrl = FirebaseStorageUploader.uploadImage(context, imageUri, userId, folderPath)
            if (firebaseUrl != null) {
                return firebaseUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fallback to ImgBB if Firebase fails
        try {
            val imgbbUrl = ImgBBUploader.uploadImage(context, imageUri)
            if (imgbbUrl != null) {
                return imgbbUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Both methods failed
        return null
    }
    
    /**
     * Delete image from storage
     * 
     * @param imageUrl The URL of the image to delete
     */
    suspend fun deleteImage(imageUrl: String) {
        // Try to delete from Firebase Storage
        try {
            FirebaseStorageUploader.deleteImage(imageUrl)
        } catch (e: Exception) {
            // ImgBB doesn't support deletion via API, so we skip it
        }
    }
}

