package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "order_history")
data class OrderHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val orderDate: Long = System.currentTimeMillis(),
    val itemsSummary: String,
    val totalAmount: Double,
    val status: String
)
