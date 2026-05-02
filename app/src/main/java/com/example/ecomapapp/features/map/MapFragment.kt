package com.example.ecomapapp.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
                    .icon(BitmapDescriptorFactory.defaultMarker(hueFor(report.category)))
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

    private fun hueFor(category: String): Float = when (category) {
        Report.CATEGORY_LITTER -> BitmapDescriptorFactory.HUE_ORANGE
        Report.CATEGORY_WATER_LEAK -> BitmapDescriptorFactory.HUE_AZURE
        Report.CATEGORY_ILLEGAL_DUMPING -> BitmapDescriptorFactory.HUE_RED
        Report.CATEGORY_INFRASTRUCTURE -> BitmapDescriptorFactory.HUE_GREEN
        Report.CATEGORY_POLLUTION -> BitmapDescriptorFactory.HUE_VIOLET
        else -> BitmapDescriptorFactory.HUE_GREEN
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
