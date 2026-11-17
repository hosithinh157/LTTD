package com.example.smartfinance.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val categoryId: Int = 0,
    val name: String,
    val type: String, // "EXPENSE" hoặc "INCOME"
    val iconName: String
)