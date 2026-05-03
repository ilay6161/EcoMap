package com.example.ecomapapp.features.edit_report

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
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
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.ecomapapp.R
import com.example.ecomapapp.data.models.StorageModel
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.databinding.DialogConfirmDeleteBinding
import com.example.ecomapapp.databinding.FragmentEditReportBinding
import com.example.ecomapapp.model.Report
import com.google.android.material.snackbar.Snackbar
import com.squareup.picasso.Picasso

class EditReportFragment : Fragment() {

    private var _binding: FragmentEditReportBinding? = null
    private val binding get() = _binding!!

    private val args: EditReportFragmentArgs by navArgs()

    private var currentReport: Report? = null
    private var capturedBitmap: Bitmap? = null
    private var selectedCategory: String? = null

    private val storageModel = StorageModel()

    private data class CategoryItem(
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView,
        val categoryValue: String
    )

    private lateinit var categoryItems: List<CategoryItem>

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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Snackbar.make(binding.root, "Camera permission is required to take photos", Snackbar.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryItems()
        setupClickListeners()
        observeReport()
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
            item.container.setOnClickListener { selectCategory(item) }
        }
    }

    private fun selectCategory(selected: CategoryItem) {
        selectedCategory = selected.categoryValue
        for (item in categoryItems) {
            item.container.setBackgroundResource(R.drawable.bg_category_item)
            item.label.setTextColor(ContextCompat.getColor(requireContext(), R.color.eco_text_dark))
            item.icon.setColorFilter(null)
        }
        selected.container.setBackgroundResource(R.drawable.bg_category_item_selected)
        selected.label.setTextColor(Color.WHITE)
        selected.icon.setColorFilter(Color.WHITE)
    }

    private fun selectCategoryByValue(value: String) {
        val item = categoryItems.find { it.categoryValue == value } ?: return
        selectCategory(item)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnDelete.setOnClickListener {
            confirmDelete()
        }

        binding.btnCamera.setOnClickListener { launchCamera() }
        binding.btnCameraChange.setOnClickListener { launchCamera() }

        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnGalleryChange.setOnClickListener { galleryLauncher.launch("image/*") }

        binding.btnSave.setOnClickListener { saveReport() }
    }

    private fun launchCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun observeReport() {
        var filled = false
        ReportsRepository.shared.getReportById(args.reportId).observe(viewLifecycleOwner) { report ->
            if (report != null && !filled) {
                filled = true
                currentReport = report
                prefillFields(report)
            }
        }
    }

    private fun prefillFields(report: Report) {
        binding.etTitle.setText(report.title)
        binding.etDescription.setText(report.description)
        binding.tvLocationAddress.text = report.locationName

        selectCategoryByValue(report.category)

        if (!report.photoUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(report.photoUrl)
                .placeholder(R.drawable.ic_category_other)
                .into(binding.ivPhotoPreview)
            binding.ivPhotoPreview.visibility = View.VISIBLE
            binding.photoPlaceholder.visibility = View.GONE
            binding.changePhotoButtons.visibility = View.VISIBLE
        }
    }

    private fun showPhotoPreview(bitmap: Bitmap) {
        binding.ivPhotoPreview.setImageBitmap(bitmap)
        binding.ivPhotoPreview.visibility = View.VISIBLE
        binding.photoPlaceholder.visibility = View.GONE
        binding.changePhotoButtons.visibility = View.VISIBLE
    }

    private fun saveReport() {
        val report = currentReport ?: return
        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val description = binding.etDescription.text?.toString()?.trim() ?: ""

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_title_required)
            return
        }
        binding.tilTitle.error = null

        if (selectedCategory == null) {
            Snackbar.make(binding.root, getString(R.string.error_category_required), Snackbar.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        if (capturedBitmap != null) {
            storageModel.uploadImage(
                StorageModel.StorageAPI.CLOUDINARY,
                capturedBitmap!!,
                report.id
            ) { photoUrl ->
                if (_binding == null) return@uploadImage
                if (photoUrl == null) {
                    setLoading(false)
                    Snackbar.make(binding.root, getString(R.string.error_upload_failed), Snackbar.LENGTH_LONG).show()
                    return@uploadImage
                }
                persistUpdate(report, title, description, selectedCategory!!, photoUrl)
            }
        } else {
            persistUpdate(report, title, description, selectedCategory!!, report.photoUrl)
        }
    }

    private fun persistUpdate(
        original: Report,
        title: String,
        description: String,
        category: String,
        photoUrl: String?
    ) {
        val updated = original.copy(
            title = title,
            description = description,
            category = category,
            photoUrl = photoUrl
        )
        ReportsRepository.shared.updateReport(updated) {
            if (_binding == null) return@updateReport
            setLoading(false)
            Snackbar.make(binding.root, R.string.report_updated, Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun confirmDelete() {
        val report = currentReport ?: return
        val dialogBinding = DialogConfirmDeleteBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnDelete.setOnClickListener {
            dialog.dismiss()
            setLoading(true)
            ReportsRepository.shared.deleteReport(report) {
                if (_binding == null) return@deleteReport
                setLoading(false)
                findNavController().popBackStack()
            }
        }
        dialog.show()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.btnDelete.isEnabled = !loading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
