package com.example.ecomapapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ecomapapp.model.Report

@Dao
interface ReportDao {

    @Query("SELECT * FROM reports ORDER BY createdAt DESC")
    fun getAllReports(): LiveData<List<Report>>

    @Query("SELECT * FROM reports WHERE id = :id")
    fun getReportById(id: String): LiveData<Report?>

    @Query("SELECT * FROM reports WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getReportsByAuthor(authorId: String): LiveData<List<Report>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReports(vararg reports: Report)

    @Delete
    fun deleteReport(report: Report)

    @Query("DELETE FROM reports WHERE id = :id")
    fun deleteReportById(id: String)
}
