package com.example.ecomapapp.data.models

import android.graphics.Bitmap
import com.example.ecomapapp.base.StringCompletion
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

object FirebaseStorageModel {

    private val storage = FirebaseStorage.getInstance()

    fun uploadImage(bitmap: Bitmap, name: String, callback: StringCompletion) {
        val storageRef = storage.reference.child("reports/$name.jpg")

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val data = baos.toByteArray()

        storageRef.putBytes(data)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    callback(uri.toString())
                }.addOnFailureListener {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
