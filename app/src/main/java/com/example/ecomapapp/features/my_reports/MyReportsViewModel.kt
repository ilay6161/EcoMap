package com.example.ecomapapp.features.my_reports

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.ecomapapp.data.models.FirebaseAuthModel
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.model.Report

class MyReportsViewModel : ViewModel() {

    private val authModel = FirebaseAuthModel()

    val reports: LiveData<List<Report>> by lazy {
        val userId = authModel.currentUser?.uid ?: ""
        ReportsRepository.shared.getReportsByAuthor(userId)
    }

    fun refresh() {
        ReportsRepository.shared.refreshReports()
    }
}
