package com.example.socmedss.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socmedss.MainActivity
import com.example.socmedss.databinding.ActivityRegisterBinding
import com.example.socmedss.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Register Activity
 * 
 * Handles new user registration using Firebase Authentication.
 * Creates user profile in Firestore upon successful registration.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupClickListeners()
    }

    /**
     * Sets up click listeners for register and back to login buttons
     */
    private fun setupClickListeners() {
        // Register button click
        binding.btnRegister.setOnClickListener {
            registerUser()
        }

        // Back to Login button click
        binding.btnGoToLogin.setOnClickListener {
            finish() // Go back to Login activity
        }

        // Toolbar back button
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Performs user registration with email, password, and username
     */
    private fun registerUser() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validate input
        if (!validateInput(username, email, password)) {
            return
        }

        // Show loading indicator
        showLoading(true)

        // Create user account with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid ?: return@addOnSuccessListener
                
                // Create user profile in Firestore
                val user = User(
                    userId = userId,
                    username = username,
                    email = email
                )
                
                firestore.collection("users")
                    .document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Registration successful!",
                            Toast.LENGTH_SHORT
                        ).show()
                        navigateToMainActivity()
                    }
                    .addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "Failed to create user profile: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Registration failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Validates registration input fields
     * 
     * @param username User's display name
     * @param email User's email address
     * @param password User's password
     * @return true if valid, false otherwise
     */
    private fun validateInput(
        username: String,
        email: String,
        password: String
    ): Boolean {
        if (username.isEmpty()) {
            binding.tilUsername.error = "Username is required"
            return false
        }
        
        if (username.length < 3) {
            binding.tilUsername.error = "Username must be at least 3 characters"
            return false
        }
        
        binding.tilUsername.error = null
        
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            return false
        }
        
        binding.tilEmail.error = null
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }
        
        binding.tilPassword.error = null
        return true
    }

    /**
     * Shows or hides the loading indicator
     * 
     * @param show true to show loading, false to hide
     */
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !show
        binding.btnGoToLogin.isEnabled = !show
    }

    /**
     * Navigates to MainActivity after successful registration
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}


