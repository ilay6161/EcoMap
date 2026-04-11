package com.example.ecomapapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Patterns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ecomapapp.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            binding.ivProfilePhoto.setImageURI(it)
            binding.ivProfilePhoto.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Set "Sign In" as bold in footer text
        binding.tvSignIn.text = Html.fromHtml(getString(R.string.have_account), Html.FROM_HTML_MODE_COMPACT)

        // Terms checkbox controls the Create Account button
        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnCreateAccount.isEnabled = isChecked
            binding.btnCreateAccount.alpha = if (isChecked) 1.0f else 0.5f
        }
        binding.btnCreateAccount.alpha = 0.5f

        binding.photoContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCreateAccount.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            if (validateInputs(name, email, password)) {
                registerWithEmail(name, email, password)
            }
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        var valid = true
        if (name.isBlank()) {
            binding.tilName.error = "Enter your full name"
            valid = false
        } else {
            binding.tilName.error = null
        }
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

    private fun registerWithEmail(name: String, email: String, password: String) {
        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        setLoading(false)
                        navigateToMain()
                    }
                } else {
                    setLoading(false)
                    showSnackbar(task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnCreateAccount.isEnabled = !loading && binding.cbTerms.isChecked
        binding.btnCreateAccount.alpha = if (!loading && binding.cbTerms.isChecked) 1.0f else 0.5f
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
