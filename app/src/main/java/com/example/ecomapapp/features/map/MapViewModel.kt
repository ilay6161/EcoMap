package com.example.ecomapapp.features.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.model.Report

class MapViewModel : ViewModel() {

    private val repository = ReportsRepository.shared

    val reports: LiveData<List<Report>> = repository.getAllReports().map { all ->
        all.filter { it.latitude != 0.0 || it.longitude != 0.0 }
    }

    fun refresh() {
        repository.refreshReports()
    }
}
