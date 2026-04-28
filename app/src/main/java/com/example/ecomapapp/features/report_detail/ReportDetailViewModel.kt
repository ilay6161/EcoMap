package com.example.ecomapapp.features.report_detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecomapapp.data.networking.CurrentWeather
import com.example.ecomapapp.data.networking.NetworkClient
import com.example.ecomapapp.data.repository.reports.ReportsRepository
import com.example.ecomapapp.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReportDetailViewModel : ViewModel() {

    val weather = MutableLiveData<CurrentWeather?>()
    private var weatherLoaded = false

    fun getReport(id: String): LiveData<Report?> =
        ReportsRepository.shared.getReportById(id)

    fun loadWeatherOnce(lat: Double, lon: Double) {
        if (weatherLoaded) return
        weatherLoaded = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.weatherClient.getCurrentWeather(lat, lon)
                withContext(Dispatchers.Main) { weather.value = response.current }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { weather.value = null }
            }
        }
    }

    fun incrementVerify(report: Report) {
        ReportsRepository.shared.incrementVerifyCount(report) {}
    }

    fun markResolved(report: Report) {
        ReportsRepository.shared.markResolved(report) {}
    }
}
