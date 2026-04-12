package com.example.ecomapapp.data.repository.reports

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import com.example.ecomapapp.base.Completion
import com.example.ecomapapp.dao.AppLocalDB
import com.example.ecomapapp.data.models.FirebaseModel
import com.example.ecomapapp.model.Report
import java.util.concurrent.Executors

class ReportsRepository {

    companion object {
        val shared = ReportsRepository()
    }

    private val firebaseModel = FirebaseModel()
    private val dao = AppLocalDB.db.reportDao
    private val executor = Executors.newSingleThreadExecutor()

    fun getAllReports(): LiveData<List<Report>> {
        return dao.getAllReports()
    }

    fun getReportById(id: String): LiveData<Report?> {
        return dao.getReportById(id)
    }

    fun getReportsByAuthor(authorId: String): LiveData<List<Report>> {
        return dao.getReportsByAuthor(authorId)
    }

    fun refreshReports() {
        val since = Report.lastUpdated
        firebaseModel.getReportsSince(since) { reports ->
            executor.execute {
                for (report in reports) {
                    dao.insertReports(report)
                }
                val maxTimestamp = reports.maxOfOrNull { it.lastUpdated ?: 0L } ?: since
                if (maxTimestamp > since) {
                    Report.lastUpdated = maxTimestamp
                }
            }
        }
    }

    fun addReport(report: Report, completion: Completion) {
        firebaseModel.addReport(report) {
            executor.execute {
                dao.insertReports(report)
                Handler(Looper.getMainLooper()).post {
                    completion()
                }
            }
        }
    }

    fun deleteReport(report: Report, completion: Completion) {
        firebaseModel.deleteReport(report.id) {
            executor.execute {
                dao.deleteReport(report)
                Handler(Looper.getMainLooper()).post {
                    completion()
                }
            }
        }
    }
}
