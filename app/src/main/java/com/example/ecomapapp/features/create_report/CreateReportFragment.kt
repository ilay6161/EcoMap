package com.example.ecomapapp.features.create_report

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.ecomapapp.R
import com.example.ecomapapp.data.models.StorageModel
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.databinding.FragmentCreateReportBinding
import com.example.ecomapapp.model.Report
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale
import java.util.UUID

class CreateReportFragment : Fragment() {

    private var _binding: FragmentCreateReportBinding? = null
    private val binding get() = _binding!!

    private var selectedCategory: String? = null
    private var capturedBitmap: Bitmap? = null
    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentLocationName: String = ""

    private val storageModel = StorageModel()

    private data class CategoryItem(
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView,
        val categoryValue: String
    )

    private lateinit var categoryItems: List<CategoryItem>

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedBitmap = it
            showPhotoPreview(it)
        }
    }

    // Gallery launcher
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

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Snackbar.make(binding.root, "Camera permission is required to take photos", Snackbar.LENGTH_LONG).show()
        }
    }

    // Location permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchCurrentLocation()
        } else {
            binding.tvLocationAddress.text = "Location permission denied"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryItems()
        setupClickListeners()
        requestLocationAndFetch()
    }

    private fun setupCategoryItems() {
        categoryItems = listOf(
            CategoryItem(binding.categoryLitter, binding.icLitter, binding.tvLitter, Report.CATEGORY_LITTER),
            CategoryItem(binding.categoryWaterLeak, binding.icWaterLeak, binding.tvWaterLeak, Report.CATEGORY_WATER_LEAK),
            CategoryItem(binding.categoryDumping, binding.icDumping, binding.tvDumping, Report.CATEGORY_ILLEGAL_DUMPING),
            CategoryItem(binding.categoryInfrastructure, binding.icInfrastructure, binding.tvInfrastructure, Report.CATEGORY_INFRASTRUCTURE),
            CategoryItem(binding.categoryPollution, binding.icPollution, binding.tvPollution, Report.CATEGORY_POLLUTION),
            CategoryItem(binding.categoryOther, binding.icOther, binding.tvOther, Report.CATEGORY_OTHER)
        )

        for (item in categoryItems) {
            item.container.setOnClickListener {
                selectCategory(item)
            }
        }
    }

    private fun selectCategory(selected: CategoryItem) {
        selectedCategory = selected.categoryValue

        // Reset all categories to default
        for (item in categoryItems) {
            item.container.setBackgroundResource(R.drawable.bg_category_item)
            item.label.setTextColor(ContextCompat.getColor(requireContext(), R.color.eco_text_dark))
            item.icon.setColorFilter(null)
        }

        // Highlight selected
        selected.container.setBackgroundResource(R.drawable.bg_category_item_selected)
        selected.label.setTextColor(Color.WHITE)
        selected.icon.setColorFilter(Color.WHITE)
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraLauncher.launch(null)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnRefreshLocation.setOnClickListener {
            requestLocationAndFetch()
        }

        binding.btnSubmit.setOnClickListener {
            submitReport()
        }
    }

    private fun showPhotoPreview(bitmap: Bitmap) {
        binding.ivPhotoPreview.setImageBitmap(bitmap)
        binding.ivPhotoPreview.visibility = View.VISIBLE
        binding.photoPlaceholder.visibility = View.GONE
    }

    private fun requestLocationAndFetch() {
        val finePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchCurrentLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (_binding == null) return@addOnSuccessListener
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                    reverseGeocode(it.latitude, it.longitude)
                    updateLocationUI()
                } ?: run {
                    binding.tvLocationAddress.text = "Could not detect location"
                }
            }.addOnFailureListener {
                if (_binding == null) return@addOnFailureListener
                binding.tvLocationAddress.text = "Location detection failed"
            }
        } catch (e: SecurityException) {
            binding.tvLocationAddress.text = "Location permission required"
        }
    }

    private fun reverseGeocode(lat: Double, lon: Double) {
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                currentLocationName = addresses[0].getAddressLine(0) ?: "Unknown"
            } else {
                currentLocationName = "Unknown location"
            }
        } catch (e: Exception) {
            currentLocationName = "Unknown location"
        }
        if (_binding != null) {
            updateLocationUI()
        }
    }

    private fun updateLocationUI() {
        binding.tvLocationAddress.text = currentLocationName.ifEmpty { "Detecting location..." }
        binding.tvGpsCoordinates.text = if (currentLatitude != 0.0 || currentLongitude != 0.0) {
            String.format(Locale.US, "GPS: %.4f° N, %.4f° W", currentLatitude, currentLongitude)
        } else {
            ""
        }
    }

    private fun validateForm(): Boolean {
        var valid = true

        if (capturedBitmap == null) {
            Snackbar.make(binding.root, getString(R.string.error_photo_required), Snackbar.LENGTH_SHORT).show()
            valid = false
        }

        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_title_required)
            valid = false
        } else {
            binding.tilTitle.error = null
        }

        if (selectedCategory == null) {
            Snackbar.make(binding.root, getString(R.string.error_category_required), Snackbar.LENGTH_SHORT).show()
            valid = false
        }

        val description = binding.etDescription.text?.toString()?.trim() ?: ""
        if (description.isEmpty()) {
            binding.tilDescription.error = getString(R.string.error_description_required)
            valid = false
        } else {
            binding.tilDescription.error = null
        }

        return valid
    }

    private fun submitReport() {
        if (!validateForm()) return

        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""
        val reportId = UUID.randomUUID().toString()

        // Show progress, disable button
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false

        // Upload image first
        storageModel.uploadImage(
            StorageModel.StorageAPI.CLOUDINARY,
            capturedBitmap!!,
            reportId
        ) { photoUrl ->
            if (_binding == null) return@uploadImage

            if (photoUrl == null) {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Snackbar.make(binding.root, getString(R.string.error_upload_failed), Snackbar.LENGTH_LONG).show()
                return@uploadImage
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val report = Report(
                id = reportId,
                title = title,
                description = description,
                category = selectedCategory!!,
                status = Report.STATUS_PENDING,
                authorId = currentUser?.uid ?: "",
                authorName = currentUser?.displayName ?: "Anonymous",
                latitude = currentLatitude,
                longitude = currentLongitude,
                locationName = currentLocationName,
                photoUrl = photoUrl,
                verifyCount = 0,
                lastUpdated = null,
                createdAt = System.currentTimeMillis()
            )

            ReportsRepository.shared.addReport(report) {
                if (_binding == null) return@addReport
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                Snackbar.make(binding.root, getString(R.string.report_submitted), Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
