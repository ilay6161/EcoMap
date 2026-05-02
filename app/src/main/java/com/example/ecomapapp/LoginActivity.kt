package com.example.ecomapapp

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.ecomapapp.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.tvSignUp.text = Html.fromHtml(getString(R.string.no_account), Html.FROM_HTML_MODE_COMPACT)

        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            if (validateInputs(email, password)) {
                signInWithEmail(email, password)
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter a valid email to reset password"
            } else {
                binding.tilEmail.error = null
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showSnackbar("Password reset email sent to $email")
                    } else {
                        showSnackbar(task.exception?.message ?: "Failed to send reset email")
                    }
                }
            }
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        var valid = true
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"
            valid = false
        } else {
            binding.tilEmail.error = null
        }
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            valid = false
        } else {
            binding.tilPassword.error = null
        }
        return valid
    }

    private fun signInWithEmail(email: String, password: String) {
        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    navigateToMain()
                } else {
                    showSnackbar(task.exception?.message ?: "Sign in failed")
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}
