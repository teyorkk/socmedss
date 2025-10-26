package com.example.socmedss.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.socmedss.MainActivity
import com.example.socmedss.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * Login Activity
 * 
 * Handles user login using Firebase Authentication.
 * Provides form validation and navigation to registration screen.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToMainActivity()
            return
        }

        setupClickListeners()
    }

    /**
     * Sets up click listeners for login and register buttons
     */
    private fun setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            loginUser()
        }

        // Navigate to Register button click
        binding.btnGoToRegister.setOnClickListener {
            navigateToRegister()
        }
    }

    /**
     * Performs user login with email and password
     */
    private fun loginUser() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validate input
        if (!validateInput(email, password)) {
            return
        }

        // Show loading indicator
        showLoading(true)

        // Authenticate with Firebase
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(
                    this,
                    "Login failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Validates login input fields
     * 
     * @param email User's email address
     * @param password User's password
     * @return true if valid, false otherwise
     */
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email"
            return false
        }
        
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return false
        }
        
        // Clear errors if validation passes
        binding.tilEmail.error = null
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
        binding.btnLogin.isEnabled = !show
        binding.btnGoToRegister.isEnabled = !show
    }

    /**
     * Navigates to MainActivity after successful authentication
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Navigates to Register Activity
     */
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}


