package com.example.ecomapapp.model

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ecomapapp.base.MyApplication
import com.google.firebase.Timestamp

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val status: String = STATUS_PENDING,
    val authorId: String = "",
    val authorName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val photoUrl: String? = null,
    val verifyCount: Int = 0,
    val lastUpdated: Long? = null,
    val createdAt: Long? = null
) {
    companion object {
        const val CATEGORY_LITTER = "LITTER"
        const val CATEGORY_WATER_LEAK = "WATER_LEAK"
        const val CATEGORY_ILLEGAL_DUMPING = "ILLEGAL_DUMPING"
        const val CATEGORY_INFRASTRUCTURE = "INFRASTRUCTURE"
        const val CATEGORY_POLLUTION = "POLLUTION"
        const val CATEGORY_OTHER = "OTHER"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_VERIFIED = "VERIFIED"
        const val STATUS_RESOLVED = "RESOLVED"

        private const val PREFS_NAME = "report_prefs"
        private const val KEY_LAST_UPDATED = "reports_last_updated"

        var lastUpdated: Long
            get() {
                val prefs = MyApplication.appContext!!
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                return prefs.getLong(KEY_LAST_UPDATED, 0L)
            }
            set(value) {
                val prefs = MyApplication.appContext!!
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_LAST_UPDATED, value).apply()
            }

        fun fromJson(json: Map<String, Any>): Report {
            val lastUpdatedTimestamp = json["lastUpdated"] as? Timestamp
            val createdAtTimestamp = json["createdAt"] as? Timestamp

            return Report(
                id = json["id"] as? String ?: "",
                title = json["title"] as? String ?: "",
                description = json["description"] as? String ?: "",
                category = json["category"] as? String ?: "",
                status = json["status"] as? String ?: STATUS_PENDING,
                authorId = json["authorId"] as? String ?: "",
                authorName = json["authorName"] as? String ?: "",
                latitude = (json["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (json["longitude"] as? Number)?.toDouble() ?: 0.0,
                locationName = json["locationName"] as? String ?: "",
                photoUrl = json["photoUrl"] as? String,
                verifyCount = (json["verifyCount"] as? Number)?.toInt() ?: 0,
                lastUpdated = lastUpdatedTimestamp?.toDate()?.time,
                createdAt = createdAtTimestamp?.toDate()?.time
                    ?: (json["createdAt"] as? Number)?.toLong()
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "category" to category,
            "status" to status,
            "authorId" to authorId,
            "authorName" to authorName,
            "latitude" to latitude,
            "longitude" to longitude,
            "locationName" to locationName,
            "photoUrl" to photoUrl,
            "verifyCount" to verifyCount,
            "createdAt" to createdAt
        )
    }
}
