package com.example.ecomapapp.features.profile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ecomapapp.dao.AppLocalDB
import com.example.ecomapapp.data.models.CloudinaryStorageModel
import com.example.ecomapapp.data.models.FirebaseModel
import com.example.ecomapapp.databinding.FragmentEditProfileBinding
import com.example.ecomapapp.model.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.squareup.picasso.Picasso
import java.util.concurrent.Executors

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firebaseModel = FirebaseModel()
    private val executor = Executors.newSingleThreadExecutor()

    private var capturedBitmap: Bitmap? = null
    private var existingPhotoUrl: String? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedBitmap = it
            showPhotoPreview(it)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(requireContext().contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                }
                capturedBitmap = bitmap
                showPhotoPreview(bitmap)
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Failed to load image", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentUser()
        setupClickListeners()
    }

    private fun loadCurrentUser() {
        val firebaseUser = auth.currentUser ?: return

        // Pre-fill from Firebase Auth
        binding.etName.setText(firebaseUser.displayName ?: "")
        binding.etEmail.setText(firebaseUser.email ?: "")

        // Load photo from Auth if available
        firebaseUser.photoUrl?.let { uri ->
            existingPhotoUrl = uri.toString()
            loadPhoto(uri.toString())
        }

        // Load additional data (phone) from Firestore
        firebaseModel.getUser(firebaseUser.uid) { user ->
            if (_binding == null) return@getUser
            user?.let {
                binding.etPhone.setText(it.phone)
                if (!it.photoUrl.isNullOrEmpty()) {
                    existingPhotoUrl = it.photoUrl
                    loadPhoto(it.photoUrl)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        binding.photoContainer.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadPhoto(url: String) {
        binding.ivProfilePhoto.imageTintList = null
        binding.ivProfilePhoto.setPadding(0, 0, 0, 0)
        Picasso.get()
            .load(url)
            .placeholder(android.R.drawable.ic_menu_my_calendar)
            .into(binding.ivProfilePhoto)
    }

    private fun showPhotoPreview(bitmap: Bitmap) {
        binding.ivProfilePhoto.imageTintList = null
        binding.ivProfilePhoto.setPadding(0, 0, 0, 0)
        binding.ivProfilePhoto.setImageBitmap(bitmap)
    }

    private fun saveProfile() {
        val name = binding.etName.text?.toString()?.trim() ?: ""
        val phone = binding.etPhone.text?.toString()?.trim() ?: ""

        if (name.isBlank()) {
            binding.tilName.error = "Enter your full name"
            return
        }
        binding.tilName.error = null

        val firebaseUser = auth.currentUser ?: return

        setLoading(true)

        if (capturedBitmap != null) {
            CloudinaryStorageModel.uploadImage(capturedBitmap!!, "profile_${firebaseUser.uid}") { url ->
                if (_binding == null) return@uploadImage
                if (url == null) {
                    setLoading(false)
                    Snackbar.make(binding.root, "Image upload failed. Please try again.", Snackbar.LENGTH_LONG).show()
                    return@uploadImage
                }
                persistProfile(firebaseUser.uid, name, firebaseUser.email ?: "", phone, url)
            }
        } else {
            persistProfile(firebaseUser.uid, name, firebaseUser.email ?: "", phone, existingPhotoUrl)
        }
    }

    private fun persistProfile(uid: String, name: String, email: String, phone: String, photoUrl: String?) {
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .apply { if (!photoUrl.isNullOrEmpty()) setPhotoUri(android.net.Uri.parse(photoUrl)) }
            .build()

        auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener {
            if (_binding == null) return@addOnCompleteListener

            val user = User(
                id = uid,
                name = name,
                email = email,
                phone = phone,
                photoUrl = photoUrl
            )

            // Save to Firestore
            firebaseModel.saveUser(user) {
                if (_binding == null) return@saveUser

                // Cache in Room
                executor.execute {
                    AppLocalDB.db.userDao.insertUser(user)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        if (_binding == null) return@post
                        setLoading(false)
                        Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
