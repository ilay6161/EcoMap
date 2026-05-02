package com.example.ecomapapp.features.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.model.Report

class MapViewModel : ViewModel() {

    private val repository = ReportsRepository.shared
    private val allReports: LiveData<List<Report>> = repository.getAllReports()

    val selectedCategories = MutableLiveData<Set<String>>(emptySet())
    val searchQuery = MutableLiveData<String>("")

    val filteredReports = MediatorLiveData<List<Report>>().apply {
        addSource(allReports) { applyFilters() }
        addSource(selectedCategories) { applyFilters() }
        addSource(searchQuery) { applyFilters() }
    }

    val allCategories: List<Pair<String, String>> = listOf(
        Report.CATEGORY_LITTER to "Litter",
        Report.CATEGORY_WATER_LEAK to "Water Leak",
        Report.CATEGORY_ILLEGAL_DUMPING to "Illegal Dumping",
        Report.CATEGORY_INFRASTRUCTURE to "Infrastructure",
        Report.CATEGORY_POLLUTION to "Pollution",
        Report.CATEGORY_OTHER to "Other"
    )

    private fun applyFilters() {
        val reports = allReports.value ?: emptyList()
        val active = selectedCategories.value.orEmpty()
        val query = searchQuery.value.orEmpty().trim().lowercase()

        filteredReports.value = reports.filter { report ->
            val matchesCategory = active.isEmpty() || report.category in active
            val matchesSearch = query.isEmpty() ||
                report.title.lowercase().contains(query) ||
                report.locationName.lowercase().contains(query)
            val hasCoords = report.latitude != 0.0 || report.longitude != 0.0
            matchesCategory && matchesSearch && hasCoords
        }
    }

    fun refresh() {
        repository.refreshReports()
    }

    fun toggleCategory(category: String) {
        val current = selectedCategories.value.orEmpty().toMutableSet()
        if (!current.add(category)) current.remove(category)
        selectedCategories.value = current
    }

    fun clearCategoryFilter() {
        selectedCategories.value = emptySet()
    }
}
