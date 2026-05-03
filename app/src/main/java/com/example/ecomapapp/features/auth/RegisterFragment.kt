package com.example.ecomapapp.features.auth

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Patterns
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ecomapapp.R
import com.example.ecomapapp.data.models.CloudinaryStorageModel
import com.example.ecomapapp.data.models.FirebaseModel
import com.example.ecomapapp.databinding.ActivityRegisterBinding
import com.example.ecomapapp.model.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterFragment : Fragment() {

    private var _binding: ActivityRegisterBinding? = null
    private val binding get() = _binding!!
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.tvSignIn.text = Html.fromHtml(getString(R.string.have_account), Html.FROM_HTML_MODE_COMPACT)

        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            binding.btnCreateAccount.isEnabled = isChecked
            binding.btnCreateAccount.alpha = if (isChecked) 1.0f else 0.5f
        }
        binding.btnCreateAccount.alpha = 0.5f

        binding.photoContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvSignIn.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCreateAccount.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            if (validateInputs(name, email, password)) registerWithEmail(name, email, password)
        }
    }

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        var valid = true
        if (name.isBlank()) { binding.tilName.error = "Enter your full name"; valid = false }
        else binding.tilName.error = null
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email"; valid = false
        } else binding.tilEmail.error = null
        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"; valid = false
        } else binding.tilPassword.error = null
        return valid
    }

    private fun registerWithEmail(name: String, email: String, password: String) {
        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    val bitmap = uriToBitmap(selectedPhotoUri)
                    if (bitmap != null) {
                        CloudinaryStorageModel.uploadImage(bitmap, "profile_${user.uid}") { photoUrl ->
                            finishRegistration(user.uid, name, email, photoUrl)
                        }
                    } else {
                        finishRegistration(user.uid, name, email, null)
                    }
                } else {
                    setLoading(false)
                    showSnackbar(task.exception?.message ?: "Registration failed")
                }
            }
    }

    private fun finishRegistration(uid: String, name: String, email: String, photoUrl: String?) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply { if (!photoUrl.isNullOrEmpty()) setPhotoUri(Uri.parse(photoUrl)) }
            .build()

        auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
            val userDoc = User(id = uid, name = name, email = email, phone = "", photoUrl = photoUrl)
            FirebaseModel().saveUser(userDoc) {
                if (_binding == null) return@saveUser
                setLoading(false)
                findNavController().navigate(R.id.action_registerFragment_to_mapFragment)
            }
        }
    }

    private fun uriToBitmap(uri: Uri?): Bitmap? {
        uri ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            }
        } catch (e: Exception) { null }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnCreateAccount.isEnabled = !loading && binding.cbTerms.isChecked
        binding.btnCreateAccount.alpha = if (!loading && binding.cbTerms.isChecked) 1.0f else 0.5f
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
