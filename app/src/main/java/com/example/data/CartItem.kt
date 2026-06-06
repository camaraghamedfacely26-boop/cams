package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: String,
    val productTitle: String,
    val variant: String,
    val customText: String,
    val logoPath: String?, // Simulated file path/uri
    val computedPrice: Double,
    val quantity: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
