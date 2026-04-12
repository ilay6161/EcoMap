package com.example.ecomapapp.data.models

import android.graphics.Bitmap
import com.example.ecomapapp.base.StringCompletion

class StorageModel {

    enum class StorageAPI {
        FIREBASE,
        CLOUDINARY
    }

    fun uploadImage(api: StorageAPI, bitmap: Bitmap, name: String, callback: StringCompletion) {
        when (api) {
            StorageAPI.FIREBASE -> FirebaseStorageModel.uploadImage(bitmap, name, callback)
            StorageAPI.CLOUDINARY -> CloudinaryStorageModel.uploadImage(bitmap, name, callback)
        }
    }
}
