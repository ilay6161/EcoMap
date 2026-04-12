package com.example.ecomapapp.dao

import androidx.room.Room
import com.example.ecomapapp.base.MyApplication

object AppLocalDB {
    val db: AppLocalDbRepository by lazy {
        Room.databaseBuilder(
            MyApplication.appContext!!,
            AppLocalDbRepository::class.java,
            "ecomap.db"
        ).fallbackToDestructiveMigration().build()
    }
}
