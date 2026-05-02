package com.example.ecomapapp.features.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.model.Report

class FeedViewModel : ViewModel() {

    private val repository = ReportsRepository.shared
    private val allReports: LiveData<List<Report>> = repository.getAllReports()

    val selectedCategory = MutableLiveData<String?>(null)
    val searchQuery = MutableLiveData<String>("")

    val filteredReports = MediatorLiveData<List<Report>>().apply {
        addSource(allReports) { applyFilters() }
        addSource(selectedCategory) { applyFilters() }
        addSource(searchQuery) { applyFilters() }
    }

    private fun applyFilters() {
        val reports = allReports.value ?: emptyList()
        val category = selectedCategory.value
        val query = searchQuery.value.orEmpty().trim().lowercase()

        filteredReports.value = reports
            .filter { report ->
                val matchesCategory = category == null || report.category == category
                val matchesSearch = query.isEmpty() ||
                    report.title.lowercase().contains(query) ||
                    report.locationName.lowercase().contains(query)
                matchesCategory && matchesSearch
            }
            .sortedByDescending { it.createdAt }
    }

    fun refresh() {
        repository.refreshReports()
    }
}
