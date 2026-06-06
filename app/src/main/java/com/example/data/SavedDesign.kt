package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_designs")
data class SavedDesign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val productId: String,
    val productTitle: String,
    val customText: String,
    val customLogoName: String?,
    val variant: String,
    val timestamp: Long = System.currentTimeMillis()
)
