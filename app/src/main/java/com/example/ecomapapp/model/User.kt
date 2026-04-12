package com.example.ecomapapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null
) {
    companion object {
        fun fromJson(json: Map<String, Any>): User {
            return User(
                id = json["id"] as? String ?: "",
                name = json["name"] as? String ?: "",
                email = json["email"] as? String ?: "",
                phone = json["phone"] as? String ?: "",
                photoUrl = json["photoUrl"] as? String
            )
        }
    }

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "photoUrl" to photoUrl
        )
    }
}
