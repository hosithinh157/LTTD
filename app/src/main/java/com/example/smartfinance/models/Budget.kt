package com.example.smartfinance.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Budgets",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["userId"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["categoryId"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val budgetId: Int = 0,
    @ColumnInfo(index = true) val userId: Int,
    @ColumnInfo(index = true) val categoryId: Int,
    val month: String, // VD: "2026-11"
    val limitAmount: Double
)