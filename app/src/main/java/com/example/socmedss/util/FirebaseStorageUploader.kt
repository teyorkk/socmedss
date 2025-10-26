package com.example.socmedss.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Helper class for uploading images to Firebase Storage
 * 
 * Features:
 * - Automatic image compression to reduce file size
 * - Organized storage with post_images/ directory
 * - Secure access rules
 */
object FirebaseStorageUploader {
    
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference
    
    /**
     * Upload image from URI to Firebase Storage
     * 
     * @param context Application context
     * @param imageUri URI of the image to upload
     * @param userId Current user ID for organizing uploads
     * @param folderPath Optional folder path (default: "post_images")
     * @return Download URL from Firebase Storage, or null if upload fails
     */
    suspend fun uploadImage(
        context: Context, 
        imageUri: Uri, 
        userId: String, 
        folderPath: String = "post_images"
    ): String? {
        try {
            // Read and compress image
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Compress image to reduce size
            val compressedBitmap = compressImage(bitmap)
            
            // Convert to byte array
            val byteArray = bitmapToByteArray(compressedBitmap)
            
            // Generate unique filename
            val timestamp = System.currentTimeMillis()
            val filename = "$folderPath/${userId}/${timestamp}.jpg"
            
            // Create reference to the file location
            val imageRef = storageRef.child(filename)
            
            // Upload the file
            imageRef.putBytes(byteArray).await()
            
            // Get download URL after successful upload
            val downloadUrl = imageRef.downloadUrl.await()
            
            return downloadUrl.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Compress bitmap to reduce file size and speed up uploads
     * Reduces to 1024x1024 max for optimal balance
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val maxWidth = 1024
        val maxHeight = 1024
        
        var width = bitmap.width
        var height = bitmap.height
        
        if (width > maxWidth || height > maxHeight) {
            val ratio = width.toFloat() / height.toFloat()
            if (ratio > 1) {
                width = maxWidth
                height = (maxWidth / ratio).toInt()
            } else {
                height = maxHeight
                width = (maxHeight * ratio).toInt()
            }
        }
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    
    /**
     * Convert bitmap to byte array with JPEG compression
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Quality 85 for better balance between file size and quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
    
    /**
     * Delete an image from Firebase Storage
     * 
     * @param imageUrl The URL of the image to delete
     */
    suspend fun deleteImage(imageUrl: String) {
        try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

