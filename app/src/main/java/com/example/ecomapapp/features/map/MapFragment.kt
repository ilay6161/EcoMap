package com.example.ecomapapp.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.ecomapapp.R
import com.example.ecomapapp.databinding.FragmentMapBinding
import com.example.ecomapapp.model.Report
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MapViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private val markers = mutableListOf<Marker>()
    private var pendingReports: List<Report>? = null
    private var hasFitBoundsOnce = false
    private val pinCache = mutableMapOf<String, BitmapDescriptor>()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            enableMyLocationLayer()
            moveCameraToCurrentLocation()
        } else {
            Snackbar.make(
                binding.root,
                getString(R.string.map_permission_rationale),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapHost = childFragmentManager
            .findFragmentById(binding.mapContainer.id) as SupportMapFragment
        mapHost.getMapAsync { map ->
            googleMap = map
            map.uiSettings.isZoomControlsEnabled = false
            map.uiSettings.isMyLocationButtonEnabled = false
            map.uiSettings.isCompassEnabled = true

            if (hasLocationPermission()) {
                enableMyLocationLayer()
            }

            map.setOnMarkerClickListener { marker ->
                val reportId = marker.tag as? String
                    ?: return@setOnMarkerClickListener false
                val action = MapFragmentDirections
                    .actionMapFragmentToReportDetailFragment(reportId)
                findNavController().navigate(action)
                true
            }

            binding.mapProgressBar.visibility = View.GONE
            pendingReports?.let { renderMarkers(it) }
        }

        viewModel.reports.observe(viewLifecycleOwner) { reports ->
            if (googleMap == null) {
                pendingReports = reports
            } else {
                renderMarkers(reports)
            }
        }

        binding.btnLocateMe.setOnClickListener {
            if (hasLocationPermission()) {
                moveCameraToCurrentLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        markers.clear()
        googleMap = null
        _binding = null
    }

    private fun renderMarkers(reports: List<Report>) {
        val map = googleMap ?: return
        markers.forEach { it.remove() }
        markers.clear()

        reports.forEach { report ->
            val position = LatLng(report.latitude, report.longitude)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(report.title)
                    .snippet(report.locationName)
                    .icon(pinFor(report.category))
            ) ?: return@forEach
            marker.tag = report.id
            markers += marker
        }

        if (!hasFitBoundsOnce && reports.size >= 2) {
            val bounds = LatLngBounds.Builder().apply {
                reports.forEach { include(LatLng(it.latitude, it.longitude)) }
            }.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
            hasFitBoundsOnce = true
        } else if (!hasFitBoundsOnce && reports.size == 1) {
            val r = reports.first()
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(r.latitude, r.longitude), 14f))
            hasFitBoundsOnce = true
        }
    }

    private fun pinFor(category: String): BitmapDescriptor {
        pinCache[category]?.let { return it }

        val ctx = requireContext()
        val density = resources.displayMetrics.density
        val pinW = (32 * density).toInt()
        val pinH = (42 * density).toInt()
        val iconSize = (14 * density).toInt()
        val iconLeft = (pinW - iconSize) / 2
        val iconTop = (7 * density).toInt()

        val pin = ContextCompat.getDrawable(ctx, R.drawable.ic_map_pin_base)!!.mutate()
        DrawableCompat.setTint(pin, ContextCompat.getColor(ctx, pinColorRes(category)))

        val icon = ContextCompat.getDrawable(ctx, iconResFor(category))!!.mutate()
        DrawableCompat.setTint(icon, Color.WHITE)

        val bitmap = Bitmap.createBitmap(pinW, pinH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        pin.setBounds(0, 0, pinW, pinH)
        pin.draw(canvas)
        icon.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        icon.draw(canvas)

        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        pinCache[category] = descriptor
        return descriptor
    }

    private fun pinColorRes(category: String): Int = when (category) {
        Report.CATEGORY_LITTER -> R.color.eco_pin_litter
        Report.CATEGORY_WATER_LEAK -> R.color.eco_pin_leak
        Report.CATEGORY_ILLEGAL_DUMPING -> R.color.eco_pin_dumping
        Report.CATEGORY_INFRASTRUCTURE -> R.color.eco_pin_infrastructure
        Report.CATEGORY_POLLUTION -> R.color.eco_pin_pollution
        else -> R.color.eco_pin_other
    }

    private fun iconResFor(category: String): Int = when (category) {
        Report.CATEGORY_LITTER -> R.drawable.ic_category_litter
        Report.CATEGORY_WATER_LEAK -> R.drawable.ic_category_water_leak
        Report.CATEGORY_ILLEGAL_DUMPING -> R.drawable.ic_category_dumping
        Report.CATEGORY_INFRASTRUCTURE -> R.drawable.ic_category_infrastructure
        Report.CATEGORY_POLLUTION -> R.drawable.ic_category_pollution
        else -> R.drawable.ic_category_other
    }

    private fun hasLocationPermission(): Boolean {
        val ctx = requireContext()
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        googleMap?.isMyLocationEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToCurrentLocation() {
        val map = googleMap ?: return
        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 14f)
                )
            }
        }
    }
}
