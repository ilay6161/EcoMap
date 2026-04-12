package com.example.ecomapapp.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ecomapapp.model.Report

@Database(entities = [Report::class], version = 1, exportSchema = false)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract val reportDao: ReportDao
}
