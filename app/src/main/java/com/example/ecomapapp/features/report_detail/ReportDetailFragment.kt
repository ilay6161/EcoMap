package com.example.ecomapapp.features.report_detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.ecomapapp.databinding.FragmentReportDetailBinding
import com.example.ecomapapp.model.Report
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReportDetailFragment : Fragment() {

    private var _binding: FragmentReportDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ReportDetailFragmentArgs by navArgs()
    private val viewModel: ReportDetailViewModel by viewModels()

    private var currentReport: Report? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnShare.setOnClickListener { shareReport() }
        binding.btnFlag.setOnClickListener {
            Snackbar.make(binding.root, "Report flagged", Snackbar.LENGTH_SHORT).show()
        }

        viewModel.getReport(args.reportId).observe(viewLifecycleOwner) { report ->
            if (report == null) return@observe
            currentReport = report
            populateUi(report)
            viewModel.loadWeatherOnce(report.latitude, report.longitude)
            binding.progressBar.visibility = View.GONE
            binding.scrollView.visibility = View.VISIBLE
        }

        viewModel.weather.observe(viewLifecycleOwner) { weather ->
            if (weather != null) {
                binding.temperature.text = "${weather.temperature.toInt()}°C"
                binding.windSpeed.text = "${weather.windSpeed.toInt()} km/h"
                binding.humidity.text = "${weather.humidity}%"
            }
        }
    }

    private fun populateUi(report: Report) {
        binding.reportTitle.text = report.title
        binding.locationName.text = report.locationName
        binding.description.text = report.description
        binding.authorName.text = report.authorName
        binding.timeAgo.text = formatTimeAgo(report.createdAt)
        binding.verifyButton.text = "👍 Verify (${report.verifyCount})"
        binding.categoryBadge.text = formatCategory(report.category)
        binding.categoryBadge.setBackgroundColor(categoryColor(report.category))

        report.photoUrl?.let { url ->
            Picasso.get().load(url).into(binding.photoImage)
        }

        val mapUrl = buildStaticMapUrl(report.latitude, report.longitude)
        Picasso.get().load(mapUrl).placeholder(android.R.color.darker_gray).into(binding.miniMap)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == report.authorId && report.status != Report.STATUS_RESOLVED) {
            binding.markResolvedButton.visibility = View.VISIBLE
        } else {
            binding.markResolvedButton.visibility = View.GONE
        }

        binding.verifyButton.setOnClickListener {
            currentReport?.let { r ->
                viewModel.incrementVerify(r)
                binding.verifyButton.text = "👍 Verify (${r.verifyCount + 1})"
                binding.verifyButton.isEnabled = false
            }
        }

        binding.markResolvedButton.setOnClickListener {
            currentReport?.let { r ->
                viewModel.markResolved(r)
                binding.markResolvedButton.visibility = View.GONE
                Snackbar.make(binding.root, "Marked as resolved", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.viewOnMapButton.setOnClickListener {
            val uri = Uri.parse("geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}(${Uri.encode(report.title)})")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                Snackbar.make(binding.root, "No maps app found", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareReport() {
        val report = currentReport ?: return
        val text = "${report.title}\n${report.locationName}\n\n${report.description}"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share report"))
    }

    private fun buildStaticMapUrl(lat: Double, lon: Double): String {
        val zoom = 15
        val n = Math.pow(2.0, zoom.toDouble())
        val xTile = ((lon + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(lat)
        val yTile = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        return "https://a.basemaps.cartocdn.com/rastertiles/voyager/$zoom/$xTile/$yTile.png"
    }

    private fun formatTimeAgo(createdAt: Long?): String {
        if (createdAt == null) return ""
        val diff = System.currentTimeMillis() - createdAt
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} hours ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(createdAt))
        }
    }

    private fun formatCategory(category: String): String = when (category) {
        Report.CATEGORY_LITTER -> "Litter"
        Report.CATEGORY_WATER_LEAK -> "Water Leak"
        Report.CATEGORY_ILLEGAL_DUMPING -> "Illegal Dumping"
        Report.CATEGORY_INFRASTRUCTURE -> "Infrastructure"
        Report.CATEGORY_POLLUTION -> "Pollution"
        else -> "Other"
    }

    private fun categoryColor(category: String): Int {
        val colorRes = when (category) {
            Report.CATEGORY_LITTER -> com.example.ecomapapp.R.color.eco_pin_litter
            Report.CATEGORY_WATER_LEAK -> com.example.ecomapapp.R.color.eco_pin_leak
            Report.CATEGORY_ILLEGAL_DUMPING -> com.example.ecomapapp.R.color.eco_pin_dumping
            else -> com.example.ecomapapp.R.color.eco_dark_green
        }
        return requireContext().getColor(colorRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
