package com.example.ecomapapp.data.models

import android.graphics.Bitmap
import com.example.ecomapapp.base.MyApplication
import com.example.ecomapapp.base.StringCompletion
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

object CloudinaryStorageModel {

    private val executor = Executors.newSingleThreadExecutor()

    fun uploadImage(bitmap: Bitmap, name: String, callback: StringCompletion) {
        executor.execute {
            try {
                val context = MyApplication.appContext!!
                val file = File(context.cacheDir, "$name.jpg")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.flush()
                outputStream.close()

                val cloudinaryUrl = "https://api.cloudinary.com/v1_1/dlrfqkl03/image/upload"
                val uploadPreset = "ecomap_unsigned"

                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url = java.net.URL(cloudinaryUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val outputStreamConn = connection.outputStream

                // Upload preset field
                val presetField = "--$boundary\r\nContent-Disposition: form-data; name=\"upload_preset\"\r\n\r\n$uploadPreset\r\n"
                outputStreamConn.write(presetField.toByteArray())

                // File field
                val fileHeader = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$name.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n"
                outputStreamConn.write(fileHeader.toByteArray())
                outputStreamConn.write(file.readBytes())
                outputStreamConn.write("\r\n".toByteArray())

                // End boundary
                outputStreamConn.write("--$boundary--\r\n".toByteArray())
                outputStreamConn.flush()
                outputStreamConn.close()

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val secureUrlRegex = """"secure_url"\s*:\s*"([^"]+)"""".toRegex()
                    val matchResult = secureUrlRegex.find(response)
                    val secureUrl = matchResult?.groupValues?.get(1)
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(secureUrl)
                    }
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.readText()
                    android.util.Log.e("CloudinaryUpload", "Upload failed: code=$responseCode body=$errorBody")
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        callback(null)
                    }
                }

                file.delete()
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    callback(null)
                }
            }
        }
    }
}
