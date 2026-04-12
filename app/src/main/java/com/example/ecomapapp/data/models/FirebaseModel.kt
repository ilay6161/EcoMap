package com.example.ecomapapp.data.models

import com.example.ecomapapp.base.Completion
import com.example.ecomapapp.base.ReportsCompletion
import com.example.ecomapapp.model.Report
import com.example.ecomapapp.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirebaseModel {

    private val db = FirebaseFirestore.getInstance()

    fun addReport(report: Report, completion: Completion) {
        val json = report.toJson().toMutableMap()
        json["lastUpdated"] = FieldValue.serverTimestamp()
        if (report.createdAt == null) {
            json["createdAt"] = FieldValue.serverTimestamp()
        }

        db.collection("reports").document(report.id).set(json)
            .addOnSuccessListener {
                android.util.Log.d("FirebaseModel", "Report saved: ${report.id}")
                completion()
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirebaseModel", "Failed to save report: ${e.message}", e)
                completion()
            }
    }

    fun updateReport(report: Report, completion: Completion) {
        val json = report.toJson().toMutableMap()
        json["lastUpdated"] = FieldValue.serverTimestamp()

        db.collection("reports").document(report.id).set(json)
            .addOnSuccessListener { completion() }
            .addOnFailureListener { completion() }
    }

    fun deleteReport(reportId: String, completion: Completion) {
        db.collection("reports").document(reportId).delete()
            .addOnSuccessListener { completion() }
            .addOnFailureListener { completion() }
    }

    fun saveUser(user: User, completion: Completion) {
        db.collection("users").document(user.id).set(user.toJson())
            .addOnSuccessListener { completion() }
            .addOnFailureListener { completion() }
    }

    fun getUser(userId: String, callback: (User?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val user = doc.data?.let { User.fromJson(it) }
                callback(user)
            }
            .addOnFailureListener { callback(null) }
    }

    fun getReportsSince(since: Long, callback: ReportsCompletion) {
        val sinceTimestamp = Timestamp(since / 1000, 0)

        db.collection("reports")
            .whereGreaterThanOrEqualTo("lastUpdated", sinceTimestamp)
            .get()
            .addOnSuccessListener { snapshot ->
                val reports = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Report.fromJson(it) }
                }
                callback(reports)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
}
