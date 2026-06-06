package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_clicks")
data class WhatsAppClick(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val productTitle: String,
    val finalPrice: Double,
    val currency: String,
    val timestamp: Long = System.currentTimeMillis()
)
