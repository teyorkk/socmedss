package com.example.socmedss.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.socmedss.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Helper class for uploading images to ImgBB
 * 
 * ImgBB is a free image hosting service with API access.
 * No credit card required!
 * 
 * API Key is stored securely in local.properties file.
 */
object ImgBBUploader {
    
    // API key loaded from BuildConfig (set in local.properties)
    private val API_KEY = BuildConfig.IMGBB_API_KEY
    private const val UPLOAD_URL = "https://api.imgbb.com/1/upload"
    
    /**
     * Upload image from URI to ImgBB
     * 
     * @param context Application context
     * @param imageUri URI of the image to upload
     * @return Image URL from ImgBB, or null if upload fails
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Read and compress image
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                
                // Compress image to reduce size
                val compressedBitmap = compressImage(bitmap)
                
                // Convert to base64
                val base64Image = bitmapToBase64(compressedBitmap)
                
                // Upload to ImgBB
                uploadToImgBB(base64Image)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Compress bitmap to reduce file size and speed up uploads
     * Reduces to 800x800 max for faster loading
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val maxWidth = 800  // Reduced from 1024 for faster loading
        val maxHeight = 800
        
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
     * Convert bitmap to base64 string with optimized compression
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Quality 85 for better balance between file size and quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    
    /**
     * Upload base64 image to ImgBB
     */
    private fun uploadToImgBB(base64Image: String): String? {
        try {
            val url = URL(UPLOAD_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            // Prepare POST data
            val postData = "key=$API_KEY&image=${URLEncoder.encode(base64Image, "UTF-8")}"
            
            // Write data
            connection.outputStream.use { outputStream ->
                outputStream.write(postData.toByteArray())
                outputStream.flush()
            }
            
            // Read response
            val responseCode = connection.responseCode
            println("ImgBB Response Code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                println("ImgBB Response: $response")
                val jsonResponse = JSONObject(response)
                
                // Extract image URL from response
                if (jsonResponse.getBoolean("success")) {
                    val data = jsonResponse.getJSONObject("data")
                    val imageUrl = data.getString("url")
                    println("ImgBB Image URL: $imageUrl")
                    return imageUrl
                } else {
                    println("ImgBB Upload failed: ${jsonResponse.optString("error")}")
                }
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                println("ImgBB Error Response: $errorResponse")
            }
            
            return null
        } catch (e: Exception) {
            println("ImgBB Upload Exception: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
}

