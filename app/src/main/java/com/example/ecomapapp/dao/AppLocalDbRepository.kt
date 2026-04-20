package com.example.ecomapapp.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ecomapapp.model.Report
import com.example.ecomapapp.model.User

@Database(entities = [Report::class, User::class], version = 2, exportSchema = false)
abstract class AppLocalDbRepository : RoomDatabase() {
    abstract val reportDao: ReportDao
    abstract val userDao: UserDao
}
